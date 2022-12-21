package jason.stdlib;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Internal action: <b><code>.member(<i>T</i>,<i>L</i>)</code></b>.
 *
 * <p>Description: checks if some term <i>T</i> is in a list <i>L</i>. If
 * <i>T</i> is a free variable, this internal action backtracks all
 * possible values for <i>T</i>.
 *
 * <p>Parameters:<ul>
 *
 * <li>+/- member (term): the term to be checked.</li>
 * <li>+ list (list): the list where the term should be in.</li>
 *
 * </ul>
 *
 * <p>Examples:<ul>
 *
 * <li> <code>.member(c,[a,b,c])</code>: true.</li>
 * <li> <code>.member(3,[a,b,c])</code>: false.</li>
 * <li> <code>.member(X,[a,b,c])</code>: unifies X with any member of the list.</li>
 *
 * </ul>
 *
 * @see concat
 * @see length
 * @see sort
 * @see nth
 * @see max
 * @see min
 * @see reverse
 * @see difference
 * @see intersection
 * @see union
 */
@Manual(
        literal = ".big_and(Res,List>)",
        hint = "given a list of terms, this function creates a single large conjunction of terms. Res must be a variable term.",
        argsHint = {
                "the term to be checked",
                "the list where the term should be in"
        },
        argsType = {
                "term",
                "list"
        },
        examples = {
                ".member(c,[a,b,c]): true",
                ".member(3,[a,b,c]): false",
                ".member(X,[a,b,c]): unifies X with any member of the list"
        },
        seeAlso = {
                "jason.stdlib.concat",
                "jason.stdlib.delete",
                "jason.stdlib.length",
                "jason.stdlib.sort",
                "jason.stdlib.nth",
                "jason.stdlib.max",
                "jason.stdlib.min",
                "jason.stdlib.reverse",
                "jason.stdlib.difference",
                "jason.stdlib.intersection",
                "jason.stdlib.union"
        }
)
@SuppressWarnings("serial")
public class big_or extends big_and {

    private static InternalAction singleton = null;

    public static InternalAction create() {
        if (singleton == null)
            singleton = new big_or();
        return singleton;
    }

    protected LogicalFormula getDefaultFormula() {
        return Literal.LTrue;
    }

    protected LogExpr createFormula(LogicalFormula first, LogicalFormula second) {
        return new LogExpr(first, LogExpr.LogicalOp.or, second);
    }
}
