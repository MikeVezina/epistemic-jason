package jason.asSemantics.epistemic.reasoner.formula;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.*;

public class AndFormula extends Formula {

    private static final String FORM_TYPE = "and";
    private static final String JSON_FORMULAS = "formulas";
    private final List<Formula> formulaList;

    public AndFormula(Formula... formulas) {
        this(Arrays.asList(formulas));
    }

    public AndFormula(Collection<Formula> formulas) {
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

        return "(" + String.join(" " + FORM_TYPE + " ", formStrings) + ")";
    }

    @Override
    protected int calcDepth() {
        int maxDepth = 0;

        for (var f : formulaList)
            maxDepth = Math.max(maxDepth, f.getDepth());

        return 1 + maxDepth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AndFormula that = (AndFormula) o;

        return Objects.equals(formulaList, that.formulaList);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (formulaList != null ? formulaList.hashCode() : 0);
        return result;
    }
}
