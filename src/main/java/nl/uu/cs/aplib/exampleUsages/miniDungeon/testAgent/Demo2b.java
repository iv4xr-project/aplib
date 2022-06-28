package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib.*;

import java.util.function.Predicate;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
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
public class Demo2b {
	
	
	public static void main(String[] args) throws Exception {
		// Create an instance of the game:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4 ;
		config.viewDistance = 4 ;
		config.nuberOfMaze = 2 ;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false ;
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		var goalLib = new GoalLib() ;
		
		String player = "Smeagol" ;
		//String player = "Frodo" ;
		var agent = new TestAgent(player,player)  ;
		
		var G = SEQ(
				  goalLib.smartEntityInCloseRange(agent,"S0_1"),
				  goalLib.entityInteracted("S0_1"),
				  goalLib.smartEntityInCloseRange(agent,"SM0"),
				  goalLib.entityInteracted("SM0"),
				  goalLib.entityInteracted("SM0"),
				  goalLib.smartEntityInCloseRange(agent,"SS1"),
				  goalLib.entityInteracted("SS1"),
				  goalLib.smartEntityInCloseRange(agent,"S0_0"),
				  goalLib.smartEntityInCloseRange(agent,"SI1")
				  //goalLib.entityInteracted("SI1")
				  
				) ;

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
			var player_ = state.worldmodel.elements.get(player) ;
			int mapId = (int) player_.properties.get("maze") ;
			int x ;
			int y ;
			if (player.equals("Frodo")) {
				x = app.dungeon.frodo().x ;
				y = app.dungeon.frodo().y ;
			}
			else {
				x = app.dungeon.smeagol().x ;
				y = app.dungeon.smeagol().y ;
			}
			System.out.println("** [" + k + "/" + app.dungeon.turnNr + "] agent in maze:" 
					+ mapId + ", @" + Utils.toTile(state.worldmodel.position)
					+ ", actual pos @(" + x + "," + y + ")"
					) ;
			//System.out.println("   #entities SS1 in wom:" + SS1.count()) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(20); 
			if (k>=1000) break ;
			k++ ;
		}	
		//G.printGoalStructureStatus();	
		
		state.updateState(player);
		
		
		WorldModel wom ;
		
		PrintUtils.printEntities(state) ;
		var path = state.multiLayerNav.findPath(Utils.loc3(1,1,2), Utils.loc3(1,18,1)) ;
		System.out.println("\n== path: " + path) ;
		path = adjustedFindPath(state, 0,17,2, 1,18,1) ;
		System.out.println("\n== path: " + path) ;
		
		
		wom = state.env().observe(player) ;
		//System.out.println("==============") ;
		//Utils.printEntities(wom);
		
		
		//Utils.printInternalEntityState(app.dungeon) ;
		//Utils.printNonWalls(state) ;
		
		//System.exit(0);	
	}

}
