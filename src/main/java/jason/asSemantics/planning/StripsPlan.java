/**
 * 
 */
package jason.asSemantics.planning;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Plan;

/**
 * A class representing the StripsPlan generated by an external planner
 * @author Felipe Meneguzzi
 *
 */
public abstract class StripsPlan extends ProblemTerms {
    
    protected byte stripsPlan[];
    
    protected StripsPlan() {
        
    }
    
    public StripsPlan(byte stripsPlan[]) {
        this.stripsPlan = stripsPlan;
    }

    /**
     * Converts this StripsPlan into a non-generic AgentSpeak plan that is 
     * activated by the introduction of a sequentially numbered term.
     * @param planSequence The number term to associate to the plan's trigger.
     * @return The resulting plan
     */
    public abstract Plan toAgentSpeakPlan(int planSequence);
    
    /**
     * Converts this StripsPlan into a generic AgentSpeak plan that is 
     * activated in place of the planner whenever similar conditions arise in 
     * the environment.
     * @param triggerCondition The literal representing the trigger condition 
     *                          for the activation of this plan
     * @param contextCondition The context condition for the new plan
     * @return The resulting plan
     */
    public abstract Plan toGenericAgentSpeakPlan(Literal triggerCondition, LogicalFormula contextCondition);
    
    /**
     * Returns the steps of this strips plan as an array of String
     * @return
     */
    public List<String> getStripsSteps() {
        ByteArrayInputStream inStream = new ByteArrayInputStream(stripsPlan);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        ArrayList<String> steps = new ArrayList<String>();
        
        try {
            while(reader.ready()) {
                steps.add(reader.readLine().trim());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return steps;
    }
}
