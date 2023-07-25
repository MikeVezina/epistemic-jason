/**
 * 
 */
package jason.asSemantics.planning.planner.jemplan;

import jason.asSyntax.DefaultTerm;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Plan;
import jason.asSyntax.RelExpr;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import jason.asSemantics.planning.planner.jemplan.EMPlan;
import jason.asSemantics.planning.planner.GoalState;
import jason.asSemantics.planning.planner.PlanContextGenerator;
import jason.asSemantics.planning.planner.PlannerConverter;
import jason.asSemantics.planning.planner.ProblemObjects;
import jason.asSemantics.planning.planner.ProblemOperators;
import jason.asSemantics.planning.planner.StartState;
import jason.asSemantics.planning.planner.StripsPlan;

/**
 * @author  frm05r
 */
public class EMPlanPlannerConverter implements PlannerConverter {
    
    protected EMPlan planner;
    protected StartState startState;
    protected GoalState goalState;
    protected ProblemOperators operators;
    protected ProblemObjects objects;
    
    protected StripsPlan stripsPlan;
    
    protected int planNumber = 0;
    
    public EMPlanPlannerConverter() {
        planner = new EMPlan();
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
            if(literal.getFunctor().startsWith("object")) {
                Term newTerm = DefaultTerm.parse(literal.getTerm(0)+"("+literal.getTerm(1)+")");
                startState.addTerm(newTerm);
            }else if( (literal.getArity() != 0) && (!literal.getFunctor().startsWith("des"))){
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
     * @return  the goalState
     * @uml.property  name="goalState"
     */
    public GoalState getGoalState() {
        return goalState;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.PlannerConverter#getStartState()
     */
    /**
     * @return  the startState
     * @uml.property  name="startState"
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
        sb.append(startState.toPlannerString());
        sb.append(goalState.toPlannerString());
        sb.append(operators.toPlannerString());
        
        String problem = sb.toString().replace(System.getProperty("line.separator"), " ");
        System.out.println("Planning problem is: "+problem);
        //problem = problem.replace(System.getProperty("line.separator"), " ");
        
        String planString = planner.emplanStream(problem);
        
        planFound = (planString != null);
        
        if(planFound) {
            stripsPlan = new StripsPlanImpl(planString.getBytes());
        }
        
        return planFound;
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
        if(generic) {
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
        
        if(literal.negated()) {
            sbTerm.append("-");
        }
        sbTerm.append(toStripsString((Term)literal));
        
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
        StringBuffer sbTerm = new StringBuffer();
        
        if(term.isStructure()) {
            Structure structure = (Structure) term;
            sbTerm.append(structure.getFunctor());
            
            if(structure.getArity() != 0) {
                sbTerm.append("(");
                for (Iterator<Term> iter = structure.getTerms().iterator(); iter.hasNext();) {
                    Term t = (Term) iter.next();
                    sbTerm.append(toStripsString(t));
                    if(iter.hasNext()) {
                        sbTerm.append(", ");
                    }
                }
                sbTerm.append(")");
            }
        } else {
            sbTerm.append(term.toString());
        }
        
        return sbTerm.toString();
    }

}
