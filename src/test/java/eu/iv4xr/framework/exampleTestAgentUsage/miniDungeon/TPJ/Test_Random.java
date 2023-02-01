package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.adjacent;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.mazeId;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils.toTile;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon ;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils; 

public class Test_Random {
	
	boolean withGraphics = true ;
	boolean supressLogging = true ;
	
	TacticLib tacticLib = new TacticLib() ;
	GoalLib goalLib = new GoalLib() ;
	
	Command[] commands = { Command.MOVEDOWN, Command.MOVEUP, Command.MOVELEFT, Command.MOVERIGHT,
			Command.USEHEAL, Command.USERAGE } ;
	
	/**
	 * Make a weigthed random choice of integers in 0..N-1.
	 */
	static int choose(Float[] weights, Random rnd) {
		float sum = Arrays.stream(weights).reduce(0f, (x,r) -> x+r) ;
		// ok generate a random number:
		float rndNumber = rnd.nextFloat() ;
		// now find out the choice that correspond to that:
		float b = 0f ;
		for (int k=0; k<weights.length-1; k++) {
			b = b + weights[k]/sum ;
			if (rndNumber < b) return k ; 
		}
		return weights.length-1 ;
	}
	
	static boolean hasNewObject(WorldModel wom, WorldModel newWom) {
		var olds = wom.elements.keySet() ;
		for (var e : newWom.elements.keySet()) {
			if (! olds.contains(e)) return true ;
		}
		return false ;
	}
	
	Random rnd = new Random() ;
	
	
	/**
	 * Doing random action. The choice is weigthed by the "reward" of doing 
	 * the action. When an action is chosen, and it leads to the discovery 
	 * of new object, its reward is increased by some amount. If it does not,
	 * its reward is reset to 1.
	 */
	public Tactic curiousityWeigthedRandomAction() {
		
		Float[] rewards = new Float[commands.length] ;
		for (int k=0; k<rewards.length; k++) rewards[k] = 1f ;
		
		return action("random action").do1((MyAgentState S) -> {
			var k = choose(rewards,rnd) ;
		    var obs = S.env().action(S.worldmodel.agentId,commands[k]) ;
		    if (hasNewObject(S.worldmodel,obs)) {
		    	rewards[k] += 3f ;
		    }
		    else rewards[k] = 1f ;
		    return null ;
		    })
		.lift()
		;
	}
	
	public GoalStructure smarterRandomTest(TestAgent agent) {
		
		Tactic smarterRandomTactic = FIRSTof(
				tacticLib.useHealingPotAction().on_(tacticLib.hasHealPot_and_HpLow).lift(),
				SEQ(addBefore(S -> { 
					    System.out.println(">>> deploying grab heal-pot.") ;
					    return goalLib.grabPot(agent, EntityType.HEALPOT) ;} )
					.on_(goalLib.whenToGoAfterHealPot)
					.lift(), 
					ABORT()),
				curiousityWeigthedRandomAction() ) ;

		GoalStructure smarterRandomTestG = goal("random test").toSolve(obs -> false)
			 .withTactic(smarterRandomTactic)
			 .lift() ;
		return REPEAT(smarterRandomTestG) ;

	}
	
	Action action_(Command cmd) {
		return action("" + cmd)
		.do1((MyAgentState S) -> S.env().action(S.worldmodel.agentId,cmd)) ;
	}
	
	/**
	 * Test that just randomly do actions.
	 */
	public GoalStructure randomTest() {
		return goal("random test")
			.toSolve(obs -> false)
			.withTactic(
			    ANYof(action_(Command.MOVEDOWN).lift(),
			    	  action_(Command.MOVEUP).lift(),
			    	  action_(Command.MOVELEFT).lift(),
			    	  action_(Command.MOVERIGHT).lift(),
			    	  action_(Command.USEHEAL).lift(),
			    	  action_(Command.USERAGE).lift()))
			.lift() ;
	}
	
	@Test
	public void testRandom1() throws Exception {

		MiniDungeonConfig config = TPJconfigs.MDconfig0();
		System.out.println(">>> Configuration:\n" + config);

		String agentId = "Frodo";
		// create an agent:
		var agent = new TestAgent(agentId, "tester");

		GoalStructure G = randomTest();
		int sleep = 0 ;
		boolean stopAfterAgentDie = true ;
		TPJUtils.runAgent(agent, config, G, 4000, sleep, stopAfterAgentDie, withGraphics, supressLogging);
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	@Test
	public void testRandom2() throws Exception {

		MiniDungeonConfig config = TPJconfigs.MDconfig1();
		System.out.println(">>> Configuration:\n" + config);

		String agentId = "Frodo";
		// create an agent:
		var agent = new TestAgent(agentId, "tester");

		GoalStructure G = randomTest();
		int sleep = 0 ;
		boolean stopAfterAgentDie = true ;
		TPJUtils.runAgent(agent, config, G, 4000, sleep,stopAfterAgentDie,withGraphics, supressLogging);
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine();
	}
	
	@Test
	public void smarterTestRandom1() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig0() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		// create an agent:
		var agent = new TestAgent(agentId, "tester");
		
		GoalStructure G = smarterRandomTest(agent) ;
		int sleep = 0 ;
		boolean stopAfterAgentDie = true ;
		TPJUtils.runAgent(agent,config,G,3000,sleep,stopAfterAgentDie,withGraphics,supressLogging) ;		
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	
	@Test
	public void smarterTestRandom2() throws Exception {
		
		MiniDungeonConfig config = TPJconfigs.MDconfig1() ;
		System.out.println(">>> Configuration:\n" + config);
		
		String agentId = "Frodo" ;		
		// create an agent:
		var agent = new TestAgent(agentId, "tester");
		
		GoalStructure G = smarterRandomTest(agent) ;
		int sleep = 0 ;
		boolean stopAfterAgentDie = true ;
		TPJUtils.runAgent(agent,config,G,3000,sleep,stopAfterAgentDie,withGraphics,supressLogging) ;		
		assertTrue(agent.evaluateLTLs()) ;
		//(new Scanner(System.in)).nextLine() ;
	}
	
	

}
