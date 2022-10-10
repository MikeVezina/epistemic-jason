package jason.asSemantics.epistemic;

import jason.asSemantics.epistemic.reasoner.formula.Formula;
import jason.asSemantics.epistemic.reasoner.formula.PropFormula;
import jason.asSyntax.Literal;
import jason.asSyntax.Pred;

import java.util.HashMap;
import java.util.Map;

public class DELEvent {
    private String eventId;
    private Formula preCondition;
    private Map<PropFormula, Formula> postCondition;

    public DELEvent(Object eventId, Formula preCondition) {
        this.eventId = eventId.toString();
        this.preCondition = preCondition;
        this.postCondition = new HashMap<>();
    }

    public DELEvent(Object eventId) {
        this(eventId, new PropFormula(new Pred(Literal.LTrue)));
    }


    public Map<PropFormula, Formula> getPostCondition() {
        return postCondition;
    }

    public void setPostCondition(Map<PropFormula, Formula> postCondition) {
        this.postCondition = postCondition;
    }

    public Formula getPreCondition() {
        return preCondition;
    }

    public void setPreCondition(Formula preCondition) {
        this.preCondition = preCondition;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void addPostCondition(PropFormula str, Formula val) {
        this.postCondition.put(str, val);
    }
}
