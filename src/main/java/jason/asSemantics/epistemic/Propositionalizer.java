package jason.asSemantics.epistemic;

import jason.asSemantics.epistemic.reasoner.formula.Formula;
import jason.asSyntax.Literal;

public interface Propositionalizer {

    Formula propLit(Literal l);
}
