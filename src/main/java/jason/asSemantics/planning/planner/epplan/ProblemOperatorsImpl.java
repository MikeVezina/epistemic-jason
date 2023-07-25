/**
 *
 */
package jason.asSemantics.planning.planner.epplan;

import jason.asSemantics.planning.ProblemObjects;
import jason.asSemantics.planning.planner.PlanContextExtractor;
import jason.asSemantics.planning.planner.ProblemOperators;
import jason.asSyntax.*;
import jason.asSyntax.PlanBody.BodyType;
import jdk.jshell.spi.ExecutionControl;

import java.sql.Array;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jason.mas2j.parser.ParseException;

/**
 * @author Michael Vezina, Felipe Meneguzzi
 * Modified for epistemic planning
 * <p>
 * TODO:
 * - Follow operator syntax for epistemic planner
 * - Use 'on' plans as operators
 * - How does planner handle pre of actions? Does the pre-condition have to be true in the actual world or does the planner need to be modified for allowing any pre-condition
 *          -> This is done by the planner in kstate::execute_ontic.... Specifically in kstate::add_ret_ontic_worlds via entails(act.get_executability(), start)
 *          -> Better yet, the planner has an option "-act_check WW" that seems to achieve this?
 */
public class ProblemOperatorsImpl extends ProblemOperators {

    protected static final Logger logger = Logger.getLogger(ProblemOperators.class.getName());

    private Map<String, Integer> lastOperator;
    private Map<String, Plan> operatorNameToPlan;
    private Map<Plan, String> operatorToName;
    private final EpistemicPlanPlannerConverter converter;

    public static final String LINE_SEP = System.getProperty("line.separator");

    public ProblemOperatorsImpl(EpistemicPlanPlannerConverter converter) {
        this.converter = converter;
        this.lastOperator = new LinkedHashMap<>();
        this.operatorNameToPlan = new LinkedHashMap<>();
        this.operatorToName = new LinkedHashMap<>();
    }

    @Override
    public void add(Plan plan) {
        String opName = createOperatorName(plan);
        Integer nextInt = lastOperator.getOrDefault(opName, 0) + 1;
        String newOpName = opName + "_" + nextInt;
        operatorNameToPlan.put(newOpName, plan);
        operatorToName.put(plan, newOpName);

        // Update last int
        lastOperator.put(opName, nextInt);

        super.add(plan);
    }

    protected String createOperatorName(Plan p) {
        return converter.toStripsString(p.getTrigger().getLiteral());
    }

