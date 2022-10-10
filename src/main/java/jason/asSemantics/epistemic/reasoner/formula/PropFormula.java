package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jason.asSyntax.Literal;
import jason.asSyntax.Pred;

import java.util.Objects;

public class PropFormula extends Formula {
    private static final String FORM_TYPE = "prop";
    private final Pred propLit;

    public PropFormula(Pred lit) {
        super(FORM_TYPE);
        this.propLit = lit;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.add("type", new JsonPrimitive(FORM_TYPE));
        obj.add("prop", new JsonPrimitive(propLit.toString()));
        return obj;
    }

    @Override
    public String toPropString() {
        return propLit.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PropFormula that = (PropFormula) o;

        return Objects.equals(propLit, that.propLit);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (propLit != null ? propLit.toString().hashCode() : 0);
        return result;
    }
}
