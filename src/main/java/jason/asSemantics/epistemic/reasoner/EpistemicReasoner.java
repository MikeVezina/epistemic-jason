package jason.asSemantics.epistemic.reasoner;

import com.google.gson.*;
import jason.asSemantics.epistemic.DELEventModel;
import jason.asSemantics.epistemic.Propositionalizer;
import jason.asSemantics.epistemic.reasoner.formula.EpistemicFormulaLiteral;
import jason.asSemantics.epistemic.reasoner.formula.Formula;
import jason.asSemantics.epistemic.reasoner.formula.ImpliesFormula;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class EpistemicReasoner {
    private static final String UPDATE_PROPS_SUCCESS_KEY = "success";
    private static final String EVALUATION_FORMULA_RESULTS_KEY = "result";
    private static final int NS_PER_MS = 1000000;
    private static final int MAX_CONSTRAINTS_LOG = 5000;
    private final CloseableHttpClient client;
    private static final Logger LOGGER = Logger.getLogger(EpistemicReasoner.class.getName());
    private final Logger metricsLogger = Logger.getLogger(getClass().getName() + " - Metrics");
    private final ReasonerConfiguration reasonerConfiguration;
    private final Propositionalizer propositionalizer;

    public EpistemicReasoner(CloseableHttpClient client, Propositionalizer propositionalizer) {
        this.client = client;
        this.reasonerConfiguration = ReasonerConfiguration.getInstance();
        this.propositionalizer = propositionalizer;
    }

    public EpistemicReasoner(Propositionalizer propositionalizer) {
        this(HttpClients.createDefault(), propositionalizer);
    }


    public boolean createModel(Collection<Formula> constraints) {
        metricsLogger.info("Creating model with " + constraints.size() + " constraints");

        dumpConstraints(constraints);

        if (!constraints.isEmpty())
            return true;

        // Maybe have the managed worlds object be event-driven for information updates.
        JsonObject managedJson = new JsonObject();
        managedJson.add("constraints", StringListToJsonArray(constraints));

        if (constraints.size() > MAX_CONSTRAINTS_LOG)
            LOGGER.info("Over " + MAX_CONSTRAINTS_LOG + " constraints. Not printing model creation request");
        else {
//            LOGGER.info("Model Creation (Req. Body): " + managedJson.toString());
        }

        long initialTime = System.nanoTime();


        var jsonBody = managedJson.toString();

        var request = RequestBuilder
                .post(reasonerConfiguration.getModelCreateEndpoint())
                .setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON))
                .build();

        LOGGER.info("Sending Model Creation Request");
        try (var resp = sendRequest(request, true)) {
            LOGGER.info("Model Post Response: " + resp.getStatusLine().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long creationTime = System.nanoTime() - initialTime;
        metricsLogger.info("Model creation time (ms): " + (creationTime / NS_PER_MS));
        return true;
    }

    private void dumpConstraints(Collection<Formula> constraints) {
        String fileName = "newer_constraints_" + constraints.size() + ".json";
        System.out.println("Failed to create model (too many constraints). Dumping to file: " + fileName);
        try (FileWriter fw = new FileWriter(fileName, false)) {
//                fw.write("[");
            int conSize = constraints.size();
            for (var c : constraints) {
                // // Get around stack overflow for JSON toString
                // if(c.getDepth() > 2000)
                // {
                //     System.out.println("(large constraint) Prop String for constraint:");
                //     System.out.println(c.toPropString());
                //     continue;
                // }
                fw.write(c.toJson().toString());
//                    if(conSize > 1)
//                        fw.write(",");
                fw.write("\r\n");
                conSize--;
            }
//                fw.write("]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static JsonArray StringListToJsonArray(Collection<Formula> constraints) {
        JsonArray res = new JsonArray();
        constraints.forEach(c -> res.add(c.toJson()));
        return res;
    }

    public boolean applyEventModel(DELEventModel eventModel) {
        var json = new JsonObject();
        var arr = new JsonArray();

        for (var entry : eventModel.getDelEvents()) {
            var entryJson = new JsonObject();

            entryJson.add("id", new JsonPrimitive(entry.getEventId()));
            entryJson.add("pre", entry.getPreCondition().toJson());


            var jsonPost = new JsonObject();

            for (var postEntry : entry.getPostCondition().entrySet()) {
                jsonPost.add(postEntry.getKey().toPropString(), postEntry.getValue().toJson());
            }

            entryJson.add("post", jsonPost);

            arr.add(entryJson);
        }

        json.add("events", arr);

        var req = RequestBuilder
                .post(reasonerConfiguration.getTransitionUpdateEndpoint())
                .setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON))
                .build();

        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();

        System.out.println(resultJson);
        return resultJson.get(UPDATE_PROPS_SUCCESS_KEY).getAsBoolean();
    }

    public Map<EpistemicFormulaLiteral, Boolean> evaluateFormulas(Collection<EpistemicFormulaLiteral> formulas) {
        Map<String, EpistemicFormulaLiteral> formulaHashLookup = new HashMap<>();
        Map<EpistemicFormulaLiteral, Boolean> formulaResults = new HashMap<>();

        if (formulas == null || formulas.isEmpty())
            return formulaResults;

        long initialTime = System.nanoTime();
        metricsLogger.info("Evaluating " + formulas.size() + " formulas");

        JsonObject formulaRoot = new JsonObject();
        JsonArray formulaArray = new JsonArray();

        for (EpistemicFormulaLiteral formula : formulas) {
            formulaArray.add(toFormulaJSON(formula));
            formulaHashLookup.put(formula.getUniqueId(), formula);
        }

        formulaRoot.add("formulas", formulaArray);
        long jsonStringTime = System.nanoTime() - initialTime;
        metricsLogger.info("Formula JSON build time (ms): " + (jsonStringTime / NS_PER_MS));


        var req = RequestBuilder
                .post(reasonerConfiguration.getEvaluateEndpoint())
                .setEntity(new StringEntity(formulaRoot.toString(), ContentType.APPLICATION_JSON))
                .build();

        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();

        long sendTime = System.nanoTime() - initialTime;
        metricsLogger.info("Reasoner formula evaluation time (ms): " + ((sendTime - jsonStringTime) / NS_PER_MS));

        // If the result is null, success == false, or there is no result entry, then return an empty set.
        if (resultJson == null || !resultJson.has(EVALUATION_FORMULA_RESULTS_KEY))
            return formulaResults;

        var resultPropsJson = resultJson.getAsJsonObject(EVALUATION_FORMULA_RESULTS_KEY);

        for (var key : resultPropsJson.entrySet()) {
            String formulaHashValue = key.getKey();
            Boolean formulaValuation = key.getValue().getAsBoolean();

            // Get the formula associated with the hash in the response
            var trueFormula = formulaHashLookup.getOrDefault(formulaHashValue, null);

            if (trueFormula == null)
                LOGGER.warning("Failed to lookup formula: " + key.getKey());
            else
                formulaResults.put(trueFormula, formulaValuation);
        }

        return formulaResults;
    }

    public Boolean evaluateFormula(Formula formula) {
        long initialTime = System.nanoTime();
        metricsLogger.info("Evaluating formula: " + formula.toString());

        long jsonStringTime = System.nanoTime() - initialTime;
        metricsLogger.info("Formula JSON build time (ms): " + (jsonStringTime / NS_PER_MS));


        var jsonBody = new JsonObject();
        jsonBody.add("formula", formula.toJson());

        var req = RequestBuilder
                .post(reasonerConfiguration.getSingleEvaluateEndpoint())
                .setEntity(new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON))
                .build();

        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();

        long sendTime = System.nanoTime() - initialTime;
        metricsLogger.info("Reasoner single formula evaluation time (ms): " + ((sendTime - jsonStringTime) / NS_PER_MS));

        // If the result is null, success == false, or there is no result entry, then return an empty set.
        if (resultJson == null || !resultJson.has(EVALUATION_FORMULA_RESULTS_KEY)) {
            System.out.println("Could not read single formula evaluation response");
            return false;
        }

        return resultJson.getAsJsonPrimitive(EVALUATION_FORMULA_RESULTS_KEY).getAsBoolean();
    }


    /**
     * Updates the currently believed propositions
     *
     * @param knowledgeFormulas The set of all knowledge formulas in the belief base, used for the knowledge valuation.
     * @param epistemicFormulas The formulas to evaluate immediately after updating the propositions.
     * @return The formula evaluation after updating the propositions. This will be empty if no formulas are provided.
     */
