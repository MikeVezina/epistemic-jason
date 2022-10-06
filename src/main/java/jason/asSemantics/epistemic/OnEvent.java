package jason.asSemantics.epistemic;

import jason.asSemantics.Event;
import jason.asSyntax.Pred;

public class OnEvent {
    private Event event;
    private Pred eventPred;

    public OnEvent(Event event) {
        this.event = event;
        setEvent(event);
    }

    private void setEvent(Event event) {
        this.event = event;
        Pred pos = new Pred(event.getTrigger().getLiteral());
        this.eventPred = new Pred((Pred) pos.getTerm(0));
        this.eventPred.clearAnnots();
    }

    public Pred getEventPred()
    {
        return this.eventPred;
    }

    public boolean isAddition()
    {
        return this.event.getTrigger().isAddition();
    }

    public Event getEvent()
    {
        return this.event;
    }
}
