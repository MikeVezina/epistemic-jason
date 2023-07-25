/**
 *
 */
package jason.asSemantics.planning.planner.epplan;

import jason.asSemantics.planning.planner.*;
import jason.asSyntax.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author frm05r
 */
public class EpistemicPlanPlannerConverter implements PlannerConverter {

    protected EpistemicPlanner planner;
    protected StartState startState;
    protected GoalState goalState;
    protected ProblemOperators operators;
    protected ProblemObjects objects;

    protected StripsPlan stripsPlan;

    protected int planNumber = 0;

    public EpistemicPlanPlannerConverter() {
        planner = new EpistemicPlanner();
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#createPlanningProblem(java.util.List, java.util.List, java.util.List)
     */
    public void createPlanningProblem(List<Literal> beliefs, List<Plan> plans, List<Term> goals) {
        startState = new StartStateImpl(this);
        goalState = new GoalStateImpl();
        operators = new ProblemOperatorsImpl(this);
        //XXX This variable is created just so the user don't get a null pointer when requesting for the objects
        objects = new ProblemObjectsImpl();

        goalState.addAll(goals);

        for (Literal literal : beliefs) {
            if (literal.getFunctor().startsWith("object")) {
                Term newTerm = DefaultTerm.parse(literal.getTerm(0) + "(" + literal.getTerm(1) + ")");
                startState.addTerm(newTerm);
            } else if ((literal.getArity() != 0) && (!literal.getFunctor().startsWith("des"))) {
                startState.addTerm(literal);
            }
        }

        for (Plan plan : plans) {
            operators.add(plan);
        }
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getGoalState()
     */

    /**
     * @return the goalState
     * @uml.property name="goalState"
     */
    public GoalState getGoalState() {
        return goalState;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getStartState()
     */

    /**
     * @return the startState
     * @uml.property name="startState"
     */
    public StartState getStartState() {
        return startState;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getProblemOperators()
     */
    public ProblemOperators getProblemOperators() {
        return operators;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getProblemObjects()
     */
    public ProblemObjects getProblemObjects() {
        return objects;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#executePlanner(jason.asSemantics.planning.planner.ProblemObjects, jason.asSemantics.planning.planner.StartState, jason.asSemantics.planning.planner.GoalState, jason.asSemantics.planning.planner.ProblemOperators)
     */
    public boolean executePlanner(ProblemObjects objects,
                                  StartState startState, GoalState goalState,
                                  ProblemOperators operators) {
        return executePlanner(objects, startState, goalState, operators, 10);
    }

    public boolean executePlanner(ProblemObjects objects, StartState startState, GoalState goalState, ProblemOperators operators, int maxPlanSteps) {
        boolean planFound = false;
        StringBuffer sb = new StringBuffer();

        sb.append(getAllFluents()); // Add fluents (all range values)
        sb.append(getAllActions()); // Add operator header 'action x, y, z...'
        sb.append("agent a;"); // Add single agent 'a'



        sb.append(startState.toPlannerString()); // TODO: replace with current Kripke model
        sb.append(goalState.toPlannerString()); // TODO: N/A?
        sb.append(operators.toPlannerString()); // TODO:

        String problem = sb.toString().replace(System.getProperty("line.separator"), " ");
        System.out.println("Planning problem is: " + problem);
        //problem = problem.replace(System.getProperty("line.separator"), " ");

        String planString = planner.emplanStream(problem);

        planFound = (planString != null);

        if (planFound) {
            stripsPlan = new StripsPlanImpl(planString.getBytes());
        }

        return planFound;
    }

    public String toStringList(List<?> list)
    {
        return toStringList(list, true);
    }

    public String toStringList(List<?> list, boolean semiTerminate)
    {
        List<String> strList = list.stream().map(Object::toString).collect(Collectors.toList());
        return String.join(",", strList) + (semiTerminate ? ";" : "");
    }

    private String getAllActions() {
        StringBuilder sb = new StringBuilder();
        List<Plan> operators = getProblemOperators().getPlans();
        System.out.println("Actions available: " + operators.size());
        if (!operators.isEmpty()) {
            sb.append("action ");
            for (int i = 0; i < operators.size(); i++) {
                sb.append(operators.get(i).getTrigger().getLiteral());

                if (i < operators.size() - 1)
                    sb.append(", ");
            }

            sb.append(";");
        }


        return sb.toString();
    }

    private List<String> getAllFluents() {
        return new ArrayList<>();
    }

    public boolean executePlanner(ProblemObjects objects,
                                  StartState startState, GoalState goalState,
                                  ProblemOperators operators, int maxPlanSteps, long timeout)
            throws TimeoutException {
        return false;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getStripsPlan()
     */
    public StripsPlan getStripsPlan() {
        return stripsPlan;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getAgentSpeakPlan()
     */
    public Plan getAgentSpeakPlan(boolean generic) {
        if (generic) {
            ListTerm goals = new ListTermImpl();
            goals.addAll(goalState.getTerms());
            Literal literal = new LiteralImpl("goalConj");
            literal.addTerm(goals);
            LogicalFormula contextCondition = PlanContextGenerator.getInstance().generateContext(stripsPlan.getStripsSteps(), operators.getPlans());
            return stripsPlan.toGenericAgentSpeakPlan(literal, contextCondition);
        } else {
            return stripsPlan.toAgentSpeakPlan(planNumber++);
        }
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#toStripsString(jason.asSyntax.Term)
     */
    public String toStripsString(Literal literal) {
        StringBuffer sbTerm = new StringBuffer();

        if (literal.negated()) {
            sbTerm.append("-");
        }
        sbTerm.append(toStripsString((Term) literal));

        return sbTerm.toString();
    }

    /*
     * (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#toStripsString(jason.asSyntax.RelExpr)
     */
    public String toStripsString(RelExpr expr) {
        //XXX Since the underlying planner can't do anything about this
        //We leave it like that
        return "";
    }

    /*
     * (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#toStripsString(jason.asSyntax.Term)
     */
    public String toStripsString(Term term) {
        // Planner requires ground propositions at all times
        // This does not handle strong negation? actually, handled in overloaded method


        StringBuffer sbTerm = new StringBuffer();

        if (term.isStructure()) {
            Structure structure = (Structure) term;
            sbTerm.append(structure.getFunctor());

            if (structure.getArity() != 0) {
                sbTerm.append("_");
                for (Iterator<Term> iter = structure.getTerms().iterator(); iter.hasNext(); ) {
                    Term t = (Term) iter.next();
                    sbTerm.append(toStripsString(t));
                    if (iter.hasNext()) {
                        sbTerm.append("_");
                    }
                }
                // sbTerm.append("");
            }
        } else {
            sbTerm.append(term.toString());
        }

        return sbTerm.toString();
    }

}
