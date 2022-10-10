package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.*;

public class OrFormula extends Formula {

    private static final String FORM_TYPE = "or";
    private static final String JSON_FORMULAS = "formulas";
    private final List<Formula> formulaList;

    public OrFormula(Formula... formulas) {
        this(Arrays.asList(formulas));
    }

    public OrFormula(Collection<Formula> formulas) {
        super(FORM_TYPE);
        this.formulaList = new ArrayList<>(formulas);
    }

    @Override
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.add("type", new JsonPrimitive(FORM_TYPE));

        JsonArray formulaArr = new JsonArray();
        for(var form : formulaList)
            formulaArr.add(form.toJson());

        obj.add(JSON_FORMULAS, formulaArr);
        return obj;
    }

    @Override
    public String toPropString() {
        List<String> formStrings = new ArrayList<>();
        for(var f : formulaList)
            formStrings.add(f.toPropString());

        return String.join(" " + FORM_TYPE + " ", formStrings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        OrFormula orFormula = (OrFormula) o;

        return Objects.equals(formulaList, orFormula.formulaList);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (formulaList != null ? formulaList.hashCode() : 0);
        return result;
    }
}
