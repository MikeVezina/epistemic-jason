package jason.asSyntax;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jason.asSemantics.RewriteUnifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jason.asSemantics.Agent;
import jason.asSemantics.InternalAction;
import jason.asSemantics.Unifier;
import jason.stdlib.puts;


/**
 * A particular type of literal used to represent internal actions (which has a "." in the functor).
 *
 * @navassoc - ia - InternalAction
 */
public class InternalActionLiteral extends Structure implements LogicalFormula {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(InternalActionLiteral.class.getName());

    private InternalAction ia = null; // reference to the object that implements the internal action, stored here to speed up the process of looking for the IA class inside the agent

    public InternalActionLiteral(String functor) {
        super(functor);
    }

    // used by clone
    public InternalActionLiteral(InternalActionLiteral l) {
        super(l.getNS(), (Structure) l);
        this.ia = l.ia;
    }

    // used by capply
    private InternalActionLiteral(InternalActionLiteral l, Unifier u) {
        super((Structure) l, u);
        this.ia = l.ia;
    }

    // used by cloneNS
    private InternalActionLiteral(Atom ns, InternalActionLiteral l) {
        super(ns, (Structure) l);
        this.ia = l.ia;
    }

    // used by the parser
    public InternalActionLiteral(Structure p, Agent ag) throws Exception {
        this(DefaultNS, p, ag);
    }

    public InternalActionLiteral(Atom ns, Structure p, Agent ag) throws Exception {
        super(ns, p);
        if (ag != null)
            ia = ag.getIA(getFunctor());
    }

    @Override
    public boolean isInternalAction() {
        return true;
    }

    @Override
    public boolean isAtom() {
        return false;
    }

    @Override
    public Literal makeVarsAnnon(Unifier un) {
        Literal t = super.makeVarsAnnon(un);
        if (t.getFunctor().equals(".puts")) { // vars inside strings like in .puts("bla #{X}") should be also replace
            // TODO: should it work for any string? if so, proceed this replacement inside StringTermImpl
            ((puts) puts.create()).makeVarsAnnon(t, un);
        }
        return t;
    }

    // handled by simplify
//    @Override
//    public Formula toPropFormula() {
//        if(!this.isGround())
//            return LFalse.toPropFormula();
//
//        // The expression has already been evaluated using log. consequences.
//        // I.e., can be propositionalized as 'true'
//        return LTrue.toPropFormula();
//    }

    @Override
    public Literal simplify() {
//        if(!this.isGround())
//            return LFalse;

        // The expression has already been evaluated using log. consequences.
        // I.e., can be propositionalized as 'true'
        return LTrue;
    }

    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(Agent ag, Unifier un) {
        var iter = executeCons(ag, un);
        if (iter == null)
            return EMPTY_REWRITE_UNIF_LIST.iterator();

        List<RewriteUnifier> list = new ArrayList<>();

        while(iter.hasNext())
        {
            Unifier unif = iter.next();
            InternalActionLiteral intC = (InternalActionLiteral) this.capply(unif);

            // Only rewrite to true if it is ground (this depends on the IA, some will only ground the result term, leaving others unground)
//            if(intC.isGround())
            list.add(new RewriteUnifier(intC, unif));

//            list.add(new RewriteUnifier(LTrue, iter.next()));
        }

        return list.iterator();

    }

    protected Iterator<Unifier> executeCons(Agent ag, Unifier un) {
        if (ag == null || ag.getTS().getAgArch().isRunning()) {
            try {
                InternalAction ia = getIA(ag);
                if (!ia.canBeUsedInContext()) {
                    logger.log(Level.SEVERE, getErrorMsg() + ": internal action " + getFunctor() + " cannot be used in context or rules!");
                    return LogExpr.EMPTY_UNIF_LIST.iterator();
                }
                // calls IA's execute method
                Object oresult = ia.execute(ag.getTS(), un, ia.prepareArguments(this, un));
                if (oresult instanceof Boolean) {
                    if ((Boolean) oresult) {
                        if (ag.getLogger().isLoggable(Level.FINE))
                            ag.getLogger().log(Level.FINE, "     | internal action " + this + " executed " + " -- " + un);
                        return LogExpr.createUnifIterator(un);
                    } else {
                        if (ag.getLogger().isLoggable(Level.FINE))
                            ag.getLogger().log(Level.FINE, "     | internal action " + this + " failed " + " -- " + un);
                    }
                } else if (oresult instanceof Iterator) {
                    //if (kForChache == null) {
                    if (ag.getLogger().isLoggable(Level.FINE)) {
                        return new Iterator<Unifier>() {
                            @Override
                            public boolean hasNext() {
                                return ((Iterator<Unifier>) oresult).hasNext();
                            }

                            @Override
                            public Unifier next() {
                                Unifier r = ((Iterator<Unifier>) oresult).next();
                                ag.getLogger().log(Level.FINE, "     | internal action " + InternalActionLiteral.this + " option " + " -- " + r);
                                return r;
                            }
                        };
                    } else {
                        return ((Iterator<Unifier>) oresult);
                    }
                }
            } catch (ConcurrentModificationException e) {
                System.out.println("*-*-* " + getFunctor() + " concurrent exception - try later");
                e.printStackTrace();
                // try again later
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                }
                return executeCons(ag, un);
            } catch (Exception e) {
                logger.log(Level.SEVERE, getErrorMsg() + ": " + e.getMessage(), e);
            }
        }
        return LogExpr.EMPTY_UNIF_LIST.iterator();  // empty iterator for unifier
    }

    @SuppressWarnings("unchecked")
    public Iterator<Unifier> logicalConsequence(Agent ag, Unifier un) {
        return executeCons(ag, un);
    }

    public void setIA(InternalAction ia) {
        this.ia = ia;
    }

    public InternalAction getIA(Agent ag) throws Exception {
        if (ia == null && ag != null)
            ia = ag.getIA(getFunctor());
        return ia;
    }

    @Override
    public String getErrorMsg() {
        String src = getSrcInfo() == null ? "" : " (" + getSrcInfo() + ")";
        return "Error in internal action '" + this + "'" + src;
    }

    @Override
    public Term capply(Unifier u) {
        return new InternalActionLiteral(this, u);
    }

    public InternalActionLiteral clone() {
        return new InternalActionLiteral(this);
    }

    @Override
    public Literal cloneNS(Atom newnamespace) {
        return new InternalActionLiteral(newnamespace, this);
    }

    /**
     * get as XML
     */
    @Override
    public Element getAsDOM(Document document) {
        Element u = super.getAsDOM(document);
        u.setAttribute("ia", isInternalAction() + "");
        return u;
    }
}
