package jason.asSyntax;

import jason.asSemantics.Agent;
import jason.asSemantics.RewriteUnifier;
import jason.asSemantics.Unifier;

import java.util.Iterator;

/**
 * Represents a logical formula (p, p & q, not p, 3 > X, ...) which can be
 * evaluated into a truth value.
 *
 * @opt nodefillcolor lightgoldenrodyellow
 *
 * @author Jomi
 */
public interface LogicalFormula extends Term, Cloneable {
    /**
     * Checks whether the formula is a
     * logical consequence of the belief base.
     *
     * Returns an iterator for all unifiers that are consequence.
     */
    public Iterator<Unifier> logicalConsequence(Agent ag, Unifier un);

    /**
     * Rewrites the formula strictly in terms of ground belief literals, true, and/or false.
     * To do this, we must simultaneously evaluate and rewrite.
     * The end result is a ground formula, written in terms of the given literal set.
     */
    Iterator<RewriteUnifier> rewriteConsequences(Agent ag, Unifier un);

}
