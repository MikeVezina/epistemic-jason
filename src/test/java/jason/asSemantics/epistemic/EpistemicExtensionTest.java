package jason.asSemantics.epistemic;

import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Rule;
import jason.runtime.Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class EpistemicExtensionTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void modelCreateSem() {
        var ts = new TransitionSystem(new Agent(), new Circumstance(), new Settings(), new AgArch());
        var ag = ts.getAg();
        ag.initAg();

        var es = new EpistemicExtension(ts);

        try {
            ag.getBB().add(new Rule(ASSyntax.parseLiteral("test(X, Y)"),
                    ASSyntax.parseFormula("((X = 5) & (Y = 2))")));

            var lc = es.getModelCreationConstraints();
            assertTrue(lc.isEmpty());
            ag.getBB().clear();


            ag.getBB().add(new Rule(ASSyntax.parseLiteral("range(test(X, Y))"),
                    ASSyntax.parseFormula("(((X = 5) & (Y = 2)) | ((X = 8) & (Y = 3)))")));
            lc = es.getModelCreationConstraints();
            assertEquals(lc.size(), 1);

            ag.getBB().add(new Rule(ASSyntax.parseLiteral("range(wow(X, Y))"),
                    ASSyntax.parseFormula("(((X = 5) & (Y = 2)) | ((X = 8) & (Y = 3)))")));
            lc = es.getModelCreationConstraints();
            assertEquals(lc.size(), 2);
            ag.getBB().clear();


            ag.getBB().add(new Rule(ASSyntax.parseLiteral("range(test(1, 1))"),
                    ASSyntax.parseFormula("(((X = 5) & (Y = 2)) | ((X = 8) & (Y = 3)))")));
            lc = es.getModelCreationConstraints();
            assertEquals(lc.size(), 1);
            ag.getBB().clear();

            ag.getBB().add(new Rule(ASSyntax.parseLiteral("range(test(1, 1))"),
                    ASSyntax.parseFormula("false")));
            lc = es.getModelCreationConstraints();
            assertEquals(lc.size(), 1);
            ag.getBB().clear();

        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e);
        }


        System.out.println();
    }
}
