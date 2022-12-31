package jason.asSemantics.epistemic;

import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import jason.runtime.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;

import static jason.asSyntax.ASSyntax.createLiteral;
import static jason.asSyntax.ASSyntax.parseLiteral;
import static jason.asSyntax.Literal.LFalse;
import static jason.asSyntax.Literal.LTrue;

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

            DefaultBeliefBase initialBeliefs = new DefaultBeliefBase();
            initialBeliefs.add(ASSyntax.parseLiteral("~none"));


            DefaultBeliefBase defaultBeliefBase = new DefaultBeliefBase();

            for (var b : initialBeliefs)
                defaultBeliefBase.add(b);

            Agent a = new Agent();
            a.setBB(defaultBeliefBase);

            a.setTS(new TransitionSystem(a, new Circumstance(), new Settings(), new AgArch() {
                @Override
                public boolean isRunning() {
                    return true;
                }
            }));

            a.initAg();


            // On plan for bel(1)
            a.getPL().add(new Plan(null,
                    new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, createLiteral("on", createLiteral("bel", new VarTerm("Y")))),
                    ASSyntax.parseFormula("~loc(X)"),
                    new PlanBodyImpl(PlanBody.BodyType.addBel, ASSyntax.createLiteral("loc", new VarTerm("X")))));


            a.getPL().add(ASSyntax.parsePlan("+on(moved(right))\n" +
                    "    : loc(X, Y)\n" +
                    "    <-  -loc(X, Y);\n" +
                    "        +loc(X + 1, Y)."));


