package jason.asSyntax;

import jason.asSemantics.Agent;
import jason.asSemantics.RewriteUnifier;
import jason.asSemantics.Unifier;
import jason.asSemantics.epistemic.reasoner.formula.AndFormula;
import jason.asSemantics.epistemic.reasoner.formula.Formula;
import jason.asSemantics.epistemic.reasoner.formula.NotFormula;
import jason.asSemantics.epistemic.reasoner.formula.OrFormula;
import jason.asSyntax.parser.as2j;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Represents a logical formula with some logical operator ("&amp;",  "|", "not").
 *
 * @navassoc - op - LogicalOp
 */
public class LogExpr extends BinaryStructure implements LogicalFormula {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(LogExpr.class.getName());

    public static final List<Unifier> EMPTY_UNIF_LIST = Collections.emptyList();

    public enum LogicalOp {
        none {
            public String toString() {
                return "";
            }
        },
        not {
            public String toString() {
                return "not ";
            }
        },
        and {
            public String toString() {
                return " & ";
            }
        },
        or {
            public String toString() {
                return " | ";
            }
        };
    }

    private LogicalOp op = LogicalOp.none;

    public LogExpr(LogicalFormula f1, LogicalOp oper, LogicalFormula f2) {
        super(f1, oper.toString(), f2);
        op = oper;
    }

    public LogExpr(LogicalOp oper, LogicalFormula f) {
        super(oper.toString(), (Term) f);
        op = oper;
    }

    /**
     * gets the LHS of this Expression
     */
    public LogicalFormula getLHS() {
        return (LogicalFormula) getTerm(0);
    }

    /**
     * gets the RHS of this Expression
     */
    public LogicalFormula getRHS() {
        return (LogicalFormula) getTerm(1);
    }


    @Override
    public Formula toPropFormula() {
        if (!this.isGround())
            return LFalse.toPropFormula();

        switch (getOp()) {
            case not -> {
                return new NotFormula(getLHS().toPropFormula());
            }
            case and -> {
                return new AndFormula(getLHS().toPropFormula(), getRHS().toPropFormula());
            }
            case or -> {
                return new OrFormula(getLHS().toPropFormula(), getRHS().toPropFormula());
            }
            default -> {
                return getLHS().toPropFormula();
            }
        }
    }

    /**
     * Simplifies the expression, if possible, to provide a more compact representation.
     */
    public Literal simplify() {
        if (getOp() == LogExpr.LogicalOp.and) {
            var formLeft = getLHS().simplify();
            var formRight = getRHS().simplify();

            if (formLeft == Literal.LTrue) return formRight;

            if (formRight == Literal.LTrue) return formLeft;

            // Otherwise, return simplified expression.
            return new LogExpr(formLeft, LogExpr.LogicalOp.and, formRight);
        }


        if (getOp() == LogExpr.LogicalOp.or) {
            var formLeft = getLHS().simplify();
            var formRight = getRHS().simplify();

            if (formLeft == Literal.LFalse || !formLeft.isGround()) return formRight;

            if (formRight == Literal.LFalse || !formRight.isGround()) return formLeft;

            // Otherwise, return pared expression.
            return new LogExpr(formLeft, LogExpr.LogicalOp.or, formRight);
        }


        if (getOp() == LogExpr.LogicalOp.not) {
            return new LogExpr(LogicalOp.not, getLHS().simplify());
        } else {
            // E.g.: not(~test(X, Y)) = for all possible unifiers (X, Y), ~(~test(X, Y)) holds true
            // Special case, where we need to potentially ground all literals
            // More complicated: not(test(X, Y) & not(other(X, Z) OR other(Z, X)))
            //      With gs = {test(1, 2), test(3, 4), test(5, 5), other(1, 4), other(4, 1), other(5, 5)}
            //      Meaning => not([test(1, 2), test(3, 4), ] ???
            // TODO: Handle this, but for now, we assume only strong negation in constraint rules... Handled I think?
            throw new RuntimeException("Unsupported simplify for: " + getOp().toString());
        }
    }

