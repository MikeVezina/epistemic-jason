package jason.asSemantics.epistemic.reasoner.formula;

//import epistemic.wrappers.Literal;

import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;

import java.util.Objects;
import java.util.UUID;

/**
 * This class is used to represent a knowledge formula.
 * For example, a literal 'know(know(hello))' will get unwrapped so that we have access
 * to the rootLiteral (i.e. hello) as well as the chain of embedded literals.
 */
public abstract class EpistemicFormulaLiteral {

    private final Literal rootLiteral;
    private final Literal originalLiteral;
    private final EpistemicModality modality;
    private final boolean modalityNegated;
    private final boolean propositionNegated;
    private final UUID uuid;

    /**
     * Can only be constructed through the static parseLiteral method.
     *
     * @param originalLiteral The original literal corresponding to this epistemic formula
     */
    protected EpistemicFormulaLiteral(EpistemicModality modality, Literal originalLiteral) {
        this.modality = modality;
        this.originalLiteral = new LiteralImpl(originalLiteral);
        this.rootLiteral = processRootLiteral(this.originalLiteral);
        this.modalityNegated = getModalityNegated();
        this.propositionNegated = ((Literal) rootLiteral.clone()).negated();
        uuid = UUID.randomUUID();
    }

    public static EpistemicFormulaLiteral CreateFormula(EpistemicModality epistemicModality, Literal next) {
        switch (epistemicModality) {
            case KNOW -> {
                return new KnowEpistemicFormulaLiteral(next);
            }
            /*case POSSIBLE */ default -> {
                return new PossibleEpistemicFormulaLiteral(ASSyntax.createLiteral(EpistemicModality.POSSIBLE.getFunctor(), next));
            }
        }
    }

    protected abstract boolean getModalityNegated();

    protected abstract Literal processRootLiteral(Literal originalLiteral);

    public boolean isPropositionNegated() {
        return propositionNegated;
    }

    public boolean isModalityNegated() {
        return modalityNegated;
    }

    /**
     * Recursively parses a literal into an epistemic formula. If the literal is not
     * an epistemic literal, an EpistemicFormula object will not be created and
     * null will be returned. Calls {@link EpistemicFormulaLiteral#isEpistemicLiteral(Literal)}
     * to check if literal is epistemic.
     *
     * @param originalLiteral The epistemic literal to be converted into an EpistemicFormula object.
     * @return An EpistemicFormula object parsed from the literal. Null if the literal is not epistemic.
     * @see EpistemicFormulaLiteral#isEpistemicLiteral(Literal)
     */
    public static EpistemicFormulaLiteral fromLiteral(Literal originalLiteral) {
        Literal copyOriginal = originalLiteral.copy();

        if (EpistemicModality.POSSIBLE.isFunctor(copyOriginal.getFunctor()))
            return new PossibleEpistemicFormulaLiteral(copyOriginal);
        else
            return new KnowEpistemicFormulaLiteral(copyOriginal);
    }

    public EpistemicModality getEpistemicModality() {
        return modality;
    }

    public Literal getRootLiteral() {
        return rootLiteral;
    }

    @Deprecated
    private static boolean isEpistemicLiteral(Literal curLit) {
        return curLit != null && EpistemicModality.findFunctor(curLit.getFunctor()) != null && curLit.getArity() == 1;
    }

    public String getUniqueId() {
        return this.uuid.toString();
    }

    public Literal getCleanedOriginal() {
//        return originalLiteral.getCleanedLiteral();
        return originalLiteral.copy();
    }

    public Literal getOriginalLiteral() {
        return originalLiteral;
    }

    /**
     * Applies the unifier to the current formula
     *
     * @param unifier The unifier with the corresponding variable values
     * @return A new epistemic formula object with the unified values.
     */
    public EpistemicFormulaLiteral capply(Unifier unifier) {
        Literal applied = (Literal) getCleanedOriginal().capply(unifier);
        applied.resetHashCodeCache();
        return EpistemicFormulaLiteral.fromLiteral(applied);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EpistemicFormulaLiteral)) return false;
        EpistemicFormulaLiteral that = (EpistemicFormulaLiteral) o;
        return modalityNegated == that.modalityNegated && propositionNegated == that.propositionNegated && rootLiteral.equals(that.rootLiteral) && modality == that.modality;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootLiteral.toString(), modality, modalityNegated, propositionNegated);
    }

    @Override
    public String toString() {
        return getCleanedOriginal().toString();
    }
}
