package jason.asSemantics;

import jason.asSyntax.LogicalFormula;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RewriteUnifier extends HashMap.SimpleEntry<LogicalFormula, Unifier>{

    private final LogicalFormula formula;
    private final Unifier unifier;

    public RewriteUnifier(LogicalFormula formula, Unifier unifier) {
        super(formula, unifier);
        this.formula = formula;
        this.unifier = unifier;
    }

    public LogicalFormula getFormula() {
        return formula;
    }

    public Unifier getUnifier() {
        return unifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RewriteUnifier that = (RewriteUnifier) o;

        if (!Objects.equals(formula, that.formula)) return false;
        return Objects.equals(unifier, that.unifier);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (formula != null ? formula.hashCode() : 0);
        result = 31 * result + (unifier != null ? unifier.hashCode() : 0);
        return result;
    }
}
