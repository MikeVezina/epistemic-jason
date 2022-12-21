package jason.stdlib;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSemantics.epistemic.LogOp;
import jason.asSyntax.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
public class big_and extends DefaultInternalAction {

    private static InternalAction singleton = null;

    public static InternalAction create() {
        if (singleton == null)
            singleton = new big_and();
        return singleton;
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    protected void checkArguments(Term[] args) throws JasonException {
        super.checkArguments(args); // check number of arguments
        // We only allow var terms that are unground.
        if (!args[0].isVar() || args[0].isGround()) {
            throw JasonException.createWrongArgument(this,"first argument must be an unground variable term");
        }

        if (!args[1].isList()) {
            throw JasonException.createWrongArgument(this,"second argument must be a list");
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object execute(TransitionSystem ts, final Unifier un, Term[] args) throws Exception {
        checkArguments(args);

        Term result = args[0];
        ListTerm list = (ListTerm) args[1];

        List<LogicalFormula> formulas = new ArrayList<>();

        for (var form : list) {
            Term cForm = form.capply(un);
            if (!cForm.isGround()) {
                System.out.println("big_and: term is not ground -- " + cForm + " from " + form);
                continue;
            }

            if (!(cForm instanceof LogicalFormula)) {
                System.out.println("big_and: term is not a formula -- " + cForm + " from " + form);
                continue;
            }

            formulas.add((LogicalFormula) cForm);
        }

        LogicalFormula resForm = createFormula(formulas, 0);
        return un.unifies(result, resForm);
    }

    protected LogicalFormula getDefaultFormula() {
        return Literal.LTrue;
    }

    protected LogExpr createFormula(LogicalFormula first, LogicalFormula second) {
        return new LogExpr(first, LogExpr.LogicalOp.and, second);
    }

    private LogicalFormula createFormula(List<LogicalFormula> terms, int cur) {
        if (cur >= terms.size())
            return getDefaultFormula();

        if (cur == terms.size() - 1)
            return terms.get(cur);

        return createFormula(terms.get(cur), createFormula(terms, cur + 1));
    }
}
