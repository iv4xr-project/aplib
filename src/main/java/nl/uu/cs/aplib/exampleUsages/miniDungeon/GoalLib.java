package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.*;
import static nl.uu.cs.aplib.AplibEDSL.* ;
import eu.iv4xr.framework.mainConcepts.*;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.TacticLib.* ;

import java.util.Scanner;

public class GoalLib {
	
	TacticLib tacticLib = new TacticLib() ;
	
	public GoalStructure EntityTouched(String targetId) {
		
		var G = goal("Entity " + targetId + " is touched.") 
				.toSolve((WorldModel wom) -> {
					WorldEntity e = wom.elements.get(targetId) ;
					if (e==null) return false ;
					return adjacent(toTile(wom.position),toTile(e.position)) ;
				})
				.withTactic(
				   FIRSTof(
						   tacticLib.navigateTo(targetId),
						   tacticLib.explore(),
				   		   ABORT()) 
				  )
				.lift()
				;
		
		return G ;		
	}
	
	public GoalStructure EntityInteracted(String targetId) {
		
		var G = goal("Entity " + targetId + " is interacted.") 
				.toSolve((WorldModel wom) -> true)
				.withTactic(
				   FIRSTof(
						   tacticLib.interact(targetId),
						   ABORT()) 
				  )
				.lift()
				;
		
		return G ;
		
	}
	
	
	public static void main(String[] args) throws InterruptedException {

		// System.out.println(">>>" + Frodo.class.getSimpleName()) ;

		MiniDungeonConfig config = new MiniDungeonConfig();
		config.viewDistance = 4 ;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		var goalLib = new GoalLib() ;
		var G = SEQ(
				goalLib.EntityTouched("S0"),
				goalLib.EntityInteracted("S0"),
				goalLib.EntityTouched("Shr"),
				goalLib.EntityInteracted("Shr")) ;

		var agent = new TestAgent("Frodo","Frodo") 
				. attachState(state)
				. attachEnvironment(env)
				. setGoal(G) ;

		Thread.sleep(2000);
		
		state.updateState("Frodo");
		printEntities(state) ;
		
		//System.exit(0);
		
		System.out.println(">> Start agent loop...") ;
		//var scanner = new Scanner(System.in) ;
		//scanner.nextLine() ;

		int k = 0 ;
		
		while(G.getStatus().inProgress()) {
			System.out.println("** [" + k + "] agent @" + toTile(state.worldmodel.position)) ;
			agent.update();
			Thread.sleep(30); 
			if (k>=150) break ;
			k++ ;
		}
		
		G.printGoalStructureStatus();
		
		//System.exit(0);
		
	}

}
