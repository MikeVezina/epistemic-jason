package jason.asSemantics.epistemic;

import jason.JasonException;
import jason.RevisionFailedException;
import jason.asSemantics.*;
import jason.asSemantics.epistemic.reasoner.EpistemicReasoner;
import jason.asSemantics.epistemic.reasoner.formula.*;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

enum ReasonerType {
    PAL,
    DEL
}

public class EpistemicExtension implements Propositionalizer, CircumstanceListener {
    private static final PredicateIndicator RANGE_PRED_IND = new PredicateIndicator("range", 1);
    private static final PredicateIndicator SINGLE_PRED_IND = new PredicateIndicator("single", 1);

    private final ReasonerType reasonerType;
    private TransitionSystem ts;
    private EpistemicReasoner reasoner;
    private boolean modelCreated;

    public EpistemicExtension(TransitionSystem ts, ReasonerType reasonerType) {
        this.ts = ts;
        this.modelCreated = false;
        this.reasonerType = reasonerType;
        this.reasoner = new EpistemicReasoner(this);
    }

    public EpistemicExtension(TransitionSystem ts) {
        // Use DEL as default
        this(ts, ReasonerType.DEL);
    }


    public void modelCreateSem() {
        // Do not re-invoke
        if (modelCreated) return;

        long startTime = System.nanoTime();
        List<Formula> constraints = getModelCreationConstraints();
        long endConstraintTime = System.nanoTime();

        this.ts.getAg().getLogger().info("Constraint Consequences Time (ms): " + ((endConstraintTime - startTime) / 1000000));

        endConstraintTime = System.nanoTime(); // reset for next calc

        boolean result = reasoner.createModel(constraints);
        long endGenerationTime = System.nanoTime();

        this.ts.getAg().getLogger().info("Model Generation Time (ms): " + ((endGenerationTime - endConstraintTime) / 1000000));
        this.ts.getAg().getLogger().info("Total Model Creation Time (ms): " + ((endGenerationTime - startTime) / 1000000));
        if (!result) {
            this.ts.getAg().getLogger().info("Failed to create epistemic model from constraints");
        }
        modelCreated = true;
    }

    /**
     * Find all constraints for model creation:
     * - Initial beliefs
     * - Initial Ranges
     * - Constraint rules (from range values only)
     *
     * @return
     */
    protected List<Formula> getModelCreationConstraints() {
        List<Formula> constraints = new ArrayList<>();

        // Add initial beliefs to constraints
        this.ts.getAg().getBB().forEach(l -> {
            if (!l.isRule() && l.getNS() == Literal.DefaultNS) {
                // Decision: should percepts be part of the initial model if they are available?
                // Cons of having them: initial model may not be able to be cached.
                // These are captured by event models and special event plans. We may mis-represent the event plans if we capture it here.
                // E.g. if +x is a public announcement of x, the impact of this event will change if we include x in the initial model
                if (!l.hasSource(new Atom("percept"))) {
                    // groundingBase.add(l);
                    constraints.add(l.toPropFormula());
                }
            }
        });

        System.out.println("Propositionalized beliefs");

        // Process rule semantics
        List<Literal> rangeValues = new ArrayList<>();

        // Load propositionalized range rules (and populate range values)
        // Ranges are added to belief base in this call
        constraints.addAll(getRangeConstraints(this.ts.getAg(), rangeValues));
        int rangeCons = constraints.size();
        System.out.println("Range Constraints: " + rangeCons);

        // constraints.addAll(getSingleConstraints(allRules));
        // int singleConstraints = constraints.size() - rangeCons;
        // System.out.println("Single Constraints: " + singleConstraints);

        // Load initial beliefs and
        constraints.addAll(getRangeConstraintRules(ts.getAg(), rangeValues));
        int constraintRules = constraints.size() - rangeCons;
        // int constraintTime = constraints.size() - singleConstraints - rangeCons;
        System.out.println("Standard Rule Constraints: " + constraintRules);

        return constraints;
    }