//            a.getTS().getAgArch().isRunning();

            // Range values (for grounding)
            // defaultBeliefBase.add(ASSyntax.parseLiteral("loc(1)"));
            // defaultBeliefBase.add(ASSyntax.parseLiteral("~loc(1)"));
            // defaultBeliefBase.add(ASSyntax.parseLiteral("loc(1)"));
            // defaultBeliefBase.add(ASSyntax.parseLiteral("loc(2)"));
            // defaultBeliefBase.add(ASSyntax.parseLiteral("loc(3)"));
            // defaultBeliefBase.add(ASSyntax.parseLiteral("~loc(2)"));
            // // defaultBeliefBase.add(ASSyntax.parseLiteral("loc(3)"));
            // // defaultBeliefBase.add(ASSyntax.parseLiteral("~loc(3)"));
            // // defaultBeliefBase.add(ASSyntax.parseLiteral("none"));
            // // defaultBeliefBase.add(ASSyntax.parseLiteral("~none"));
            //
            //
            // defaultBeliefBase.add(ASSyntax.parseRule("range(none) :- true."));
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(3) :- loc(1) & loc(2)."));
            //
            // var res = ASSyntax.parseLiteral("poss(loc(X))").logicalConsequence(a, new Unifier());
            // while (res != null && res.hasNext())
            // {
            //     System.out.println(res.next());
            // }
            //  res = ASSyntax.parseLiteral("poss(loc(2))").logicalConsequence(a, new Unifier());
            // while (res != null && res.hasNext())
            // {
            //     System.out.println(res.next());
            // }
            //
            // res = ASSyntax.parseLiteral("loc(X)").logicalConsequence(a, new Unifier());
            // while (res != null && res.hasNext())
            // {
            //     System.out.println(res.next());
            // }


            defaultBeliefBase.add(ASSyntax.createRule(createLiteral("range", ASSyntax.createLiteral("none")), LTrue));

            int locs = 100;
            for (int x = 0; x < locs; x++) {
                for (int y = 0; y < locs; y++) {
                    if ((x * y) % 500 == 0)
                        System.out.println(x + ", " + y);
                    // defaultBeliefBase.add(createLiteral("range", createLiteral("loc", ASSyntax.createNumber(x))));
                    defaultBeliefBase.add(ASSyntax.createRule(createLiteral("range", ASSyntax.createLiteral("loc", ASSyntax.createNumber(x), ASSyntax.createNumber(y))), LTrue));

                    // for (int j = 0; j < locs; j++) {
                    //     if (x != j)
                    //         defaultBeliefBase.add(ASSyntax.createRule(createLiteral("locs", ASSyntax.createNumber(x), ASSyntax.createNumber(j)), LTrue));
                    // }
                }
            }

            // defaultBeliefBase.add(ASSyntax.parseRule("range(none). :- (not loc(1) & not loc(2) & not loc(3))."));


            // We need a way to 'expand' a conjunction, maybe using internal actions.


            Unifier u = new Unifier();
            // u.bind(new VarTerm("Hi"), new LogExpr(LFalse, LogExpr.LogicalOp.and, LTrue));

            // Literal res = (Literal) ASSyntax.parseLiteral("test(Hi)").capply(u);

            // defaultBeliefBase.add(ASSyntax.parseRule("range(loc(X)) :- .member(X, [1, 2, 3])."));
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(Z) :- .ground(Z) & .findall(~loc(X), range(loc(X)) & X \\== Z , List) & .big_and(Y, List) & Y."));
            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(Z) :- .ground(Z) & .findall(loc(X), range(loc(X)) & X \\== Z , List) & .big_or(Y, List) & Y."));


            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(Z) :- .belief(range(loc(Z))) & .belief(range(loc(X))) & X \\== Z & loc(Z)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(Z) :- locs(X, Z) & loc(X)."));


            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(1) :- loc(2) | loc(3)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(2) :- loc(1) | loc(3)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(3) :- loc(1) | loc(2)."));


            defaultBeliefBase.add(ASSyntax.parseRule("none :- .findall(not(loc(X, Y)), range(loc(X, Y)), List) & .big_and(Y, List) & Y."));
            defaultBeliefBase.add(ASSyntax.parseRule("~loc(X1, Y1) :- range(loc(X1, Y1)) & loc(X2, Y2) & (X1 \\== X2 | Y1 \\== Y2)."));
            defaultBeliefBase.add(ASSyntax.parseRule("obs(down) :- loc(1, 1) | loc(2, 1)."));
            defaultBeliefBase.add(ASSyntax.parseRule("~obs(D) :- .member(D, [up, down, left, right]) & not obs(D)."));

            a.getTS().getEpistemic().modelCreateSem();


            var rew = ASSyntax.parseLiteral("~obs(down)").rewriteConsequences(a, new Unifier());

            while (rew != null && rew.hasNext()) {
                System.out.println(rew.next().getFormula().simplify());
            }


            // a.getTS().getC().addEvent(new Event(new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, createLiteral("bel", new NumberTermImpl(1))), null));
            a.getTS().getC().addEvent(new Event(new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, parseLiteral("moved(right)")), null));

            a.getTS().getEpistemic().modelUpdateSem();


            // EpistemicExtension ext = new EpistemicExtension(a.getTS());

            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(Z) :- .ground(Z) & .belief(~loc(X)) & X \\== Z."));
            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(30) :- .big_or(Y, [a, b, c]) & (X = (loc(1) | loc(2))) & Y."));
            // defaultBeliefBase.add(ASSyntax.parseRule("~loc(X) :- .member(X, [1, 2, 3]) & .member(X2, [1, 2, 3]) & X \\== X2 & loc(X2)."));
            //
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(3) :- loc(1) & loc(2)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(3) :- loc(1) & loc(2) & loc(2)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(4) :- loc(1) | loc(2)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(X) :- (X = 10)."));
            // defaultBeliefBase.add(ASSyntax.parseRule("loc(X) :- (X >= 10)."));

            propRules(ASSyntax.parseLiteral("none"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("~none"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("loc(1)"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("~loc(1)"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("loc(2)"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("~loc(2)"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("loc(3)"), defaultBeliefBase, a);
            propRules(ASSyntax.parseLiteral("~loc(3)"), defaultBeliefBase, a);


            LogicalFormula form = ASSyntax.parseFormula("~loc(X)"); //ASSyntax.parseFormula("(not X & (not (.member(Y, [1, 2]))))");
//            LogicalFormula form = ASSyntax.parseFormula("loc(X) | not loc(Y)"); //ASSyntax.parseFormula("(not X & (not (.member(Y, [1, 2]))))");
            System.out.println("Original: " + form);

//            var r = new Rewriter(null, null);
//            var newForm = r.rewrite(form);

            var sec = ASSyntax.parseFormula("test(1)").logicalConsequence(a, new Unifier()); //form.logicalConsequence(a, new Unifier());


            while (sec != null && sec.hasNext()) {
                System.out.println(sec.next());
            }

            var iter = form.rewriteConsequences(a, new Unifier());

            while (iter != null && iter.hasNext()) {
                var val = iter.next();
                var result = (LogicalFormula) form.capply(val.getUnifier());
                System.out.println(result + " rewritten as: " + val.getKey() + " --- with: " + val.getValue());
                System.out.println(result + " simplified to: " + val.getKey().simplify());
                System.out.println(result + " Prop: " + val.getKey().simplify().toPropFormula());
                System.out.println("======");
            }
//            newForm.logicalConsequence()

//            System.out.println("Consequences: " + r.rewrite(form));
        } catch (Exception pe) {
            pe.printStackTrace();
        }


    }

    private static void propRules(Literal lit, DefaultBeliefBase defaultBeliefBase, Agent a) throws Exception {
        var res = defaultBeliefBase.getCandidateBeliefs(lit, new Unifier());

        while (res != null && res.hasNext()) {
            Literal next = res.next();
            if (!next.isRule())
                continue;

            // Get rule
            Rule r = (Rule) next;

            var iter = r.getBody().rewriteConsequences(a, new Unifier());

            // var clonedBody = (LogicalFormula) r.getBody().clone();

            while (iter != null && iter.hasNext()) {
                var val = iter.next();
                var cOrig = (LogicalFormula) next.capply(val.getUnifier());
                // clonedBody = (LogicalFormula) clonedBody.capply(val.getUnifier());
                System.out.println(cOrig + " rewritten as: " + val.getFormula() + " --- with: " + val.getUnifier());
                System.out.println("Prop: " + val.getFormula().simplify().toPropFormula() + " implies " + r.headCApply(val.getUnifier()));
                System.out.println("======");
            }

        }


    }
}
