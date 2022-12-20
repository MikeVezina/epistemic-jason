package jason.asSemantics.epistemic;

import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import jason.runtime.Settings;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.Set;

public class Rewriter {

    // Containing both ground managed beliefs and the rewrite rules.
    private final BeliefBase beliefBase;

    public Rewriter(@NotNull BeliefBase beliefBase, @NotNull Set<Literal> managed) {
        this.beliefBase = beliefBase;
//        this.managed = managed;
    }

    /*
        To easily rewrite, we simply remove any weak negation ('not') from the formula
     */
    public LogicalFormula rewrite(LogicalFormula t) {
//        if (!t.isGround()) throw new RuntimeException("Term must be ground to be pared: " + t);

        if (t instanceof LogExpr) return rewriteExpr((LogExpr) t);

        // Terms that rewrite to true, e.g. rel expr (5 = 5) or (X = 5), int actions (.member(5, [1, 5]))
        // Can't assume true (unevaluated)
        // Todo: handle literals in rel expr
//        if (t instanceof RelExpr)
//            return rewriteRelExpr((RelExpr) t);
//
//        if(t instanceof InternalActionLiteral)
//            return rewriteIA((InternalActionLiteral) t); // instead of Literal.LTrue;
//
//        if (t instanceof VarTerm) return rewriteVarTerm((VarTerm) t);
//
//        if (t instanceof LiteralImpl) return rewriteLitImpl((LiteralImpl) t);
//
//        if (t instanceof Literal) return rewriteLiteral((Literal) t);

//        if (t instanceof LogicalFormula)
//            return pareForm((LogicalFormula) t);

        System.out.println("Unknown term type: " + t.getClass().getSimpleName() + " for " + t);
        return t;
    }

    protected LogicalFormula rewriteVarTerm(VarTerm t) {
        return null;
    }

    protected LogicalFormula rewriteRelExpr(RelExpr relExpr) {
        // TODO: handle lits in rel expression
        return relExpr;
    }

    protected LogicalFormula rewriteLitImpl(LiteralImpl t) {
        // These are the terms that we would like to ground and rewrite
//        var can = beliefBase.getCandidateBeliefs(t, new Unifier());

        // For all candidates c:
        // Is c a Belief?
        // Is c a rule?


        return t;
    }

    protected LogicalFormula rewriteExpr(LogExpr term) {
        // Handle: AND, OR, NOT

        if (term.getOp().equals(LogExpr.LogicalOp.not))
            return rewrite(term.getLHS());

        return new LogExpr(rewrite(term.getLHS()), term.getOp(), rewrite(term.getRHS()));
    }

    protected LogicalFormula rewriteIA(InternalActionLiteral action) {
        return action;
    }


    protected LogicalFormula rewriteLiteral(Literal literal) {
        return literal;
    }

    public BeliefBase getBeliefBase() {
        return beliefBase;
    }


    public static void main(String[] args) {
        try {

            DefaultBeliefBase defaultBeliefBase = new DefaultBeliefBase();
            Agent a = new Agent();
            a.setBB(defaultBeliefBase);

            a.setTS(new TransitionSystem(a, new Circumstance(), new Settings(), new AgArch() {
                @Override
                public boolean isRunning() {
                    return true;
                }
            }));

            a.initAg();

//            a.getTS().getAgArch().isRunning();

            defaultBeliefBase.add(ASSyntax.parseLiteral("loc(1)"));
            defaultBeliefBase.add(ASSyntax.parseLiteral("loc(2)"));
            defaultBeliefBase.add(ASSyntax.parseRule("loc(X) :- .member(X, [2]) | .member(X, [5, 6])."));
            defaultBeliefBase.add(ASSyntax.parseRule("loc(3) :- loc(1) & loc(2)."));
            defaultBeliefBase.add(ASSyntax.parseRule("loc(3) :- loc(1) & loc(2) & loc(2)."));
            defaultBeliefBase.add(ASSyntax.parseRule("loc(4) :- loc(1) | loc(2)."));
            defaultBeliefBase.add(ASSyntax.parseRule("loc(X) :- (X >= 10)."));

            LogicalFormula form = ASSyntax.parseFormula("loc(X) | not loc(Y)"); //ASSyntax.parseFormula("(not X & (not (.member(Y, [1, 2]))))");
            System.out.println("Original: " + form);

//            var r = new Rewriter(null, null);
//            var newForm = r.rewrite(form);

            var cons = form.logicalConsequence(a, new Unifier());

            while(cons != null && cons.hasNext())
            {
                System.out.println(cons.next());
            }

            var iter = form.rewriteConsequences(a, new Unifier());

            while (iter != null && iter.hasNext()) {
                var val = iter.next();
//                System.out.println("Rewritten as: " + val);
                System.out.println( form.capply(val.getUnifier()) + " rewritten as: " + val.getKey() + " --- with: " + val.getValue());
            }
//            newForm.logicalConsequence()

//            System.out.println("Consequences: " + r.rewrite(form));
        } catch (Exception pe)
        {
            pe.printStackTrace();
        }


    }
}
