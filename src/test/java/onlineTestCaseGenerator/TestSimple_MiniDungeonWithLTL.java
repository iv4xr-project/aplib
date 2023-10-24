package onlineTestCaseGenerator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;
import java.util.function.Predicate;

import static nl.uu.cs.aplib.AplibEDSL.*;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.SimpleState;

import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

public class TestSimple_MiniDungeonWithLTL {
	
	// switch to true if you want to see graphic
	boolean withGraphics = false ;
	boolean supressLogging = true ;

	
	/**
	 * In this test the agent will search and pick up scroll S0_1,
	 * and then use it on the shrine of level-0. We expect that
	 * the shrine will then be cleansed.
	 */
	@Test
	public void test1() throws Exception {
		// Create an instance of the game:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4 ;
		config.viewDistance = 4 ;
		System.out.println(">>> Configuration:\n" + config);
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);
		//(new Scanner(System.in)).nextLine() ;
		
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		var goalLib = new GoalLib() ;
		
		//var agent = new TestAgent("Frodo","Frodo")  ;
		var agent = new TestAgent("Smeagol","Smeagol")  ;
		
		//
		// Specify a goal for the agent: search and grab scroll S0 then use it on the Shrine.
		//
		var G = SEQ(
				  goalLib.smartEntityInCloseRange(agent,"S0_1"),
				  goalLib.entityInteracted("S0_1"),
				  goalLib.smartEntityInCloseRange(agent,"SM0"),
				  goalLib.entityInteracted("SM0"),
				  SUCCESS()
				) ;
		
		//MyAgentState T = new MyAgentState();
		//T.worldmodel().val("hp") ;
				

		// Now, create an agent, attach the game to it, and give it the above goal:
		agent. attachState(state)
			 . attachEnvironment(env)
			 . setGoal(G) ;
		
		var specs = new SomeLTLSpecifications() ;
		var psi1 = specs.spec1() ;
		var psi2 = specs.spec2() ;
		var psi3 = specs.spec3() ;

		
		agent.addLTL(psi1,psi2,psi3) ;
		agent.resetLTLs() ;

		Thread.sleep(1000);
		
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(10); 
			if (k>=300) break ;
			k++ ;
		}	
		assertTrue(G.getStatus().success())	 ;
		WorldEntity shrine = state.worldmodel.elements.get("SM0") ;
		System.out.println("=== " + shrine) ;
		assertTrue((Boolean) shrine.properties.get("cleansed")) ;
		
		
		var ok = agent.evaluateLTLs() ;
		
		System.out.println(">>>> LTL results: " + ok) ;
		System.out.println(">>>> psi1 : " + psi1.sat()) ;
		System.out.println(">>>> psi2 : " + psi2.sat()) ;
		System.out.println(">>>> psi3 : " + psi3.sat()) ;
		
		assertTrue(ok) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	
}
