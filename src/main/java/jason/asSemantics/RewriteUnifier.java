package jason.asSemantics;

import jason.asSyntax.LogicalFormula;

import java.util.HashMap;
import java.util.Map;

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
}
