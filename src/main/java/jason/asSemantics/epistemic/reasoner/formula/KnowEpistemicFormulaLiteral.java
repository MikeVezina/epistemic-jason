package jason.asSemantics.epistemic.reasoner.formula;

import jason.asSyntax.Literal;

public class KnowEpistemicFormulaLiteral extends EpistemicFormulaLiteral {
    /**
     * Can only be constructed through the static parseLiteral method.
     *
     * @param originalLiteral The original literal corresponding to this epistemic formula
     */
    public KnowEpistemicFormulaLiteral(Literal originalLiteral) {
        super(EpistemicModality.KNOW, originalLiteral);
    }

    /**
     * @return Knowledge formulas parsed from a literal will never have a negated modality.
     */
    @Override
    protected boolean getModalityNegated() {
        return false;
    }


    @Override
    protected Literal processRootLiteral(Literal originalLiteral) {
        return originalLiteral.copy();
    }
}