    @Override
    public Iterator<RewriteUnifier> rewriteConsequences(Agent ag, Unifier un) {
        try {
            switch (op) {
                case none:
                    break;

                case not:
                    // unifier should be separate when rewriting 'not' formulae.
                    Unifier unClone = un.clone();
                    var cons = getLHS().rewriteConsequences(ag, unClone);
                    if (!cons.hasNext()) {
                        // if there are no consequences of inside formula, return a true formula
                        // E.g., not X (where X is not true) ==> TRUE
                        return createRewriteUnifIterator(new RewriteUnifier(LTrue, un));
                    }
                    // If there are consequences, we rewrite the formula(s) to include the consequences.
                    // If x :- a or b, then "not x" should be replaced with "not (a or b)" rather than "not a" or "not b"

                    // Create OR containing all consequences
                    LogicalFormula curForm = cons.next().getFormula();

                    while (cons.hasNext()) {
                        curForm = new LogExpr(cons.next().getFormula(), LogicalOp.or, curForm);
                    }

                    // I don't think we should return the 'not' formula unifiers. Jason does not do this, and it will impact the evaluation of later formulas.
                    return createRewriteUnifIterator(new RewriteUnifier(new LogExpr(LogicalOp.not, curForm), un));

                case and:
                    return new Iterator<RewriteUnifier>() {
                        Iterator<RewriteUnifier> ileft = getLHS().rewriteConsequences(ag, un);
                        RewriteUnifier left = null;
                        ;
                        Iterator<RewriteUnifier> iright = null;
                        RewriteUnifier current = null;
                        boolean needsUpdate = true;

                        public boolean hasNext() {
                            if (needsUpdate)
                                get();
                            return current != null;
                        }

                        public RewriteUnifier next() {
                            if (needsUpdate)
                                get();
                            if (current != null)
                                needsUpdate = true;
                            return current;
                        }

                        private void get() {
                            needsUpdate = false;
                            current = null;
                            while ((iright == null || !iright.hasNext()) && ileft.hasNext()) {
                                left = ileft.next();
                                iright = getRHS().rewriteConsequences(ag, left.getUnifier());
                            }
                            if (iright != null && iright.hasNext()) {
                                var right = iright.next();
                                current = new RewriteUnifier(new LogExpr(left.getFormula(), LogicalOp.and, right.getFormula()), right.getUnifier());
                            }
                        }

                        public void remove() {
                        }
                    };

                case or:
                    Unifier originalUn = un.clone();
                    return new Iterator<RewriteUnifier>() {
                        Iterator<RewriteUnifier> ileft = getLHS().rewriteConsequences(ag, un);
                        Iterator<RewriteUnifier> iright = null;
                        RewriteUnifier current = null;
                        boolean needsUpdate = true;

                        public boolean hasNext() {
                            if (needsUpdate)
                                get();
                            return current != null;
                        }

                        public RewriteUnifier next() {
                            if (needsUpdate)
                                get();
                            if (current != null)
                                needsUpdate = true;
                            return current;
                        }

                        private void get() {
                            needsUpdate = false;
                            current = null;
                            if (ileft != null && ileft.hasNext())
                                current = ileft.next();
                            else {
                                if (iright == null)
                                    iright = getRHS().rewriteConsequences(ag, originalUn);
                                if (iright != null && iright.hasNext())
                                    current = iright.next();
                            }
                        }

                        public void remove() {
                        }
                    };
            }
        } catch (Exception e) {
            String slhs = "is null ";
            Iterator<RewriteUnifier> i = getLHS().rewriteConsequences(ag, un);
            if (i != null) {
                slhs = "";
                while (i.hasNext()) {
                    slhs += i.next().toString() + ", ";
                }
            } else {
                slhs = "iterator is null";
            }
            String srhs = "is null ";
            if (!isUnary()) {
                i = getRHS().rewriteConsequences(ag, un);
                if (i != null) {
                    srhs = "";
                    while (i.hasNext()) {
                        srhs += i.next().toString() + ", ";
                    }
                } else {
                    srhs = "iterator is null";
                }
            }

            logger.log(Level.SEVERE, "Error evaluating rewrite expression " + this + ". \nlhs elements=" + slhs + ". \nrhs elements=" + srhs, e);
        }
        return EMPTY_REWRITE_UNIF_LIST.iterator();  // empty iterator for unifier
    }