    /* (non-Javadoc)
         * @see jason.asSemantics.planning.planner.ProblemOperators#toPlannerString()
         * Example:
         * executable shout_1 if B(a, q), at_1;
         *  shout_1 announces q;
         *  a observes shout_1;
            b observes shout_1;

            executable shout_2 if B(a, q), at_2;
            shout_2 announces q;
            a observes shout_2;
            b observes shout_2;
            c observes shout_2;
         *
         *
         */
    @SuppressWarnings("unchecked")
    @Override
    public String toPlannerString() {
        StringBuffer sb = new StringBuffer();


        // Plans should be ground at this point (but I need to double check)
        for (var opEntry : operatorToName.entrySet()) {
            Plan plan = opEntry.getKey();
            String operatorName = opEntry.getValue();
            if (!plan.isGround()) {
                logger.warning("WARNING: Plan operator not ground -- " + plan);
                throw new RuntimeException(new ExecutionControl.NotImplementedException("Missing syntax transformation for non-ground Plan"));
            }

            sb.append("executable ").append(operatorName);


            /*
              ------------------------------------
              HANDLE PLAN CONTEXT
              ------------------------------------
             */

            // Not sure what planner syntax is for missing pre-condition
            if (plan.getContext() != null) {
                sb.append(" if ");

                PlanContextExtractor contextExtractor = PlanContextExtractor.getPlanContextExtractor();
                contextExtractor.extractContext(plan);
                List<LogicalFormula> contextTerms = contextExtractor.getContext();
                List<String> precondStrings = new ArrayList<>();

                for (Iterator<LogicalFormula> iter = contextTerms.iterator(); iter.hasNext(); ) {
                    LogicalFormula formula = iter.next();
                    if (formula instanceof Literal) {
                        Literal literal = (Literal) formula;
                        precondStrings.add(converter.toStripsString(literal));
                    } else {
                        logger.fine("Ignored formula: " + formula);
                    }
                }
                if (precondStrings.isEmpty()) {
                    sb.append("true");
                } else {
                    sb.append(converter.toStringList(precondStrings, false));
                }
            }

            // End executable line (with context)
            sb.append(";");


            sb.append(LINE_SEP);


            /*
              ------------------------------------
              HANDLE PLAN BODY/EFFECTS
              ------------------------------------
              z
              ?a
              b
              ?c
              d

              => z
              => b -> ?a
              => d -> ?c, ?a

             */


            PlanBody body = plan.getBody();
            Map<String, String> sbEffects = new LinkedHashMap<>();
            List<String> testConditions = new ArrayList<>();

            for (PlanBody literal : (PlanBodyImpl) body) {

                /* Handle Test Conditions */
                /* TODO: Add test conditions to 'on' plans*/
                if (literal.getBodyType() == BodyType.test) {
                    if (literal.getBodyTerm().toString().startsWith("not")) {
                        LogExpr expr = (LogExpr) literal.getBodyTerm();
                        testConditions.add("-" + converter.toStripsString(expr.getTerm(0)));
                    } else {
                        testConditions.add(converter.toStripsString(literal.getBodyTerm()));
                    }
                }

                /* Handle Bel Add/Del */
                if (literal.getBodyType() == BodyType.delBel)
                    sbEffects.put("-" + converter.toStripsString(literal.getBodyTerm()), converter.toStringList(testConditions, false));
                else if (literal.getBodyType() == BodyType.addBel) {
                    sbEffects.put(converter.toStripsString(literal.getBodyTerm()), converter.toStringList(testConditions, false));
                }
            }

            if (!sbEffects.isEmpty()) {
                for (var entry : sbEffects.entrySet())
                    sb.append(createOperatorEffect(operatorName, entry.getKey(), entry.getValue()))
                            .append(LINE_SEP);
            }

            // sb.append(LINE_SEP);
            // sb.append(LINE_SEP);

            // For now, all agents observe all actions

            sb.append("a observes ").append(operatorName).append(";").append(LINE_SEP).append(LINE_SEP);


        }

        // sb.append(LINE_SEP);

        return sb.toString();
    }

    protected String createOperatorEffect(String operator, String effect, String condition) {
        if (operator.isBlank() || (effect.isBlank() && condition.isBlank()))
            return "";


        StringBuilder sb = new StringBuilder();
        sb.append(operator);

        if (!effect.isBlank())
            sb.append(" causes ").append(effect);

        if (!condition.isBlank())
            sb.append(" if ").append(condition);

        sb.append(";");
        return sb.toString();
    }

    @Override
    public List<String> getHeader() {
        // May need to be modified for: negation, toStripsString, etc.
        return this.plans.stream().map(p -> p.getTrigger().getLiteral().toString()).collect(Collectors.toList());
    }


    /**
     * hacky testing
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        ProblemOperatorsImpl operators = new ProblemOperatorsImpl(new EpistemicPlanPlannerConverter());
        operators.add(ASSyntax.parsePlan("+plan : ctx(a)."));
        operators.add(ASSyntax.parsePlan("+plan : true <- +eff(a); ?test(b); +eff(c)."));
        operators.add(ASSyntax.parsePlan("+plan : true <- +eff(a); ?test(b); +eff(c); -eff(d)."));
        operators.add(ASSyntax.parsePlan("+plan <- +eff(a); -eff(b)."));
        operators.add(ASSyntax.parsePlan("+plan : ctx(b) <- +eff(a)."));

        System.out.println(operators.toPlannerString());
    }
}
