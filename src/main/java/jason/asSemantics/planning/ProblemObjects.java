/**
 * 
 */
package jason.asSemantics.planning;



public abstract class ProblemObjects extends ProblemTerms {
    public String toString() {
        return toPlannerString();
    }
    
    public abstract String toPlannerString();
}
