package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

/*
    Represents antecedent => consequent
 */
public class EquivFormula extends Formula {

    private static final String FORM_TYPE = "equiv";
    private static final String SYMBOL = "<=>";
    private static final String JSON_ANTECEDANT = "antecedent";
    private static final String JSON_CONSEQUENT = "consequent";
    private final Formula antecedent;
    private final Formula consequent;

    public EquivFormula(Formula antecedent, Formula consequent) {
        super(FORM_TYPE);
        this.antecedent = antecedent;
        this.consequent = consequent;
    }

    @Override
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.add("type", new JsonPrimitive(FORM_TYPE));
        obj.add(JSON_ANTECEDANT, antecedent.toJson());
        obj.add(JSON_CONSEQUENT, consequent.toJson());
        return obj;
    }

    @Override
    public String toPropString() {
        return antecedent.toPropString() + " " +  SYMBOL + " " + consequent.toPropString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EquivFormula that = (EquivFormula) o;

        if (!Objects.equals(antecedent, that.antecedent)) return false;
        return Objects.equals(consequent, that.consequent);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (antecedent != null ? antecedent.hashCode() : 0);
        result = 31 * result + (consequent != null ? consequent.hashCode() : 0);
        return result;
    }
}
