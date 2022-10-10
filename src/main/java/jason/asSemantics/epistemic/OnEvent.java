package jason.asSemantics.epistemic;

import jason.asSemantics.Event;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Pred;
import jason.asSyntax.Trigger;

public class OnEvent {
    private Event event;
    private Pred eventPred;
    private Literal eventLiteral;

    public OnEvent(Event event) {
        this.event = event;
        setEvent(event);
    }

    private void setEvent(Event event) {
        this.event = event;
        Pred pos = new Pred(event.getTrigger().getLiteral());

        this.eventLiteral = new LiteralImpl((Literal) pos.getTerm(0));
        this.eventLiteral.clearAnnots();

        this.eventPred = new Pred( eventLiteral);
    }

    public Pred getEventPred()
    {
        return this.eventPred;
    }
    public Literal getEventLit()
    {
        return this.eventLiteral;
    }

    public boolean isAddition()
    {
        return this.event.getTrigger().isAddition();
    }

    public Event getEvent()
    {
        return this.event;
    }

    public Trigger getTrigger() {
        return event.getTrigger();
    }
}