    public Iterator<Unifier> logicalConsequence(final Agent ag, final Unifier un) {
        try {
            switch (op) {

                case none:
                    break;

                case not:
                    if (!getLHS().logicalConsequence(ag, un).hasNext()) {
                        return createUnifIterator(un);
                    }
                    break;

                case and:
                    return new Iterator<Unifier>() {
                        Iterator<Unifier> ileft = getLHS().logicalConsequence(ag, un);
                        ;
                        Iterator<Unifier> iright = null;
                        Unifier current = null;
                        boolean needsUpdate = true;

                        public boolean hasNext() {
                            if (needsUpdate)
                                get();
                            return current != null;
                        }

                        public Unifier next() {
                            if (needsUpdate)
                                get();
                            if (current != null)
                                needsUpdate = true;
                            return current;
                        }

                        private void get() {
                            needsUpdate = false;
                            current = null;
                            while ((iright == null || !iright.hasNext()) && ileft.hasNext())
                                iright = getRHS().logicalConsequence(ag, ileft.next());
                            if (iright != null && iright.hasNext())
                                current = iright.next();
                        }

                        public void remove() {
                        }
                    };

                case or:
                    Unifier originalUn = un.clone();
                    return new Iterator<Unifier>() {
                        Iterator<Unifier> ileft = getLHS().logicalConsequence(ag, un);
                        Iterator<Unifier> iright = null;
                        Unifier current = null;
                        boolean needsUpdate = true;

                        public boolean hasNext() {
                            if (needsUpdate)
                                get();
                            return current != null;
                        }

                        public Unifier next() {
                            if (needsUpdate)
                                get();
                            if (current != null)
                                needsUpdate = true;
                            return current;
                        }

                        private void get() {
                            needsUpdate = false;
                            current = null;
                            if (ileft != null && ileft.hasNext())
                                current = ileft.next();
                            else {
                                if (iright == null)
                                    iright = getRHS().logicalConsequence(ag, originalUn);
                                if (iright != null && iright.hasNext())
                                    current = iright.next();
                            }
                        }

                        public void remove() {
                        }
                    };
            }
        } catch (Exception e) {
            String slhs = "is null ";
            Iterator<Unifier> i = getLHS().logicalConsequence(ag, un);
            if (i != null) {
                slhs = "";
                while (i.hasNext()) {
                    slhs += i.next().toString() + ", ";
                }
            } else {
                slhs = "iterator is null";
            }
            String srhs = "is null ";
            if (!isUnary()) {
                i = getRHS().logicalConsequence(ag, un);
                if (i != null) {
                    srhs = "";
                    while (i.hasNext()) {
                        srhs += i.next().toString() + ", ";
                    }
                } else {
                    srhs = "iterator is null";
                }
            }

            logger.log(Level.SEVERE, "Error evaluating expression " + this + ". \nlhs elements=" + slhs + ". \nrhs elements=" + srhs, e);
        }
        return EMPTY_UNIF_LIST.iterator();  // empty iterator for unifier
    }

    /**
     * creates an iterator for a list of unifiers
     */
    static public Iterator<Unifier> createUnifIterator(final Unifier... unifs) {
        return new Iterator<Unifier>() {
            int i = 0;

            public boolean hasNext() {
                return i < unifs.length;
            }

            public Unifier next() {
                return unifs[i++];
            }

            public void remove() {
            }
        };
    }

    static public Iterator<RewriteUnifier> createRewriteUnifIterator(final RewriteUnifier... unifs) {
        return new Iterator<RewriteUnifier>() {
            int i = 0;

            public boolean hasNext() {
                return i < unifs.length;
            }

            public RewriteUnifier next() {
                return unifs[i++];
            }

            public void remove() {
            }
        };
    }

    /**
     * returns some LogicalFormula that can be evaluated
     */
    public static LogicalFormula parseExpr(String sExpr) {
        as2j parser = new as2j(new StringReader(sExpr));
        try {
            return (LogicalFormula) parser.log_expr();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing expression " + sExpr, e);
        }
        return null;
    }

    @Override
    public Term capply(Unifier u) {
        // do not call constructor with term parameter!
        if (isUnary())
            return new LogExpr(op, (LogicalFormula) getTerm(0).capply(u));
        else
            return new LogExpr((LogicalFormula) getTerm(0).capply(u), op, (LogicalFormula) getTerm(1).capply(u));
    }

    /**
     * make a hard copy of the terms
     */
    public LogicalFormula clone() {
        // do not call constructor with term parameter!
        if (isUnary())
            return new LogExpr(op, (LogicalFormula) getTerm(0).clone());
        else
            return new LogExpr((LogicalFormula) getTerm(0).clone(), op, (LogicalFormula) getTerm(1).clone());
    }

    @Override
    public Literal cloneNS(Atom newnamespace) {
        if (isUnary())
            return new LogExpr(op, (LogicalFormula) getTerm(0).cloneNS(newnamespace));
        else
            return new LogExpr((LogicalFormula) getTerm(0).cloneNS(newnamespace), op, (LogicalFormula) getTerm(1).cloneNS(newnamespace));
    }


    /**
     * gets the Operation of this Expression
     */
    public LogicalOp getOp() {
        return op;
    }

    /**
     * get as XML
     */
    public Element getAsDOM(Document document) {
        Element u = super.getAsDOM(document);
        u.setAttribute("type", "logical");
        return u;
    }

}
