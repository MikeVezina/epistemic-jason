package jason.asSemantics.epistemic;

import jason.JasonException;
import jason.asSemantics.*;
import jason.asSemantics.epistemic.reasoner.EpistemicReasoner;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EpistemicExtension {
    private static final PredicateIndicator RANGE_PRED_IND = new PredicateIndicator("range", 1);
    private static final String LOG_OR_SYMBOL = "or";
    private static final String LOG_AND_SYMBOL = "and";
    private static final PredicateIndicator SINGLE_PRED_IND = new PredicateIndicator("single", 1);
    private TransitionSystem ts;
    private EpistemicReasoner reasoner;

    private DefaultBeliefBase groundingBase;
    private boolean modelCreated;

    public EpistemicExtension(TransitionSystem ts) {
        this.ts = ts;
        this.modelCreated = false;
        this.groundingBase = new DefaultBeliefBase();
        this.reasoner = new EpistemicReasoner();
    }

    public void modelCreateSem() {
        if (modelCreated) return;

        List<String> constraints = getModelCreationConstraints();

        reasoner.createModel(constraints);

        modelCreated = true;
    }

    protected List<String> getModelCreationConstraints() {
        List<String> constraints = new ArrayList<>();

        // Load range rule constraints
        constraints.addAll(getRangeConstraints());

        constraints.addAll(getSingleConstraints());

        // Load initial beliefs and
        constraints.addAll(getRuleConstraints());

        return constraints;
    }

    private List<String> getSingleConstraints() {
        // Filter range rules
        List<Rule> singleRules = getAllRules().stream().filter(r -> r.getPredicateIndicator().equals(SINGLE_PRED_IND)).collect(Collectors.toList());

        // Map each rule to its constraint
        return singleRules.stream().map(this::interpretSingle).collect(Collectors.toList());
    }

    public void modelUpdateSem() throws JasonException {
        // Get all belief events
        var events = ts.getC().getEvents().stream()
                .filter(e -> e.getTrigger().getType() == Trigger.TEType.belief);

        // Map each event '+/-e' to '+/-on(e)
        var onEvents = events
                .map(e -> new Event(new Trigger(
                        e.getTrigger().getOperator(),
                        e.getTrigger().getType(),
                        ASSyntax.createLiteral("on", e.getTrigger().getLiteral())
                ), e.getIntention()));

        onEvents.forEach(event -> {
            try {
                applyEventModel(event);
            } catch (JasonException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void applyEventModel(Event event) throws JasonException {
        DELEventModel eventModel = createEventModel(event);
        reasoner.applyEventModel(eventModel);
    }

    private DELEventModel createEventModel(Event event) throws JasonException {
        // Map each trigger to a list of relevant plans
        OnEvent onEvent = new OnEvent(event);

        var relPlans = ts.relevantPlans(event.getTrigger(), null);

        // Map each plan to a clone, where we can find all unifs (otherwise, Jason only finds the first unifier)
        var appPlans = applicablePlans(relPlans);

        if (appPlans == null || appPlans.isEmpty())
            return createDefaultEventModel(onEvent);
        createDefaultEventModel(onEvent);


        Set<DELEvent> delEvents = appPlans.stream().map(this::mapPlanToEvent).collect(Collectors.toSet());

        /*

          Next Steps:
          -


         */

        return new DELEventModel(delEvents);
    }

    private DELEventModel createDefaultEventModel(OnEvent onEvent) {

        Pred pEv = new Pred(onEvent.getEventPred());
        DELEvent defEv = new DELEvent(onEvent.getEvent().getTrigger());
        if (onEvent.isAddition())
            defEv.addPostCondition(pEv.toString(), Literal.LTrue.toString());
        else
            defEv.addPostCondition(pEv.toString(), Literal.LFalse.toString());

        return new DELEventModel(Set.of(defEv));
    }

    private DELEvent mapPlanToEvent(Option option) {
        DELEvent ev = new DELEvent(option);
        Plan unifPlan = option.getPlan().capply(option.getUnifier());

        String propPrecondition = pareAndProp(unifPlan.getContext());
        ev.setPreCondition(propPrecondition);

        PlanBody cur = unifPlan.getBody();
        while (cur != null) {
            if (cur.getBodyType() == PlanBody.BodyType.addBel) {
                ev.addPostCondition(prop(cur.getBodyTerm()), Literal.LTrue.toString());
            } else if (cur.getBodyType() == PlanBody.BodyType.delBel) {
                ev.addPostCondition(prop(cur.getBodyTerm()), Literal.LFalse.toString());
            }
            cur = cur.getBodyNext();
        }
//        for (var act : unifPlan.getBody())

        return ev;
    }


    /**
     * Copied + modified from ts.applicablePlans, since we need a custom logical consequence function.
     *
     * @param rp
     * @return
     */
    public List<Option> applicablePlans(List<Option> rp) {
        synchronized (ts.getC().syncApPlanSense) {
            if (rp == null)
                return null;

            List<Option> ap = null;
            for (Option opt : rp) {
                LogicalFormula context = opt.getPlan().getContext();
                if (ts.getLogger().isLoggable(Level.FINE))
                    ts.getLogger().log(Level.FINE, "option for " + ts.getC().getSelectedEvent().getTrigger() + " is plan " + opt.getPlan().getLabel() + " " + opt.getPlan().getTrigger() + " : " + context + " -- with unification " + opt.getUnifier());

                if (context == null) { // context is true
                    if (ap == null) ap = new LinkedList<>();
                    ap.add(opt);
                    if (ts.getLogger().isLoggable(Level.FINE))
                        ts.getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is applicable with unification " + opt.getUnifier());
                } else {
                    //  boolean allUnifs = opt.getPlan().isAllUnifs();
                    boolean allUnifs = true;

                    Iterator<Unifier> r = logicalCons(context, groundingBase, opt.getUnifier());
                    boolean isApplicable = false;
                    if (r != null) {
                        while (r.hasNext()) {
                            isApplicable = true;
                            opt.setUnifier(r.next());

                            if (ap == null) ap = new LinkedList<>();
                            ap.add(opt);

                            if (ts.getLogger().isLoggable(Level.FINE))
                                ts.getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is applicable with unification " + opt.getUnifier());

                            if (!allUnifs) break; // returns only the first unification
                            if (r.hasNext()) {
                                // create a new option for the next loop step
                                opt = new Option(opt.getPlan(), null);
                            }
                        }
                    }

                    if (!isApplicable && ts.getLogger().isLoggable(Level.FINE))
                        ts.getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is not applicable");
                }

            }
            return ap;
        }
    }

    /**
     * Gets log cons with respect to a different set of literals
     */
    private Iterator<Unifier> logicalCons(LogicalFormula context, BeliefBase groundingBase, Unifier unifier) {
        if (context == null)
            return null;

        // Need to clone agent and set up new belief base
        var customAg = ts.getAg().clone(ts.getAgArch());
        customAg.setBB(groundingBase);

        return context.logicalConsequence(customAg, unifier);
    }

    private List<String> getRuleConstraints() {
        // Filter range rules
        var standardRuleStm = getAllRules().stream()
                .filter(r -> !r.getPredicateIndicator().equals(RANGE_PRED_IND))
                .filter(r -> !r.getPredicateIndicator().equals(SINGLE_PRED_IND))
                .filter(r -> r.getNS() == Literal.DefaultNS);


        Set<Literal> allGroundRules = standardRuleStm.map(this::getGroundConsequences).reduce((r1, r2) -> {
            Set<Literal> joined = new HashSet<>(r1);
            joined.addAll(r2);
            return joined;
        }).orElse(new HashSet<>());

        Map<Literal, Set<LogicalFormula>> headToBodyMap = new HashMap<>();

        for(Literal ruleLit : allGroundRules)
        {
            Rule rule = (Rule) ruleLit;

            Literal head = rule.getHead();
            LogicalFormula body = rule.getBody();

            if(!headToBodyMap.containsKey(head))
                headToBodyMap.put(head, new HashSet<>());

            headToBodyMap.get(head).add(body);

        }


        List<Rule> nonRangeRules = standardRuleStm.collect(Collectors.toList());

        // Map each rule to its constraint
        return nonRangeRules.stream().map((Rule r) -> interpretRuleConstraint(r, headToBodyMap)).collect(Collectors.toList());

    }

    private String interpretRuleConstraint(Rule r, Map<Literal, Set<LogicalFormula>> headToBodyMap) {
        // Rule must be ground
        if(!r.getHead().isGround())
            throw new RuntimeException("Rule " + r + " must be ground!");

        // 1. Find all groundings of range
        Set<Literal> ground = getGroundConsequences(r);

        // Map into pairs of propositionalized (ground body -> ground head)
        // TODO: Note that rule body may not be ground!
        List<List<Term>> implicationLists = ground.stream()
                .map(l -> (Rule) l) // Map each lit to a rule
                .map(rule -> List.of(pare(rule.getHead()), pare(rule.getBody())))
                .collect(Collectors.toList());

        // Insert all other initial beliefs:
        for (Literal bel : ts.getAg().getInitialBels()) {
            if (bel.isRule())
                continue;

            // true -> bel (bel is true in all worlds)
            implicationLists.add(List.of(bel, Literal.LTrue));
        }


        List<String> implications = implicationLists.stream()
                .filter(l -> l.get(1) != Literal.LFalse) // Bodies that collapse to false are filtered out
                .map(l -> {
                    // Map each implication pair to its corresponding string.
                    // Pairs with a 'true' body will just be propositionalized as a head (no implication)
                    // those with 'false' body will be excluded (though this shouldn't happen due to obtaining log. consequences first)
                    Term head = l.get(0);
                    Term body = l.get(1);

                    if (body == Literal.LTrue)
                        return prop(head);

                    return prop(body) + " -> " + prop(head);
                })
                .collect(Collectors.toList());





        // 2. For each ground lit l:
        //  a. convert to disjunction: l or not l
        //  b. add {l, ~l} to grounding set

        for (var list : implicationLists) {
            // Add head literal to grounding base if not seen before
            groundingBase.add((Literal) list.get(0));

            // TODO: Figure out if we should also add negation if applicable (what about non-ranged lits?)
        }


        /*
            (Range 1, Constraint 1): Get all groundings of rule
            (Range 2): Convert groundings to disjunction



         */

        return createConjunction(implications);
    }

    private String prop(Term pared) {
        if (!pared.isGround())
            throw new RuntimeException("Term must be ground to be propositionalized: " + pared);

        if (pared instanceof LogExpr)
            return propExpr((LogExpr) pared);

        if (pared instanceof Literal)
            return propLit((Literal) pared);

        throw new RuntimeException("Prop not supported?");
    }

    private String propExpr(LogExpr pared) {

        return "(" + prop(pared.getLHS()) + " " + pared.getOp().toString() + " " + pare(pared.getRHS()) + ")";
    }

    private String pareAndProp(Term unpared) {
        return prop(pare(unpared));
    }


    private List<String> getRangeConstraints() {
        // Filter range rules
        List<Rule> rangeRules = getAllRules().stream().filter(r -> r.getPredicateIndicator().equals(RANGE_PRED_IND)).collect(Collectors.toList());

        // Map each rule to its constraint
        return rangeRules.stream().map(this::interpretRange).collect(Collectors.toList());
    }

    private String interpretRange(Rule r) {

        // 1. Find all groundings of range
        Set<Literal> ground = getGroundConsequences(r);

        // Isolate ground head and remove 'Range' wrapper
        ground = ground.stream().map(l -> (Literal) l.getTerm(0)).collect(Collectors.toSet());

        // 2. For each ground lit l:
        //  a. convert to disjunction: l or not l
        //  b. add {l, ~l} to grounding set
        List<String> disjunctions = new ArrayList<>();
        for (Literal l : ground) {
            // Add literal pos and neg
            Literal neg = ((Literal) l.clone()).setNegated(Literal.LNeg);
            Literal pos = ((Literal) l.clone()).setNegated(Literal.LPos);

            // Create disj form string, and add both forms to ground base
            disjunctions.add(createDisjunction(Set.of(prop(pos), prop(neg))));
            groundingBase.add(neg);
            groundingBase.add(pos);
        }


        /*
            (Range 1, Constraint 1): Get all groundings of rule
            (Range 2): Convert groundings to disjunction



         */

        return createConjunction(disjunctions);
    }

    private String interpretSingle(Rule r) {
// single(X) => single(X = {a, b, c})
        // Add a, b, c to grounding set
        // Constraint: (a and not b and not c) or ...

        // 1. Find all groundings of single rule r
        Set<Literal> ground = getGroundConsequences(r);

        // Isolate ground head and remove 'Range' wrapper
        ground = ground.stream().map(l -> (Literal) l.getTerm(0)).collect(Collectors.toSet());

        if (ground.isEmpty())
            return "";

        String csProps = ground.stream().map(this::prop).collect(Collectors.joining(", "));

        return "exact(1, [" + csProps + "])";
    }

    private String oldInterpretSingle(Rule r) {
        // single(X) => single(X = {a, b, c})
        // Add a, b, c to grounding set
        // Constraint: (a and not b and not c) or ...

        // 1. Find all groundings of single rule r
        Set<Literal> ground = getGroundConsequences(r);

        // Isolate ground head and remove 'Range' wrapper
        ground = ground.stream().map(l -> (Literal) l.getTerm(0)).collect(Collectors.toSet());

        // 2. For each ground lit l:
        //  a. convert to disjunction: l or not l
        //  b. add {l, ~l} to grounding set
        List<String> allConjunctions = new ArrayList<>();
        for (Literal postLit : ground) {
            // Add literal pos and neg
            Literal pos = ((Literal) postLit.clone()).setNegated(Literal.LPos);

            // Add pos and neg to Ground set
            groundingBase.add(pos);
            groundingBase.add(new LiteralImpl(pos).setNegated(Literal.LNeg));

            Set<String> conjunctions = new HashSet<>();
            conjunctions.add(prop(pos));

            for (Literal negLit : ground) {
                if (negLit == postLit)
                    continue;

                Literal neg = ((Literal) negLit.clone()).setNegated(Literal.LNeg);
                conjunctions.add(prop(neg));
            }


            // Create disj form string, and add both forms to ground base
            allConjunctions.add(createConjunction(conjunctions));

        }


        /*
            (Range 1, Constraint 1): Get all groundings of rule
            (Range 2): Convert groundings to disjunction



         */

        return createDisjunction(allConjunctions);
    }


    private Set<Literal> getGroundConsequences(Literal l) {
        // Find all groundings of range
        Set<Literal> ground = new HashSet<>();

        LogicalFormula litForCons = l;

        if (l.isRule())
            litForCons = ((Rule) l).getBody();

        // Get all consequences
        var cons = litForCons.logicalConsequence(ts.getAg(), new Unifier());

        // No consequences
        if (cons == null)
            return ground;

        // Add head if already ground
        if (l.isGround())
            ground.add(l);

        // Iterate all unifiers
        while (cons.hasNext()) {
            var next = cons.next();
            Literal res = (Literal) l.capply(next);

            boolean isGround = res.isGround();

            // rule.isGround always returns false, need to check head manually
            // NOTE: do not check if rule body is ground, think of case where 'x(1, 2) OR y(A, Z)' unifies head
            if (res.isRule()) {
                Rule resRule = (Rule) res;
                isGround = resRule.getHead().isGround();
            }

            if (isGround)
                ground.add(res);
        }

        return ground;
    }


    private Term pare(Term t) {
        if (!t.isGround())
            throw new RuntimeException("Term must be ground to be pared: " + t);

        if (t instanceof LogExpr)
            return pareExpr((LogExpr) t);

        // Terms that simplify to true, e.g. rel expr (5 = 5), int actions (.member(5, [1, 5]))
        if (t instanceof RelExpr || t instanceof InternalActionLiteral)
            return Literal.LTrue;

        if (t instanceof Literal)
            return pareLit((Literal) t);

//        if (t instanceof LogicalFormula)
//            return pareForm((LogicalFormula) t);

        System.out.println("Unknown term type: " + t.getClass().getSimpleName());
        return t;
    }

    /**
     * l1 & l2 => pare(l1) & pare(l2)
     * l1 & l2 => if (pare(l1) & LTrue) OR (LTrue & pare(l2)) or
     */
    private Term pareExpr(LogExpr t) {
        if (t.getOp() == LogExpr.LogicalOp.and) {
            var formLeft = (LogicalFormula) pare(t.getLHS());
            var formRight = (LogicalFormula) pare(t.getLHS());

            if (formLeft == Literal.LTrue)
                return formRight;

            if (formRight == Literal.LTrue)
                return formLeft;

            // Otherwise, return pared expression.
            return new LogExpr(formLeft, LogExpr.LogicalOp.and, formRight);
        }


        if (t.getOp() == LogExpr.LogicalOp.or) {
            var formLeft = (LogicalFormula) pare(t.getLHS());
            var formRight = (LogicalFormula) pare(t.getLHS());

            if (formLeft == Literal.LFalse)
                return formRight;

            if (formRight == Literal.LFalse)
                return formLeft;

            // Otherwise, return pared expression.
            return new LogExpr(formLeft, LogExpr.LogicalOp.or, formRight);
        }


//        if(t.getOp() == LogExpr.LogicalOp.not)
        else {
            // E.g.: not(~test(X, Y)) = for all possible unifiers (X, Y), ~(~test(X, Y)) holds true
            // Special case, where we need to potentially ground all literals
            // More complicated: not(test(X, Y) & not(other(X, Z) OR other(Z, X)))
            //      With gs = {test(1, 2), test(3, 4), test(5, 5), other(1, 4), other(4, 1), other(5, 5)}
            //      Meaning => not([test(1, 2), test(3, 4), ] ???
            // TODO: Handle this, but for now, we assume only strong negation in constraint rules
            throw new RuntimeException("Unsupported paring: " + t.getOp().toString());
        }
    }

    private Literal pareLit(Literal lit) {
        if (lit.isGround())
            return lit;
        // if lit is not ground, return false lit
        // This covers case of possible disjunction in log expr: x(1, 2) OR y(A, Z)
        // Returns 'x(1, 2) OR FALSE'
        return Literal.LFalse;


    }

    private String propLit(Literal l) {
        if (!l.isGround())
            throw new RuntimeException("Literal must be ground for prop!");

        if (l == Literal.LTrue || l == Literal.LFalse || !l.negated())
            return l.toString();

        // Convert literal to prop sentence form.
        return "("
                + (l.negated() ? "not " : "")
                + l.setNegated(Literal.LPos)
                + ")";
    }

    private String createForm(Collection<String> lits, String symbol) {
        // Map each lit to string, then join with log. OR symbol
        return "(" + String.join(" " + symbol + " ", lits) + ")";
    }

    private String createConjunction(Collection<String> literals) {
        return createForm(literals, LOG_AND_SYMBOL);
    }

    private String createDisjunction(Collection<String> literals) {
        return createForm(literals, LOG_OR_SYMBOL);
    }

    private List<Rule> getAllRules() {
        List<Rule> allRules = new ArrayList<>();
        for (var b : ts.getAg().getBB())
            if (b.isRule()) allRules.add((Rule) b);
        return allRules;
    }

}
