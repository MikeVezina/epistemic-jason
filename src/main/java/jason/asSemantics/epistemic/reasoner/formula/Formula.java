package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

public abstract class Formula {

    private final String type;
    protected Formula(String type)
    {
        this.type = type;
    }

    public abstract JsonElement toJson();
    public abstract String toPropString();

    public String toString()
    {
        return toPropString();
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Formula formula = (Formula) o;

        return Objects.equals(type, formula.type);
    }
}
