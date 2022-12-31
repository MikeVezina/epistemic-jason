package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

public class ModalPropFormula extends Formula {
    private EpistemicModality modality;
    private static final String FORM_TYPE = "modal";
    private final Formula inner;

    public ModalPropFormula(EpistemicModality modality, Formula inner) {
        super(modality.getFunctor());
        this.modality = modality;
        this.inner = inner;
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.add("type", new JsonPrimitive(FORM_TYPE));
        obj.add("modal", new JsonPrimitive(this.modality.getFunctor()));
        obj.add("formula", inner.toJson());
        return obj;
    }

    @Override
    public String toPropString() {
        throw new RuntimeException("Could not propositionalize with modality.");
        // return this.modality.getFunctor() + " " inner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ModalPropFormula that = (ModalPropFormula) o;

        if (modality != that.modality) return false;
        return Objects.equals(inner, that.inner);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (modality != null ? modality.hashCode() : 0);
        result = 31 * result + (inner != null ? inner.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "(" + modality.getFunctor() +
                " " + inner.toString() +
                ')';
    }

    @Override
    protected int calcDepth() {
        return 1 + inner.getDepth();
    }
}
