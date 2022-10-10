package jason.asSemantics.epistemic;

public enum LogOp {
    IMPLIES("=>"),
    OR("or"),
    AND("and"),
    EQUIV("<=>");

    private final String symbol;

    LogOp(String symbol) {
        this.symbol = symbol;
    }

    public String toString() {
        return symbol;
    }
}
