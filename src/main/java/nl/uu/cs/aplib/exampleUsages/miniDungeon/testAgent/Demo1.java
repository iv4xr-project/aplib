package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

/**
 * A simple demo of controlling MiniDungeon using Goals. We set a simple goal
 * for the agent to find and pick a specific scroll, and then to find the shrine
 * to use the scroll on it. The used goals includes a tactic when the agent is
 * attacked by monsters while it is on its way.
 * 
 * @author wish
 */
public class Demo1 {
	
	public static void main(String[] args) throws Exception {
		// Create an instance of the game:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.viewDistance = 6 ;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		var goalLib = new GoalLib() ;
		//
		// Specify a goal for the agent: search and grab scroll S0 then use it on the Shrine.
		//
		var G = SEQ(
				goalLib.entityInCloseRange("S0_1"),
				goalLib.entityInteracted("S0_1"),
				goalLib.entityInCloseRange("SM0"),
				goalLib.entityInteracted("SM0")) ;

		// Now, create an agent, attach the game to it, and give it the above goal:
		var agent = new TestAgent("Frodo","Frodo") 
				. attachState(state)
				. attachEnvironment(env)
				. setGoal(G) ;

		Thread.sleep(1000);
		
		state.updateState("Frodo");
		PrintUtils.printEntities(state) ;
		
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(50); 
			if (k>=150) break ;
			k++ ;
		}	
		//System.exit(0);	
	}

}
