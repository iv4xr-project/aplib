package nl.uu.cs.aplib.ExampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.*;

import nl.uu.cs.aplib.Environments.SimpleSystemConsoleEnv;
import nl.uu.cs.aplib.MainConcepts.*;


public class DumbDoctor {
	
	static public class DoctorBelief extends SimpleState {
		Integer patientHappiness = 0 ;
		
		@Override
		public SimpleSystemConsoleEnv env() { return (SimpleSystemConsoleEnv) super.env() ; }
	}
	
	static public void main(String[] args) {
	  // specifying the goal to solve:
	  Goal g = goal("the-goal").toSolve((Integer happiness) -> happiness >= 5) ;
	   
	  // defining few actions for the agent:
	  var opening = action("opening")
			.desc("To say an opening question to the patient.")
			.do1_((DoctorBelief belief) -> actionstate_ -> {
			  belief.env().ask("How do you feel today?");
			  return ++belief.patientHappiness ;
		  })
		  .on_((DoctorBelief belief) -> belief.patientHappiness == 0) 
		  .lift()
		  ;
	
	  var a1 = action("a1")
			.desc("To ask a 'smart' question to the patient :D")
			.do1_((DoctorBelief belief) -> actionstate_ -> {
			  belief.env().ask("Please explain a bit more...");
			  return ++belief.patientHappiness ;
		       })
			.lift();
	
	  var a2 = action("a2")
			.desc("Another 'smart' question to pose to the patient ;)")
			.do1_((DoctorBelief belief) -> actionstate_ -> {
			  belief.env().ask("I see... And why is that?");
			  return ++belief.patientHappiness ;
		      })
			.lift() ;
	      

      // creating the agent, and configuring it:
      GoalTree topgoal = lift(g.withStrategy(FIRSTof(opening,ANYof(a1,a2)))) ;
      var belief = new DoctorBelief() ;
      belief.setEnvironment(new SimpleSystemConsoleEnv()) ;      
      var doctorAgent = new BasicAgent() . attachState(belief) . setGoal(topgoal) ;

      // run the doctor-agent until it solves its goal:
      while (topgoal.getStatus().inProgress()) {
    	  doctorAgent.update(); 
      }
      if(g.getStatus().sucess()) 
    	  belief.env().println("I am glad you are happier now :)");
  

	}

}