//    public Map<EpistemicFormula, Boolean> updateProps(Set<KnowEpistemicFormula> knowledgeFormulas, Collection<EpistemicFormula> epistemicFormulas) {
//
//        if (knowledgeFormulas == null)
//            throw new IllegalArgumentException("propositions list should not be null");
//
//        long initialUpdateTime = System.nanoTime();
//
//        // Object does not contain contradictions
//        JsonObject knowledgeValuation = new JsonObject();
//
//        // We use the HashMap to track contradictions
//        Map<String, Boolean> knowledgeValuationMap = new HashMap<>();
//
//        // This is where we create the knowledge valuation
//        for (KnowEpistemicFormula currentFormula : knowledgeFormulas) {
//            var propName = propositionalizer.propLit(currentFormula.getRootLiteral());
//            var isPositive = !currentFormula.isPropositionNegated();
//
//            // Check for proposition contradictions
//            var existing = knowledgeValuationMap.get(propName);
//
//            // If contradiction (i.e. existing value that is different)
//            // Don't include the contradictions in the model update (remove from JSON object)
//            if (existing != null && existing != isPositive) {
//                LOGGER.warning("There is a proposition contradiction for " + propName + " (both a true and false knowledge value). It has been excluded from the knowledge valuation.");
//                LOGGER.warning("Due to the removed contradiction, the epistemic model may contain more uncertainty than expected. Please check belief consistency.");
//
//                if (knowledgeValuation.has(propName))
//                    knowledgeValuation.remove(propName);
//            } else {
//                // Else, add to both objects/maps
//                knowledgeValuationMap.put(propName, isPositive);
//                knowledgeValuation.addProperty(propName, isPositive);
//            }
//        }
//
//        JsonObject bodyElement = new JsonObject();
//        bodyElement.add("props", knowledgeValuation);
//
//        var req = RequestBuilder
//                .put(reasonerConfiguration.getPropUpdateEndpoint())
//                .setEntity(new StringEntity(bodyElement.toString(), ContentType.APPLICATION_JSON))
//                .build();
//
//        long jsonStringTime = System.nanoTime() - initialUpdateTime;
//        metricsLogger.info("Prop JSON build time (ms): " + (jsonStringTime / NS_PER_MS));
//
//        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();
//
//        long totalTime = System.nanoTime() - initialUpdateTime;
//        metricsLogger.info("Reasoner Update Time (ms): " + ((totalTime - jsonStringTime) / NS_PER_MS));
//
//        if (resultJson == null || !resultJson.has(UPDATE_PROPS_SUCCESS_KEY) || !resultJson.get(UPDATE_PROPS_SUCCESS_KEY).getAsBoolean()) {
//            LOGGER.warning("Failed to successfully update props: " + bodyElement.toString());
//            LOGGER.warning("This typically indicates that your beliefs are inconsistent, or they contradict the created epistemic model.");
//        } else
//            LOGGER.info("Updated props successfully. Request Body: " + bodyElement.toString());
//
//        throw new RuntimeException("Not used");
//        return null;
////        evaluateFormulas(epistemicFormulas);
//    }


    /**
     * Sends the request without closing the response.
     *
     * @param request
     * @return
     */
    CloseableHttpResponse sendRequest(HttpUriRequest request, boolean shouldClose) {

        try {
            var res = client.execute(request);

            if (shouldClose)
                res.close();

            return res;
        } catch (IOException e) {

            throw new RuntimeException("Failed to connect to the reasoner!", e);
        }
    }

    /**
     * Sends a request, processes the response and closes the response stream.
     *
     * @param request
     * @param responseProcessFunc
     * @param <R>
     * @return
     */
    private <R> R
    sendRequest(HttpUriRequest request, @NotNull Function<CloseableHttpResponse, R> responseProcessFunc) {
        try (var res = sendRequest(request, false)) {
            return responseProcessFunc.apply(res);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static JsonElement jsonTransform(CloseableHttpResponse response) {
        try {
            BufferedInputStream bR = new BufferedInputStream(response.getEntity().getContent());
            String jsonStr = new String(bR.readAllBytes());
            return (new JsonParser()).parse(jsonStr).getAsJsonObject();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


//    static JsonObject ManagedWorldsToJson(ManagedWorlds managedWorlds) {
//        JsonObject modelObject = new JsonObject();
//
//        JsonArray worldsArray = new JsonArray();
//
//        Map<Integer, World> hashed = new HashMap<>();
//        int collisions = 0;
//
//
//        for (World world : managedWorlds) {
//            if (hashed.containsKey(world.hashCode())) {
//                hashed.get(world.hashCode());
//                collisions++;
//            } else
//                hashed.put(world.hashCode(), world);
//
//            worldsArray.add(WorldToJson(world));
//        }
//
//        LOGGER.warning("Hashing collision. There are " + collisions + " world hash collisions");
//
//        modelObject.add("worlds", worldsArray);
//
//        // TODO : Change this to the hashcode of an actual pointed world.
//        // No pointed world, the epistemic.reasoner will choose one at random.
//        // modelObject.addProperty("pointedWorld", getWorldName(managedWorlds.getPointedWorld()));
//        return modelObject;
//    }

//    private static JsonObject WorldToJson(World world) {
//        JsonObject worldObject = new JsonObject();
//        JsonObject propsVal = new JsonObject();
//
//        worldObject.addProperty("name", world.getUniqueName());
//        for (WrappedLiteral wrappedLiteral : world.getValuation()) {
//            propsVal.add(getWorldIdProp(world), new JsonPrimitive(true));
//            propsVal.add(String.valueOf(wrappedLiteral.toSafePropName()), new JsonPrimitive(true));
//        }
//        worldObject.add("props", propsVal);
//
//        return worldObject;
//    }

    /**
     * Returns a JSON element containing data for a formula.
     * The JSON element should encode:
     * - ID of formula
     * - Epistemic Modality Type ("know" or "possible")
     * - Negation of Modality (i.e. "~possible")
     * - Contained Proposition (i.e. cards["Alice", "AA"])
     * - Proposition Negation (i.e. ~cards["Alice", "AA"])
     *
     * @param formula
     * @return
     */
    JsonElement toFormulaJSON(EpistemicFormulaLiteral formula) {
        var jsonElement = new JsonObject();
        jsonElement.addProperty("id", formula.getUniqueId());

        jsonElement.addProperty("modalityNegated", formula.isModalityNegated());
        jsonElement.addProperty("modality", formula.getEpistemicModality().getFunctor());

        jsonElement.addProperty("propNegated", formula.isPropositionNegated());
        jsonElement.addProperty("prop", this.propositionalizer.propLit(formula.getRootLiteral()).toPropString());


        return jsonElement;
    }

//    private static String getWorldIdProp(World world)
//    {
//        return "world-id-" + world.getUniqueName();
//    }
//    /**
//     * Execute a DEL event, with a post-condition that maps propositions to (basic) Formulae.
//     * Each entry in the rule transitions
//     *
//     * @param worldTransitions
//     */
//    public boolean processTransitions(Map<World, World> worldTransitions) {
//        var json = new JsonObject();
//        var arr = new JsonArray();
//
//        for(var entry : worldTransitions.entrySet())
//        {
//            var entryJson = new JsonObject();
//
//            entryJson.add("pre", new JsonPrimitive(getWorldIdProp(entry.getKey())));
//            entryJson.add("post", new JsonPrimitive(getWorldIdProp(entry.getValue())));
//            arr.add(entryJson);
//        }
//
//        json.add("transitions", arr);
//
//        var req = RequestBuilder
//                .post(reasonerConfiguration.getTransitionUpdateEndpoint())
//                .setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON))
//                .build();
//
//        var resultJson = sendRequest(req, EpistemicReasoner::jsonTransform).getAsJsonObject();
//
//        System.out.println(resultJson);
//        return resultJson.get(UPDATE_PROPS_SUCCESS_KEY).getAsBoolean();
//    }
}