    private Set<Formula> getSingleConstraints(List<Rule> allRules) {
        // Filter range rules
        List<Rule> singleRules = allRules.stream().filter(r -> r.getPredicateIndicator().equals(SINGLE_PRED_IND)).collect(Collectors.toList());

        // Map each rule to its constraint
        Set<Formula> constraints = new HashSet<>();

        for (var r : singleRules)
            constraints.addAll(interpretSingle(r));

        return constraints;
    }

    public void modelUpdateSem() throws JasonException {
        // Get all belief events
        var events = ts.getC().getEvents().stream().filter(e -> e.getTrigger().getType() == Trigger.TEType.belief);

        // Map each event '+/-e' to '+/-on(e)
        var onEvents = events.map(e -> new Event(new Trigger(e.getTrigger().getOperator(), e.getTrigger().getType(), ASSyntax.createLiteral("on", e.getTrigger().getLiteral().clearAnnots())), e.getIntention()));

        onEvents.forEach(event -> {
            try {
                applyEventModel(event);
            } catch (JasonException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * Apply PAL/DEL event model based on current configuration
     * @param event
     * @throws JasonException
     */
    public void applyEventModel(Event event) throws JasonException {
        long startTime = System.nanoTime();

        // Map each trigger to an 'on' event representation
        OnEvent onEvent = new OnEvent(event);

        // Skip non-default NS for now
        if (onEvent.getEventLit().getNS() != Literal.DefaultNS) {
            ts.getLogger().info("Skipping on event for " + event.getTrigger());
            return;
        }

        // Apply PAL announcement
        if(reasonerType == ReasonerType.PAL)
        {
            DELEventModel palModel = new DELEventModel(Set.of(
                    new DELEvent(onEvent.getEventLit().toString(), simplifyAndProp(onEvent.getEventLit()))
            ));
            reasoner.applyEventModel(palModel);
        }
        else {
            DELEventModel eventModel = createEventModel(onEvent);
            long consEndTime = System.nanoTime();
            System.out.println("Time to find applicable 'on' plans (ms): " + (consEndTime - startTime) / 1000000);
            reasoner.applyEventModel(eventModel);
        }

        long reasonerEndTime = System.nanoTime();
        // System.out.println("Time to apply event model (ms): " + (reasonerEndTime - consEndTime) / 1000000);
        System.out.println("Total event time (ms): " + (reasonerEndTime - startTime) / 1000000);
    }

    private DELEventModel createEventModel(OnEvent onEvent) throws JasonException {
        var relPlans = ts.relevantPlans(onEvent.getTrigger(), null);

        // Map each plan to a clone, where we can find all unifs (otherwise, Jason only finds the first unifier)
        var appPlans = applicablePlans(relPlans);

        if (appPlans == null || appPlans.isEmpty())
            return createDefaultEventModel(onEvent);


        Set<DELEvent> delEvents = appPlans.stream().map(this::mapPlanToEvent).collect(Collectors.toSet());
        return new DELEventModel(delEvents);
    }

    private DELEventModel createDefaultEventModel(OnEvent onEvent) {

        Pred pEv = new Pred(onEvent.getEventPred());
        DELEvent defEv = new DELEvent(onEvent.getEvent().getTrigger().getOperator().toString() + onEvent.getEventLit());
        if (onEvent.isAddition() && !onEvent.getEventLit().negated())
            defEv.addPostCondition(new PropFormula(pEv), new PropFormula(new Pred(Literal.LTrue)));
        else
            defEv.addPostCondition(new PropFormula(pEv), new PropFormula(new Pred(Literal.LFalse)));

        return new DELEventModel(Set.of(defEv));
    }

    private DELEvent mapPlanToEvent(Option option) {
        Plan unifPlan = option.getPlan().capply(option.getUnifier());

        // Erase label from plan for the sake of event id readability
        Plan planNoLabel = ((Plan) unifPlan.clone());
        planNoLabel.delLabel();

        DELEvent ev = new DELEvent(planNoLabel);

        // Only process context when non-null
        if (unifPlan.getContext() != null) {
            Formula propPrecondition = simplifyAndProp(unifPlan.getContext());
            ev.setPreCondition(propPrecondition);
        }

        PlanBody cur = unifPlan.getBody();
        while (cur != null && cur.getBodyTerm() != null) {
            if (!cur.getBodyTerm().isLiteral()) {
                cur = cur.getBodyNext();
                continue;
            }

            Literal bodyLit = (Literal) cur.getBodyTerm();

            if (cur.getBodyType() == PlanBody.BodyType.addBel && !bodyLit.negated()) {
                ev.addPostCondition(new PropFormula(new Pred(bodyLit)), new PropFormula(new Pred(Literal.LTrue)));
            } else if (cur.getBodyType() == PlanBody.BodyType.delBel || bodyLit.negated()) {
                ev.addPostCondition(new PropFormula(new Pred(bodyLit)), new PropFormula(new Pred(Literal.LFalse)));
            }

            cur = cur.getBodyNext();
        }
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
            if (rp == null) return null;

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
                    Iterator<RewriteUnifier> r = rewriteCons(context, opt.getUnifier());
                    boolean isApplicable = false;
                    Set<RewriteUnifier> distinctUnifs = new HashSet<>();

                    if (r != null) {
                        while (r.hasNext()) {
                            isApplicable = true;

                            if (ap == null)
                                ap = new LinkedList<>();

                            var unif = r.next();
                            var simplifiedUnif = new RewriteUnifier(unif.getFormula().simplify(), unif.getUnifier());

                            // Only add distinct unifiers
                            if (!distinctUnifs.contains(simplifiedUnif)) {
                                // Clone plan and insert re-write formulas
                                Plan cPlan = (Plan) opt.getPlan().clone();
                                cPlan.setContext(simplifiedUnif.getFormula().simplify());
                                ap.add(new Option(cPlan, simplifiedUnif.getUnifier()));
                            }
                            distinctUnifs.add(simplifiedUnif);

                            if (ts.getLogger().isLoggable(Level.FINE))
                                ts.getLogger().log(Level.FINE, "     " + opt.getPlan().getLabel() + " is applicable with unification " + opt.getUnifier());

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
    private Iterator<RewriteUnifier> rewriteCons(LogicalFormula context, Unifier unifier) {
        if (context == null) return null;

        // Need to clone agent and set up new belief base
        // var customAg = CallbackLogicalConsequence.CreateBBLogicalConsequence(ts.getAg(), groundingBase);
        return context.rewriteConsequences(ts.getAg(), unifier);
    }

    private List<Formula> getRangeConstraintRules(Agent ag, List<Literal> allRange) {
        // allRange should contain +/~ lits
        List<Formula> constraintRulesProps = new ArrayList<>();

        Map<Literal, Collection<LogicalFormula>> headToBodyMap = new HashMap<>();

        System.out.println("Processing constraints for " + allRange.size() + " range literals");
        int cur = 0;
        int conRuleCount = 0;
        for (Literal rangeLit : allRange) {
            cur++;

            var conRulesSet = getCandidateRules(ag, ag.getBB(), rangeLit, new Unifier());

            if (conRulesSet.size() > 100 || (allRange.size() > 500 && cur % 100 == 0)) {
                System.out.println(cur + "/" + allRange.size() + " -- processed " + conRuleCount + " constraint rules");
                // System.gc();
            }

            if ((allRange.size() > 500 && cur % 3000 == 0)) {
                System.out.println("Collecting");
                System.gc();
            }

            // For all constraint rules, we must obtain "rewrite" consequences, in order to simplify and propositionalize the rule
            for (Rule conRule : conRulesSet) {
                conRuleCount++;

                var rewriteIter = conRule.getBody().rewriteConsequences(ag, new Unifier());

                // How do we want to propositionalize a rule with a 'false' body?
                // Eg loc(1) :- false. is propped as 'true or loc(1)' which is trivially true.
                if (rewriteIter == null || !rewriteIter.hasNext())
                    continue;


                while (rewriteIter.hasNext()) {
                    var next = rewriteIter.next();

                    // Obtain unified head
                    var headUnif = conRule.headCApply(next.getUnifier());
                    if (!headUnif.isGround()) {
                        System.out.println("Head unif is not ground");
                        continue;
                    }
                    var set = headToBodyMap.getOrDefault(headUnif, new ArrayList<>());
                    set.add(next.getFormula());
                    headToBodyMap.put(headUnif, set);
                }
            }
        }

        for (var entry : headToBodyMap.entrySet()) {
            var head = entry.getKey();
            var fullForm = entry.getValue();

            // Create a disjoint formula containing several ground formulas.
            List<Formula> groundDisjForms = new ArrayList<>();

            for (var form : fullForm)
                groundDisjForms.add(form.simplify().toPropFormula());

            // Not sure if this is the right approach...
            constraintRulesProps.add(new ImpliesFormula(new OrFormula(groundDisjForms), head.toPropFormula()));
        }

        return constraintRulesProps;
    }

    private Formula prop(Term pared) {
        if (!pared.isGround()) throw new RuntimeException("Term must be ground to be propositionalized: " + pared);

        if (pared instanceof LogExpr) return propExpr((LogExpr) pared);

        if (pared instanceof Literal) return propLit((Literal) pared);

        throw new RuntimeException("Prop not supported?");
    }

    private Formula propExpr(LogExpr pared) {
        switch (pared.getOp()) {
            case not -> {
                return new NotFormula(prop(pared.getLHS()));
            }
            case and -> {
                return new AndFormula(prop(pared.getLHS()), prop(pared.getRHS()));
            }
            case or -> {
                return new OrFormula(prop(pared.getLHS()), prop(pared.getRHS()));
            }
            default -> {
                return prop(pared.getLHS());
            }
        }

    }

    private Formula simplifyAndProp(LogicalFormula unpared) {
        return unpared.simplify().toPropFormula();
    }

    /**
     * Find rules with heads that unify the given literal.
     *
     * @param a
     * @param beliefBase
     * @param literal
     * @param unif
     * @return
     */
    private Collection<Rule> getCandidateRules(Agent a, BeliefBase beliefBase, Literal literal, Unifier unif) {
        List<Rule> candidates = new ArrayList<>();
        var rangeIter = beliefBase.getCandidateBeliefs(literal, unif);

        // try literal iterator
        while (rangeIter != null && rangeIter.hasNext()) {
            var nextBel = rangeIter.next(); // b is the relevant entry in BB
            if (nextBel.isRule()) {
                Rule rule = (Rule) nextBel;

                // create a copy of this literal, ground it and
                // make its vars anonymous,
                // it is used to define what will be the unifier used
                // inside the rule.
                Literal cloneAnnon = (Literal) literal.capply(unif);
                cloneAnnon.makeVarsAnnon();

                Unifier ruleUn = unif.clone();
                if (ruleUn.unifiesNoUndo(cloneAnnon, rule)) { // the rule head unifies with the literal
                    candidates.add(new Rule(rule, ruleUn));

                }
            }
        }

        return candidates;
    }

    private void addRangesToBeliefBase(Agent ag) {
        Literal rangeVar = ASSyntax.createLiteral("range", ASSyntax.createVar());

        // Get all range predicates
        var rangeIter = rangeVar.rewriteConsequences(ag, new Unifier());

        // Map each rule to its constraint
        List<Formula> constraints = new ArrayList<>();

        while (rangeIter != null && rangeIter.hasNext()) {
            RewriteUnifier next = rangeIter.next();
            ag.getBB().add((Literal) rangeVar.getTerm(0).capply(next.getUnifier()));
        }

    }


    private Collection<Formula> getRangeConstraints(@NotNull Agent ag, List<Literal> rangeOut) {
        Literal rangeVar = ASSyntax.createLiteral("range", ASSyntax.createVar());

        // Get all range predicates
        var rangeIter = rangeVar.rewriteConsequences(ag, new Unifier());

        // Map each rule to its constraint
        List<Formula> constraints = new ArrayList<>();

        while (rangeIter != null && rangeIter.hasNext()) {
            RewriteUnifier next = rangeIter.next();
            Formula rangeProp = propRange(rangeVar, next.getUnifier(), rangeOut, this.ts.getAg().getBB());

            if (rangeProp != null)
                constraints.add(rangeProp);
        }
        return constraints;
    }

    /**
     * Propositionalizes a single range literal. We append positive and negative ground range values (first terms) to th e rangeValOut and belBase.
     *
     * @param rangeLit
     * @param u
     * @param rangeValOut A set of range values that is appended, if not null.
     * @param belBase     Add range values to the belief base as grounded beliefs, if not null.
     * @return The propositional formula, given a ground literal (potentially grounded by u),
     * or null if the literal can not be grounded, or if the first term is negated.
     */
    private Formula propRange(Literal rangeLit, Unifier u, List<Literal> rangeValOut, BeliefBase belBase) {
        // 1. Find all groundings of range
        // Set<Literal> ground = new HashSet<>();

        Literal groundRange = (Literal) rangeLit.capply(u);
        Term firstTerm = groundRange.getTerm(0);

        // If the literal is not ground, or the inner term is negated, we return
        if (!groundRange.isGround() || (firstTerm.isLiteral() && ((Literal) firstTerm).negated()))
            return null;

        // If firstTerm is a predicate/atom/etc. (but still a 'literal'), negated returns null.
        // So we must force full literal implementation
        Literal fullFirst = ((Literal) firstTerm).forceFullLiteralImpl();

        // Add literal pos and neg
        Literal neg = ((Literal) fullFirst.clone()).setNegated(Literal.LNeg);
        Literal pos = ((Literal) fullFirst.clone()).setNegated(Literal.LPos);


        if (rangeValOut != null) {
            rangeValOut.add(neg);
            rangeValOut.add(pos);
        }

        if (belBase != null) {
            belBase.add(neg);
            belBase.add(pos);
        }

        return createDisjunction(Set.of(pos.toPropFormula(), neg.toPropFormula()));
    }

    private Collection<Formula> interpretSingle(Rule r) {

        // single(X) => single(X = {a, b, c})
        // Add a, b, c to grounding set
        // Constraint: (a and not b and not c) or ...

        // 1. Find all groundings of single rule r
        Set<Literal> ground = getGroundConsequences(r);

        // Isolate ground head and remove 'Range' wrapper
        ground = ground.stream().map(l -> (Literal) l.getTerm(0)).collect(Collectors.toSet());

        if (ground.isEmpty()) return new HashSet<>();


        Set<Formula> singleConstraints = new HashSet<>();


        // Individual implies formulas created too many formulas!

        Set<Formula> groundProp = ground.stream().map(this::prop).collect(Collectors.toSet());
        singleConstraints.add(createDisjunction(groundProp));

//        List<Formula> allForm = new ArrayList<>();


        for (var g : ground) {
            var curForms = new ArrayList<Formula>();
            curForms.add(prop(g));

            for (var g2 : ground) {
                if (g == g2) continue;
                curForms.add(new NotFormula(prop(g2)));
            }
//            singleConstraints.add(new ImpliesFormula(prop(g), new NotFormula(prop(g2))));
//            singleConstraints.add(new EquivFormula(prop(g), new AndFormula(curForms)));
            singleConstraints.add(new ImpliesFormula(prop(g), new AndFormula(curForms)));
        }

//        String csProps = ground.stream().map(this::prop).collect(Collectors.joining(", "));
//        return "exact(1, [" + csProps + "])";


        return singleConstraints;
//        return Set.of(new OrFormula(singleConstraints));
    }

    private Set<Literal> getGroundConsequences(Literal l) {
        // Find all groundings of range
        Set<Literal> ground = new HashSet<>();

        LogicalFormula litForCons = l.clearAnnots();

        if (l.isRule()) litForCons = ((Rule) l).getBody();

        // Get all consequences (using grounding base)
        var cons = rewriteCons(litForCons, new Unifier());

        // No consequences
        if (cons == null) return ground;

        // Add head if already ground
        if (l.isGround()) ground.add(l);

        // Iterate all unifiers
        while (cons.hasNext()) {
            var next = cons.next();
            Literal res = (Literal) l.capply(next.getUnifier());

            boolean isGround = res.isGround();

            // rule.isGround always returns false, need to check head manually
            // NOTE: do not check if rule body is ground, think of case where 'x(1, 2) OR y(A, Z)' unifies head
            if (res.isRule()) {
                Rule resRule = (Rule) res;
                isGround = resRule.getHead().isGround();
            }

            if (isGround) ground.add(res);
        }

        return ground;
    }


    private Term simplify(Term t) {
        if (!t.isGround()) throw new RuntimeException("Term must be ground to be pared: " + t);

        if (t instanceof LogExpr) return simplifyExpr((LogExpr) t);

        // Terms that simplify to true, e.g. rel expr (5 = 5), int actions (.member(5, [1, 5]))
        if (t instanceof RelExpr || t instanceof InternalActionLiteral) return Literal.LTrue;

        if (t instanceof Literal) return simplifyLit((Literal) t);

//        if (t instanceof LogicalFormula)
//            return pareForm((LogicalFormula) t);

        System.out.println("Unknown term type: " + t.getClass().getSimpleName());
        return t;
    }

    /**
     * l1 & l2 => pare(l1) & pare(l2)
     * l1 & l2 => if (pare(l1) & LTrue) OR (LTrue & pare(l2)) or
     */
    private Term simplifyExpr(LogExpr t) {
        if (t.getOp() == LogExpr.LogicalOp.and) {
            var formLeft = (LogicalFormula) simplify(t.getLHS());
            var formRight = (LogicalFormula) simplify(t.getRHS());

            if (formLeft == Literal.LTrue) return formRight;

            if (formRight == Literal.LTrue) return formLeft;

            // Otherwise, return simplified expression.
            return new LogExpr(formLeft, LogExpr.LogicalOp.and, formRight);
        }


        if (t.getOp() == LogExpr.LogicalOp.or) {
            var formLeft = (LogicalFormula) simplify(t.getLHS());
            var formRight = (LogicalFormula) simplify(t.getRHS());

            if (formLeft == Literal.LFalse) return formRight;

            if (formRight == Literal.LFalse) return formLeft;

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

    private Literal simplifyLit(Literal lit) {
        if (lit.isGround()) return lit;
        // if lit is not ground, return false lit
        // This covers case of possible disjunction in log expr: x(1, 2) OR y(A, Z)
        // Returns 'x(1, 2) OR FALSE'
        return Literal.LFalse;


    }

    public Formula propLit(Literal l) {
        if (!l.isGround()) throw new RuntimeException("Literal must be ground for prop!");

        // Remove annotations
        l = l.copy().clearAnnots();

        if (l == Literal.LTrue || l == Literal.LFalse || !l.negated()) return new PropFormula(new Pred(l));

        // Convert literal to prop sentence form.
        return new NotFormula(new Pred(l)); // "(" + (l.negated() ? "not " : "") + l.setNegated(Literal.LPos) + ")";
    }

    private AndFormula createConjunction(Set<Formula> literals) {
        return new AndFormula(literals);
    }

    private OrFormula createDisjunction(Set<Formula> literals) {
        return new OrFormula(literals);
    }

    private List<Rule> getAllRules() {
        List<Rule> allRules = new ArrayList<>();
        for (var b : ts.getAg().getBB())
            if (b.isRule() && b.getNS() == Literal.DefaultNS) allRules.add((Rule) b);
        return allRules;
    }

    public boolean evaluate(EpistemicModality modality, Formula propFormula) {
        // Evaluate true/false without delegating to reasoner
        if (propFormula instanceof PropFormula) {
            if (((PropFormula) propFormula).getPropLit().equals(Literal.LTrue))
                return true;
            else if (((PropFormula) propFormula).getPropLit().equals(Literal.LFalse))
                return false;
        }

        if (!modelCreated) {
            System.out.println("WARNING: Evaluating formula while model is uninitialized. All consequences may return true.");
            return true;
        }

        // Map modality to formulas
        return reasoner.evaluateFormula(new ModalPropFormula(modality, propFormula));
    }

    @Override
    public void eventAdded(Event e) {
        // Only apply 'on' plans to belief events
        if (e.getTrigger().getType() != Trigger.TEType.belief)
            return;

        // Map the event '+/-e' to '+/-on(e)
        var event = new Event(
                new Trigger(e.getTrigger().getOperator(), e.getTrigger().getType(),
                        ASSyntax.createLiteral("on", e.getTrigger().getLiteral().clearAnnots())),
                e.getIntention());

        // Create and apply DEL event model
        try {
            applyEventModel(event);
        } catch (JasonException ex) {
            throw new RuntimeException(ex);
        }
        CircumstanceListener.super.eventAdded(e);
    }
}
