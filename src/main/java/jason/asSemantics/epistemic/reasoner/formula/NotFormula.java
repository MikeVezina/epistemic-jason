package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jason.asSyntax.Pred;

import java.util.Objects;

public class NotFormula extends Formula {

    private static final String FORM_TYPE = "not";
    private static final String JSON_FORMULA = "formula";
    private final Formula innerForm;

    public NotFormula(Pred pred) {
        this(new PropFormula(pred));
    }

    public NotFormula(Formula inner)
    {
        super(FORM_TYPE);
        this.innerForm = inner;
    }

    @Override
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.add("type", new JsonPrimitive(FORM_TYPE));
        obj.add(JSON_FORMULA, innerForm.toJson());
        return obj;
    }

    @Override
    public String toPropString() {
        return "(" + FORM_TYPE + " " + innerForm.toPropString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NotFormula that = (NotFormula) o;

        return Objects.equals(innerForm, that.innerForm);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (innerForm != null ? innerForm.hashCode() : 0);
        return result;
    }
}
