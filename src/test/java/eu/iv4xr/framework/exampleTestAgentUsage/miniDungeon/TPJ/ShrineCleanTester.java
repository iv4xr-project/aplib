package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;

import static nl.uu.cs.aplib.AplibEDSL.* ;
import static eu.iv4xr.framework.Iv4xrEDSL.* ;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;

public class ShrineCleanTester {
	
	public TacticLib tacticLib = new TacticLib() ;
	public GoalLib goalLib = new GoalLib() ;
	
	static WorldEntity hasScroll(WorldModel wom) {
		for (var e : wom.elements.values()) {
			if (e.type.equals("" + EntityType.SCROLL)) {
				return e ;
			}
		}
		return null ;
	}
	
    public boolean useSurvivalTactic = true ;
    
    public void setToUseMemorizedPathFinding() {
    	tacticLib.delayPathReplan = true ;
    	goalLib.tacticLib.delayPathReplan = true ;
    }
	
	public GoalStructure areaExplored(TestAgent agent) {
		
		Tactic exploreTac = FIRSTof(
				  tacticLib.useHealingPotAction().on_(tacticLib.hasHealPot_and_HpLow).lift(),
				  tacticLib.useRagePotAction().on_(tacticLib.hasRagePot_and_inCombat).lift(),
				  tacticLib.attackMonsterAction().on_(tacticLib.inCombat_and_hpNotCritical).lift(),
				  SEQ(addBefore(S -> { 
					    System.out.println(">>> deploying grab heal-pot.") ;
					    return goalLib.grabPot(agent, EntityType.HEALPOT) ;} )
					        .on_(goalLib.whenToGoAfterHealPot)
					        .lift(), 
					   ABORT()),
				   SEQ(addBefore(S -> { 
					    System.out.println(">>> deploying grab rage-pot.") ;
					    return goalLib.grabPot(agent, EntityType.RAGEPOT) ;} )
					        .on_((MyAgentState S) -> 
					                S.worldmodel.agentId.equals("Frodo") // only let Frodo get rage
					        		&& goalLib.whenToGoAfterRagePot.test(S))
					        .lift(), 
					   ABORT()),
				  tacticLib.explore(null),
				  ABORT()) ;
		
		if (!useSurvivalTactic) {
			exploreTac = FIRSTof(tacticLib.explore(null), ABORT()) ;
		}
		
		GoalStructure explr = goal("exploring (persistent-goal: aborted when it is terminated)")
		   .toSolve(belief -> false)
		   .withTactic(exploreTac)
		   .lift() ;
		
		// could use WHILE, but let's use REPEAT here:
		return REPEAT(
				FIRSTof(lift((MyAgentState S) -> tacticLib.explorationExhausted(S)) , 
				        SEQ(explr))) ;

	}
	
	static boolean shrineIsClean(MyAgentState S, String shrine) {
		var s = S.worldmodel.elements.get(shrine) ;
		if (s==null) return false ;
		return s.getBooleanProperty("cleansed" ) ;
	}
	
	private GoalStructure myEntityInCloseRange(TestAgent agent, String id) {
		if (useSurvivalTactic) {
			return goalLib.smartEntityInCloseRange(agent,id) ;
		}
		else {
			return goalLib.entityInCloseRange(id) ;
		}
	}
	
	/**
	 * Cleansing a shrine.
	 */
	public GoalStructure cleanseShrine(TestAgent agent, String shrine) {
		
		GoalStructure testScroll =
			SEQ(DEPLOY(agent,(MyAgentState S) -> {
					  var scroll = hasScroll(S.worldmodel) ;
					  return SEQ(myEntityInCloseRange(agent,scroll.id),
						         goalLib.entityInteracted(scroll.id)) ;
					   }),
				 myEntityInCloseRange(agent,shrine),
				 goalLib.entityInteracted(shrine)) ;
		
		return SEQ(areaExplored(agent),
				   // could use WHILE here, but lets use REPEAT:
				   REPEAT(FIRSTof(
						   lift((MyAgentState S) -> shrineIsClean(S,shrine)),
						   lift((MyAgentState S) -> hasScroll(S.worldmodel) == null), 
						   SEQ(testScroll, FAIL()))),
				   assertTrue_(agent,"Shrine check", 
						   "Shrine " + shrine + " should be open.",
						   (MyAgentState S) -> { 
							   boolean ok = shrineIsClean(S,shrine) ; 
							   //assertTrue(ok) ;
							   return ok ;} )
				   );
	}
	
	/**
	 * Cleansing all shrine.
	 * 
	 * @param numberOfLevels The number of levels/mazes in the game.
	 */
	public GoalStructure cleanseAllShrines(TestAgent agent, int numberOfLevels) {
		GoalStructure[] scenario = new GoalStructure[numberOfLevels] ;
		for (int k=0; k<numberOfLevels-1; k++) {
			String shrine = "SM" + k ;
			scenario[k] = SEQ(cleanseShrine(agent,shrine), goalLib.entityInteracted(shrine)) ;
		}
		// for last shrine, immortal shrine:
		int n = numberOfLevels - 1 ;
		String shrine = "SI" + n ;
		scenario[n] = cleanseShrine(agent,shrine) ;
		return SEQ(scenario) ;
	}
	
	
}
