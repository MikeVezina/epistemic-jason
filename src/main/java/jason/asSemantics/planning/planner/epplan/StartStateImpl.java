/**
 * 
 */
package jason.asSemantics.planning.planner.epplan;

import jason.asSemantics.planning.planner.StartState;
import jason.asSyntax.Term;

import java.util.Iterator;

/**
 * @author   Michael Vezina, frm05r
 *
 * TODO:
 * - Change to initial state syntax
 * - OR use existing epistemic model in reasoner as initial state
 */
public class StartStateImpl extends StartState {
    
    protected final EpistemicPlanPlannerConverter converter;
    
    public StartStateImpl(EpistemicPlanPlannerConverter converter) {
        this.converter = converter;
    }

    /* (non-Javadoc)
     * @see jason.asSemantics.planning.planner.StartState#toPlannerString()
     */
    @Override
    public String toPlannerString() {
        StringBuffer sbStart = new StringBuffer();


        /* TODO: Request Kripke model from reasoner, forward to planner */
        
        sbStart.append("start(");
        
        for (Iterator<Term> iter = terms.iterator(); iter.hasNext();) {
            Term term = iter.next();
            sbStart.append(converter.toStripsString(term));
            if(iter.hasNext())
                sbStart.append(", ");
        }
        
        if(terms.size() == 0) {
            sbStart.append("true");
        }
        
        sbStart.append(")");
        sbStart.append(System.getProperty("line.separator"));
        
        return sbStart.toString();
    }

}
