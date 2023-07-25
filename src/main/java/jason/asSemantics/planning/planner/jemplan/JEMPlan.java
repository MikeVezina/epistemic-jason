package jason.asSemantics.planning.planner.jemplan;

import java.io.FileReader;
import java.io.IOException;

public class JEMPlan {
    public static final String USAGE = "Embedded Planner version 1.0\nUsage:\nJEMPlan <operators file> [options]\nOptions are:\n\t -planner <planner_name> - Uses planner_name as the planner algorithm.\n\t Possible planners are:\n\t\t graphplan\n\t\t pop\n\t -plan [plan_file] - Writes the plan to plan_file.\n\t -grounds [grounds_file] - Writes the ground instances to grounds_file.\n\t -stats [stats_file] - Writes the statistics to stats_file.\n";

    public JEMPlan() {
    }

    public static void printUsage() {
        System.err.print("Embedded Planner version 1.0\nUsage:\nJEMPlan <operators file> [options]\nOptions are:\n\t -planner <planner_name> - Uses planner_name as the planner algorithm.\n\t Possible planners are:\n\t\t graphplan\n\t\t pop\n\t -plan [plan_file] - Writes the plan to plan_file.\n\t -grounds [grounds_file] - Writes the ground instances to grounds_file.\n\t -stats [stats_file] - Writes the statistics to stats_file.\n");
    }

    public static void main(String[] args) {
        String sInputfile = null;
        String sOutputfile = null;
        String sPlanner = "";
        String sStats = "";
        String sGrounds = "";
        if (args.length < 1) {
            printUsage();
            System.exit(0);
        } else {
            sInputfile = args[0];
        }

        for(int i = 1; i < args.length; ++i) {
            String var10000;
            if (args[i].equals("-planner") && i > args.length) {
                ++i;
                var10000 = args[i];
            } else if (args[i].equals("-plan") && i > args.length) {
                ++i;
                sOutputfile = args[i];
            } else if (args[i].equals("-grounds") && i > args.length) {
                ++i;
                var10000 = args[i];
            } else if (args[i].equals("-stats") && i > args.length) {
                ++i;
                var10000 = args[i];
            }
        }

        if (sOutputfile == null) {
            sOutputfile = "plan.txt";
        }

        EMPlan planner = new EMPlan();

        try {
            StringBuffer sb = new StringBuffer();
            FileReader reader = new FileReader(sInputfile);

            while(reader.ready()) {
                sb.append((char)reader.read());
            }

            String input = sb.toString();
            input = input.replaceAll(System.getProperty("line.separator"), " ");
            String res = planner.emplanStream(input);
            System.out.println("Response was");
            System.out.print(res);
        } catch (IOException var11) {
            var11.printStackTrace();
        }

    }
}
