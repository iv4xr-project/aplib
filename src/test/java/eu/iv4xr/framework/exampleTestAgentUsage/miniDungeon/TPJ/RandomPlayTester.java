package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;


import static nl.uu.cs.aplib.AplibEDSL.* ;

import java.util.*;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;

public class RandomPlayTester {
	
	
	TacticLib tacticLib = new TacticLib() ;
	GoalLib goalLib = new GoalLib() ;
	
	public boolean reallyRandom = false ;
	public boolean useSurvivalTactic = true ;
	
	Random rnd = new Random() ;
	
	public RandomPlayTester() { }
	
	public RandomPlayTester(long seed) {
		rnd = new Random(seed) ;
	}
	
	
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
	
	static float novelty(WorldModel wom, WorldModel newWom) {
		float nov = 0 ;
		if (! wom.val("hp").equals(newWom.val("hp")))
			nov += 0.5 ;
		if (hasNewObject(wom,newWom))
			nov += 0.5 ;
		return nov ;
	}
	
	
	
	
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
		    if (novelty(S.worldmodel,obs) > 0) {
		    	rewards[k] += 3f ;
		    }
		    else rewards[k] = 1f ;
		    return null ;
		    })
		.lift()
		;
	}
	
	/**
	 * Random, with novelty driver, and a bit of survival tactic.
	 */
	GoalStructure smarterRandomPlay(TestAgent agent) {
		
		Tactic smarterRandomTactic = curiousityWeigthedRandomAction() ;
		if (useSurvivalTactic) {
			smarterRandomTactic = FIRSTof(
				tacticLib.useHealingPotAction().on_(tacticLib.hasHealPot_and_HpLow).lift(),
				SEQ(addBefore(S -> { 
					    System.out.println(">>> deploying grab heal-pot.") ;
					    return goalLib.grabPot(agent, EntityType.HEALPOT) ;} )
					.on_(goalLib.whenToGoAfterHealPot)
					.lift(), 
					ABORT()),
				curiousityWeigthedRandomAction() ) ;
		}

		GoalStructure smarterRandomTestG = goal("random test").toSolve(obs -> false)
			 .withTactic(smarterRandomTactic)
			 .lift() ;
		return REPEAT(SEQ(SUCCESS(),smarterRandomTestG)) ;

	}
	
	Action action_(Command cmd) {
		return action("" + cmd)
		.do1((MyAgentState S) -> S.env().action(S.worldmodel.agentId,cmd)) ;
	}
	
	/**
	 * Test that just randomly do actions.
	 */
	GoalStructure simpleRandomPlay() {
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
	
	
	/**
	 * Produce a random play.
	 */
	public GoalStructure randomPlay(TestAgent agent) {
		if (reallyRandom) return simpleRandomPlay() ;
		else return smarterRandomPlay(agent) ;
	}
	

}
