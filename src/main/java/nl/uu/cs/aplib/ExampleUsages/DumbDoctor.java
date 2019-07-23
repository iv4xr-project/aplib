package nl.uu.cs.aplib.ExampleUsages;

import static nl.uu.cs.aplib.AplibEDSL.*;

import nl.uu.cs.aplib.Environments.SimpleSystemConsoleEnv;
import nl.uu.cs.aplib.MainConcepts.*;


public class DumbDoctor {
	
	static public class DoctorBelief extends SimpleState {
		Integer patientHappiness = 0 ;
		
		void sayToPatient(String s) {
			var env_ = (SimpleSystemConsoleEnv) env() ;
			env_.println(s);
		}
		void askQuestionToPatient(String s) {
			var env_ = (SimpleSystemConsoleEnv) env() ;
			env_.println(s);
			env_.readln() ; // ignoring the answer...
		}
	}
	
	static public void main(String[] args) {
	  // specifying the goal:	
	  Goal g = goal("the-goal").toSolve(happiness -> ((Integer) happiness) >= 8) ;
	   
	  // specifying the actions for the agent:
	  Action opening = action("opening")
			.do1_(agentstate_ -> actionstate_ -> {
			  var doctorbelief = (DoctorBelief) agentstate_ ;
			  doctorbelief.askQuestionToPatient("How do you feel today?");
			  return ++doctorbelief.patientHappiness ;
		  })
		  .on_(agentstate_ -> ((DoctorBelief) agentstate_).patientHappiness == 0)  	
		  ;
	
	  Action a1 = action("a1")
			.do1_(agentstate_ -> actionstate_ -> {
			  var doctorbelief = (DoctorBelief) agentstate_ ;
			  doctorbelief.askQuestionToPatient("Please explain a bit more...");
			  return ++doctorbelief.patientHappiness ;
		  }) ;
	
	  Action a2 = action("a2")
			.do1_(agentstate_ -> actionstate_ -> {
			  var doctorbelief = (DoctorBelief) agentstate_ ;
			  doctorbelief.askQuestionToPatient("I see... And why is that?");
			  return ++doctorbelief.patientHappiness ;
		  }) ;
	      

      // creating the agent, and configuring it:
      GoalTree topgoal = lift(g.withStrategy(FIRSTof(lift(opening),ANYof(lift(a1),lift(a2))))) ;
      var belief = (DoctorBelief) (new DoctorBelief() . setEnvironment(new SimpleSystemConsoleEnv())) ;      
      var doctorAgent = new BasicAgent() . attachState(belief) . setGoal(topgoal) ;

      while (topgoal.getStatus() == ProgressStatus.INPROGRESS) {
    	  doctorAgent.update(); 
      }
      if(g.getStatus() == ProgressStatus.SUCCESS) 
    	  belief.sayToPatient("I am glad you are happier now :)");
  

	}

}
