package jason.asSemantics.epistemic;

import jason.asSemantics.Unifier;
import jason.asSemantics.epistemic.reasoner.EpistemicReasoner;
import jason.asSemantics.epistemic.reasoner.formula.EpistemicFormulaLiteral;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.PredicateIndicator;
import jason.bb.BeliefBase;
import jason.bb.ChainBBAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChainedEpistemicBB extends ChainBBAdapter {
    private final EpistemicReasoner reasoner;
    private final BeliefBase groundBase;

    public ChainedEpistemicBB(@NotNull BeliefBase groundBase, EpistemicReasoner reasoner) {
        super(groundBase);
        this.reasoner = reasoner;
        this.groundBase = groundBase;
    }

//    @Override
//    public boolean add(Literal l) {
//        var addRes = super.add(l);
//
//        if(addRes)
//            addToValuation(l);
//
//        return addRes;
//    }
//
//    @Override
//    public boolean add(int index, Literal l) {
//        var addRes = super.add(index, l);
//
//        if(addRes)
//            addToValuation(l);
//
//        return addRes;
//    }

//    private void addToValuation(Literal l) {
//
//    }
//
//    @Override
//    public Literal contains(Literal l) {
//        return super.contains(l);
//    }

    @Override
    public Iterator<Literal> getCandidateBeliefs(PredicateIndicator pi) {
        // The predicate indicator doesn't work with epistemic beliefs,
        // i.e: it will always be knows/1, we need the root literal to be able to evaluate anything
        // We just forward this to the actual belief base.
        return super.getCandidateBeliefs(pi);
    }

    @Override
    public Iterator<Literal> getCandidateBeliefs(Literal l, Unifier u) {
        // Copy & Apply the unifier to the literal
        Literal unifiedLiteral = (Literal) l.capply(u);

        var epistemicLiteral = EpistemicFormulaLiteral.fromLiteral(unifiedLiteral);

        // If the root literal is not ground, then obtain all possible managed unifications
        var groundFormulas = getCandidateFormulas(epistemicLiteral);

        var result = reasoner.evaluateFormulas(groundFormulas);
        var arr = new ArrayList<Literal>();

        // If the result is true (formula evaluated to true), then return the literal as a candidate belief
        for(var formulaResultEntry : result.entrySet()) {
            // Add formula literal to results list if the formula was evaluated to true
            if(formulaResultEntry.getValue())
                arr.add(formulaResultEntry.getKey().getCleanedOriginal());
        }

        // Return null if no candidates
        // This maintains the original BB functionality
        if(arr.isEmpty())
            return null;

        return arr.iterator();
    }

    protected List<EpistemicFormulaLiteral> getCandidateFormulas(EpistemicFormulaLiteral formula)
    {
        // Strip formula of all modalities
        Literal formLit = formula.getRootLiteral();

        // Find all groundings using ground set and return list of results
        List<EpistemicFormulaLiteral> results = new ArrayList<>();

        var belIter = this.groundBase.getCandidateBeliefs(formLit, new Unifier());

        while(belIter != null && belIter.hasNext())
        {
            Literal next = belIter.next().copy();
            next.clearAnnots();
            results.add(EpistemicFormulaLiteral.CreateFormula(formula.getEpistemicModality(), next));
        }

        return results;
    }

    @Override
    public boolean abolish(PredicateIndicator pi) {
        return super.abolish(pi);
    }

    @Override
    public boolean abolish(Atom namespace, PredicateIndicator pi) {
        return super.abolish(namespace, pi);
    }

    @Override
    public boolean remove(Literal l) {
        return super.remove(l);
    }


}
