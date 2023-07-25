package jason.asSemantics.planning.planner.epplan;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

public class EpistemicPlanner {

    public EpistemicPlanner() {
    }

    public native boolean emplan(String var1, String var2, String var3, String var4, String var5);

    public native String emplanStream(String var1);

    public boolean emplan(String problemFile, String planFile) {
        return this.emplan(problemFile, planFile, "", "", "");
    }
}
