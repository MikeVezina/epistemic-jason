package jason.asSemantics.epistemic;

import jason.asSyntax.Literal;

import java.util.HashMap;
import java.util.Map;

public class DELEvent {
    private String eventId;
    private String preCondition;
    private Map<String, String> postCondition;

    public DELEvent(Object eventId, String preCondition)
    {
        this.eventId = eventId.toString();
        this.preCondition = preCondition;
        this.postCondition = new HashMap<>();
    }

    public DELEvent(Object eventId)
    {
        this(eventId, Literal.LTrue.toString());
    }


    public Map<String, String> getPostCondition() {
        return postCondition;
    }

    public void setPostCondition(Map<String, String> postCondition) {
        this.postCondition = postCondition;
    }

    public String getPreCondition() {
        return preCondition;
    }

    public void setPreCondition(String preCondition) {
        this.preCondition = preCondition;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void addPostCondition(String str, String val) {
        this.postCondition.put(str, val);
    }
}
