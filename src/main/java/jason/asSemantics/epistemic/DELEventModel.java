package jason.asSemantics.epistemic;

import java.util.Set;

public class DELEventModel {

    private final Set<DELEvent> delEvents;

    public DELEventModel(Set<DELEvent> delEvents) {
        this.delEvents = delEvents;
    }

    public Set<DELEvent> getDelEvents() {
        return delEvents;
    }
}
