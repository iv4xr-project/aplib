package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.*;

import java.util.function.Predicate;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.HealingPotion;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

/**
 * A more sophisticated demo of controlling MiniDungeon using Goals.
 * We set a goal for the agent to find and pick a specific scroll, and then 
 * to find the shrine to use the scroll on it. The used goals includes a 
 * tactic when the agent is attacked by monsters while it is on its way.
 * 
 * <p> The goal to search for scroll/shrine is enhanced to make the agent
 * to also pick up a healing potion if it happens to be close to the agent's
 * path, and the agent has a space in its bag. We use a mechanism to
 * dynamically deploy a goal to do this. 
 */
public class Demo2 {
	
	/**
	 * A goal to send the agent to pick up a healing pot nearby. It is a dynamic goal,
	 * as it will pick any such pot (rather than a specific one decided upfront).
	 */
	static GoalStructure grabHealPot(TestAgent agent, GoalLib goalLib) { 
		return DEPLOY(agent,
		  (MyAgentState S) -> {
			  var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
			  int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
			  var healPotsInVicinity = TacticLib.nearItems(S,HealingPotion.class,5) ;
			  //System.out.println("===== checking deploy grab ") ;
			  if (bagSpaceUsed>0 || healPotsInVicinity.size() == 0) {
			      return FAIL() ;
			  }
			  var pot = healPotsInVicinity.get(0) ;
			  //System.out.println("===== deploy grab " + pot.id) ;
			  return SEQ(
					    goalLib.EntityTouched(pot.id).lift(),
					    goalLib.EntityInteracted(pot.id).lift()) ;
		   }
	    ) ;
	}
	
	static Predicate<MyAgentState> whenToGoAfterHealPot = S -> {
		var player = S.worldmodel.elements.get(S.worldmodel.agentId) ;
		int bagSpaceUsed = (int) player.properties.get("bagUsed") ;
		var healPotsInVicinity = TacticLib.nearItems(S,HealingPotion.class,4) ;
		return agentIsAlive(S) && bagSpaceUsed==0 && healPotsInVicinity.size() > 0 ;
	} ;
	
	/**
	 * A smarter version of entityTouched(e) goal, that will also pick up a nearby
	 * healing potion along the way, if the bag is empty. (well this works for Frodo, but
	 * not for Smeagol, that needs to be smarter than this).
	 */
	static GoalStructure SmartEntityTouched(
				TestAgent agent, 
			 	GoalLib goalLib, 
			 	String targetId) { 
	   var G = INTERRUPTIBLE(
			       goalLib.EntityTouched(targetId),
			       HANDLE(whenToGoAfterHealPot,grabHealPot(agent,goalLib))
			    ) ;
	   return G ;
	}
	
	public static void main(String[] args) throws InterruptedException {
		// Create an instance of the game:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4 ;
		config.viewDistance = 4 ;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		var goalLib = new GoalLib() ;
		
		var agent = new TestAgent("Frodo","Frodo")  ;
		
		//
		// Specify a goal for the agent: search and grab scroll S0 then use it on the Shrine.
		//
		var G = SEQ(
				//goalLib.EntityTouched("S0").lift(),
				//goalLib.EntityTouched("S1").lift(),
				SmartEntityTouched(agent,goalLib,"S0"),
				goalLib.EntityInteracted("S0").lift(),
				//goalLib.EntityInteracted("S1").lift(),
				SmartEntityTouched(agent,goalLib,"Shr"),
				//goalLib.EntityTouched("Shr").lift(),
				//SmartEntityTouched(agent,goalLib,"Shr"),
				goalLib.EntityInteracted("Shr").lift()) ;

		// Now, create an agent, attach the game to it, and give it the above goal:
		agent. attachState(state)
			 . attachEnvironment(env)
			 . setGoal(G) ;

		Thread.sleep(1000);
		
		//state.updateState("Frodo");
		//printEntities(state) ;
		
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(200); 
			if (k>=300) break ;
			k++ ;
		}	
		G.printGoalStructureStatus();	
		//System.exit(0);	
	}

}
