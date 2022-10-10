package jason.asSemantics.epistemic.reasoner.formula;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;

public class PossibleEpistemicFormula extends EpistemicFormula {
    /**
     * @param originalLiteral The original literal corresponding to this epistemic formula
     */
    public PossibleEpistemicFormula(Literal originalLiteral) {
        super(EpistemicModality.POSSIBLE, originalLiteral);

        // Assert arity == 1 and that nested term is literal.
        if (originalLiteral.getArity() != 1 || !originalLiteral.getTerm(0).isLiteral())
            throw new RuntimeException("Invalid Possible formula: " + originalLiteral);

    }

    public EpistemicFormula deriveNewPossibleFormula(boolean modalNegated, boolean propNegated) {
        return EpistemicFormula.fromLiteral(
                ASSyntax.createLiteral(
                        EpistemicModality.POSSIBLE.getFunctor(),
                        getRootLiteral().setNegated(propNegated ? Literal.LNeg : Literal.LPos))
//                        getRootLiteral().getCleanedLiteral().setNegated(propNegated ? Literal.LNeg : Literal.LPos))
                        .setNegated(modalNegated ? Literal.LNeg : Literal.LPos)
        );
    }

    @Override
    protected boolean getModalityNegated() {
        return getOriginalLiteral().negated();
    }

    @Override
    protected Literal processRootLiteral(Literal originalLiteral) {
        if(originalLiteral.getArity() != 1)
            throw new RuntimeException("Invalid possibility formula: " + originalLiteral);

        return ((Literal) originalLiteral.getTerm(0));
    }
}
