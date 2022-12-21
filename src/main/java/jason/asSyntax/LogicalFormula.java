package jason.asSyntax;

import jason.asSemantics.Agent;
import jason.asSemantics.RewriteUnifier;
import jason.asSemantics.Unifier;
import jason.asSemantics.epistemic.reasoner.formula.Formula;

import java.util.Iterator;

/**
 * Represents a logical formula (p, p & q, not p, 3 > X, ...) which can be
 * evaluated into a truth value.
 *
 * @author Jomi
 * @opt nodefillcolor lightgoldenrodyellow
 */
public interface LogicalFormula extends Term, Cloneable {
    /**
     * Checks whether the formula is a
     * logical consequence of the belief base.
     * <p>
     * Returns an iterator for all unifiers that are consequence.
     */
    public Iterator<Unifier> logicalConsequence(Agent ag, Unifier un);

    /**
     * Rewrites the formula strictly in terms of ground belief literals, true, and/or false.
     * To do this, we must simultaneously evaluate and rewrite.
     * The end result is a ground formula, written in terms of the given literal set.
     */
    Iterator<RewriteUnifier> rewriteConsequences(Agent ag, Unifier un);

    /**
     * Converts the logical formula into a propositional formula representation.
     * This function assumes that the formula has already been rewritten by {@link this#rewriteConsequences(Agent, Unifier)}}
     */
    Formula toPropFormula();

    /**
     * Simplifies the expression, if possible, to provide a more compact representation. Simplifies to a literal (or some sub-class).
     * This really only applies to LogExpr, but is needed here.
     */
    Literal simplify();

}
