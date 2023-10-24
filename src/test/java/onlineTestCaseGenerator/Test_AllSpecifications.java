package onlineTestCaseGenerator;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TestMiniDungeonWithAgent;
import eu.iv4xr.framework.extensions.ltl.SATVerdict;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.CoverterDot;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLibExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MiniDungeonModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.ModelLearner;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.ModelLearnerExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentStateExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Specifications;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;
import onlineTestCaseGenerator.Sa3Solver3;
import onlineTestCaseGenerator.SaSolver4MultiMaze;
import onlineTestCaseGenerator.Sa1Solver.Policy;

public class Test_AllSpecifications {

	static GoalStructure exhaustiveExplore(TestAgent agent, GoalLib goalLib, TacticLib tacticLib) {
		return WHILE(S -> !tacticLib.explorationExhausted(S), goalLib.smartExploring(agent, null, Integer.MAX_VALUE));
	}

	List<String> getConnectedEnablersFromBelief(Object shrine, MyAgentState S) {
		List<String> openers = new LinkedList<>();
		if (S.gwmodel != null) {
			for (var entry : S.gwmodel.objectlinks.entrySet()) {
				if (entry.getValue().contains(shrine)) {
					openers.add(entry.getKey());
					return openers;
				}
			}
		}
		return openers;
	}
	public Pair<Boolean, Long> testFullPlay(String agentId, TestAgent agent, Pair<String,String> targetItemOrShrine,Pair additionalFeature, Predicate<Iv4xrAgentState> sp,
			 MyAgentState state,MyAgentEnv env) throws Exception {
		
		// Goal: find a shrine and cleanse it:
		// Pair targetItemOrShrine = new Pair("type","HEALPOT") ;
		var goalLib = new GoalLib();
		var tacticLib = new TacticLib();
		var goalLibExtended = new GoalLibExtended();
		var SaSolver4MultiMaze = new SaSolver4MultiMaze<Void>(
				(S, e) -> Utils.isReachable((MyAgentState) S, e),
				(S, e) -> Utils.distanceToAgent((MyAgentState) S, e),
				S -> (e1, e2) -> Utils.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				(S, eId) -> SEQ(
						goalLibExtended.smartEntityInCloseRange((MyAgentStateExtended) S,agent, eId), goalLib.entityInteracted(eId)
						),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId),
						goalLib.entityInteracted(eId)),
				S -> tacticLib.explorationExhausted(S), 
				dummy -> exhaustiveExplore(agent, goalLib, tacticLib));
		
		
			System.out.println("******invoking solver for specification: " + targetItemOrShrine.toString());
			
			var G = SaSolver4MultiMaze.solver(agent, targetItemOrShrine, additionalFeature, new Vec3(20, 1, 1),
					e -> e.type.equals("" + EntityType.SHRINE), e -> e.type.equals("" + EntityType.SCROLL),
					e -> e.type.equals("" + EntityType.SHRINE) && (boolean) e.properties.get("cleansed"),
					(shrine, S) -> getConnectedEnablersFromBelief(shrine, (MyAgentState) S),
					// we can't close a shrine again once it is cleaned, so this is not needed:
					(shrine, S) -> null,
					sp
				, Policy.NEAREST_TO_TARGET);			
		
		
		
		if (agent.state() == null) {
			agent.attachState(state).attachEnvironment(env).setGoal(G);
		}
		else {
			agent.setGoal(G);
		}
			
		
		
		GameWorldModel model = new GameWorldModel(new GWState());
		// GameWorldModel model =
		// GameWorldModel.loadGameWorldModelFromFile("./tmp/modelMD1.json") ;

		ModelLearnerExtended modelLearner = new ModelLearnerExtended();
		agent.attachBehaviorModel(model, (S, m) -> {
			modelLearner.learn((MyAgentState) S, (GameWorldModel) m);
			return null;
		});
		
		
		Thread.sleep(1000);
		// why do we need this starting update?
		if(state.worldmodel != null) {System.out.println(">>>> agent maze " +  state.worldmodel.elements.get(agentId).getIntProperty("maze"));}
		state.updateState(agentId);
		// Utils.printEntities(state);
		// Now we run the agent:
		int delay = 20;
		long time0 = System.currentTimeMillis();
		System.out.println(">>> G status befor start = " + G.getStatus() );
		TestMiniDungeonWithAgent.runAndCheck(agent, G, true, delay, 12000);

		System.out.println(">>> G status = " + G.getStatus());

		Scanner scanner = new Scanner(System.in);
		// scanner.nextLine() ;
		assertTrue(G.getStatus().success()) ;

		System.out.println(">>> exec-time = " + (System.currentTimeMillis() - time0));

		model.defaultInitialState.currentAgentLocation = "START" + agentId;
		model.copyDefaultInitialStateToInitialState();
		model.name = "MiniDungeon";
		model.supportUSE = true;  
		// attaching alpha to the model:
		model.alpha = (i,affected) -> S -> { MiniDungeonModel.alphaFunction(agentId,i,affected,S) ; return null ; } ;
		model.use_alpha = (S,i) -> { MiniDungeonModel.alphaUseFunction(agentId,i,S) ; return null; } ;
		String fileName1 = "./tmp/modelMD1.json" ;
		String fileName2 = "./tmp/modelMD1.dot" ;
		model.save(fileName1);
		CoverterDot.saveAs(fileName2, model, true, true) ;

		
		System.out.println(">>>> LTL results: inside ");
		//
//		var specs = new Specifications();
//		var psi1 = specs.spec1();
//		var psi2 = specs.spec2();
//		var psi3 = specs.spec3();
//		var psi4 = specs.spec4();
//		var psi5 = specs.spec5();
//		var psiSp1 = specs.scenarioSpec1();
//		var psiSp2 = specs.scenarioSpec2();
//		var psiSp3 = specs.scenarioSpec3();
//		var psiSp4 = specs.scenarioSpec4();
//		var psiSp5 = specs.scenarioSpec5();
//		var psiSp6 = specs.scenarioSpec6();
//		agent.addLTL(psi2);
//		agent.resetLTLs();
//
//		
//		/* Check the specifications */
//	//	var ok = agent.evaluateLTLs();
//
//	//	System.out.println(">>>> LTL results: " + ok);
//		System.out.println(">>>> psi1 : " + psi1.sat());
//		System.out.println(">>>> psi2 : " + psi2.sat());
//		System.out.println(">>>> psi3 : " + psi3.sat());
//		System.out.println(">>>> psi4 : " + psi4.sat());
//		System.out.println(">>>> psi5 : " + psi5.sat());
//		System.out.println(">>>> psi6 : " + psiSp1.sat());
//		System.out.println(">>>> psi7 : " + psiSp2.sat());
//		System.out.println(">>>> psi8 : " + psiSp3.sat());
//		System.out.println(">>>> psi9 : " + psiSp4.sat());
//		System.out.println(">>>> psi9 : " + psiSp5.sat());
//		System.out.println(">>>> psi9 : " + psiSp6.sat());
//		assertTrue(psi1.sat() == SATVerdict.SAT	
//				&& psi2.sat() == SATVerdict.SAT
//				&& psi3.sat() == SATVerdict.SAT
//				&& psi4.sat() == SATVerdict.SAT
//				&& psiSp1.sat() == SATVerdict.SAT
//				&& psiSp2.sat() == SATVerdict.SAT
//				&& psiSp3.sat() == SATVerdict.SAT
//				) ;
		
		var totalTime  = (System.currentTimeMillis() - time0) /1000;
		return new Pair<Boolean, Long>(G.getStatus().success(),totalTime);
	}

}
