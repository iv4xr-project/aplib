package nl.uu.cs.aplib.exampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.*;

import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.mainConcepts.*;

public class DumbDoctor {

    static public class DoctorBelief extends SimpleState<ConsoleEnvironment> {
        Integer patientHappiness = 0;
    }

    static public void main(String[] args) {
        // specifying the goal to solve:
        Goal g = goal("the-goal").toSolve((Integer happiness) -> happiness >= 5);

        // defining few actions for the agent:
        var opening = action("opening").desc("To say an opening question to the patient.")
                .do1((DoctorBelief belief) -> {
                    belief.env().ask("How do you feel today?");
                    return ++belief.patientHappiness;
                }).lift();

        var a1 = action("a1").desc("To ask a 'smart' question to the patient :D").do1((DoctorBelief belief) -> {
            belief.env().ask("Please explain a bit more...");
            return ++belief.patientHappiness;
        }).lift();

        var a2 = action("a2").desc("Another 'smart' question to pose to the patient ;)").do1((DoctorBelief belief) -> {
            belief.env().ask("I see... And why is that?");
            return ++belief.patientHappiness;
        }).lift();

        // Specifying a tactic to solve the previously set goal:
        g.withTactic(FIRSTof(opening.on_((DoctorBelief belief) -> belief.patientHappiness == 0), ANYof(a1, a2)));
        // setting g as the top goal:
        GoalStructure topgoal = g.lift();

        // creating a doctor-agent, attaching state to it, and the above topgoal to it:
        var belief = new DoctorBelief();
        belief.setEnvironment(new ConsoleEnvironment());
        var doctorAgent = new BasicAgent().attachState(belief).setGoal(topgoal);

        // run the doctor-agent until it solves its goal:
        while (topgoal.getStatus().inProgress()) {
            doctorAgent.update();
        }
        if (topgoal.getStatus().success())
            belief.env().println("I am glad you are happier now :)");

        topgoal.printGoalStructureStatus();

    }

}
