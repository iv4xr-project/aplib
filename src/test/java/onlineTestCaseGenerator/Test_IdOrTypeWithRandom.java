package onlineTestCaseGenerator;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.swing.text.html.parser.Entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TestMiniDungeonWithAgent;
import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.RandomPlayTester;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.CoverterDot;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
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
import onlineTestCaseGenerator.Sa1Solver.Policy;


/**
 * This is the same as the SA3, but after achieving the goal, we run Random algorithm to test the other element.
 * To do that, we need to change the Solver
 * @author Shirz002
 *
 */
public class Test_IdOrTypeWithRandom {

	boolean withGraphics = true;
	boolean supressLogging = false;
	Pair<Integer, Tile> covertToMDLocation(Vec3 p) {
		int mapNr = Math.round(p.y);
		Tile t = new Tile(Math.round(p.x), Math.round(p.z));
		return new Pair<Integer, Tile>(mapNr, t);
	}

	@Test
	public void test0() throws Exception {
		Pair targetItemOrShrine = new Pair("id", "H0_3");
		Pair additionalFeature = new Pair("maze", 0);
		int seed  = 79371;
		testFullPlay("Frodo", targetItemOrShrine, additionalFeature, seed);
	}

	GoalStructure exhaustiveExplore(TestAgent agent, GoalLib goalLib, TacticLib tacticLib) {
		return WHILE(S -> !tacticLib.explorationExhausted(S), goalLib.smartExploring(agent, null, Integer.MAX_VALUE));
	}

	List<String> getConnectedEnablersFromBelief(String shrine, MyAgentState S) {
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
	static boolean isImmortalShrine(WorldEntity e) {		
		var shTy = (ShrineType) e.properties.get("shrinetype") ;
		return shTy == ShrineType.ShrineOfImmortals ;
	}
	public void testFullPlay(String agentId, Pair targetItemOrShrine, Pair additionalFeature, int seed) throws Exception {
		// Create an instance of the game, attach an environment to it:
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.numberOfMaze = 2;
		config.randomSeed = seed;
		config.enableSmeagol = false;
		// change the Ferado maxBagSize to 1, it is in the Entity.java should make it
		// here
		System.out.println(">>> Configuration:\n" + config);

		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics;
		if (withGraphics)
			DungeonApp.deploy(app);

		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentStateExtended();
		Specifications specifications = new Specifications();

		var goalLib = new GoalLib();
		var tacticLib = new TacticLib();
		//Pair targetItemOrShrine = new Pair("id", "H0_3");
		// create an agent:
		var agent = new TestAgent(agentId, "tester");

		// should be after create the agent, else the constructor sets the visibility
		// again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}

		var sa3Solver = new Sa3Solver3<Void>((S, e) -> Utils.isReachable((MyAgentState) S, e),
				(S, e) -> Utils.distanceToAgent((MyAgentState) S, e),
				S -> (e1, e2) -> Utils.distanceBetweenEntities((MyAgentState) S, e1, e2),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId)),
				eId -> SEQ(goalLib.smartEntityInCloseRange(agent, eId), goalLib.entityInteracted(eId),
						goalLib.entityInteracted(eId)),
				S -> tacticLib.explorationExhausted(S), 
				dummy -> exhaustiveExplore(agent, goalLib, tacticLib));

		// Goal: find a shrine and cleanse it:
		// Pair targetItemOrShrine = new Pair("type","HEALPOT") ;
		
		
		var G = sa3Solver.solver(agent, targetItemOrShrine, additionalFeature, new Vec3(20, 1, 1),
				e -> e.type.equals("" + EntityType.SHRINE), e -> e.type.equals("" + EntityType.SCROLL),
				e -> e.type.equals("" + EntityType.SHRINE) && (boolean) e.properties.get("cleansed"),
				(shrine, S) -> getConnectedEnablersFromBelief(shrine, (MyAgentState) S),
				// we can't close a shrine again once it is cleaned, so this is not needed:
				(shrine, S) -> null, 
				S -> {
					var S_ = (MyAgentStateExtended) S;
					var player = S.worldmodel.elements.get(S.worldmodel.agentId);
					
					
					System.out.println("Checking the condition: phi ");
					List<WorldEntity> e = null;
					// if the agent is dead or win the game
					var shrine = S.worldmodel.elements.values().stream().filter( s ->
				  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
					
					if(!shrine.isEmpty()) {  
						  var immortalShrine = shrine.stream().filter(s -> isImmortalShrine(s) && (boolean) s.properties.get("cleansed")).findFirst();
						  if(!immortalShrine.isEmpty()) {
							  System.out.println("Immortal Shrine is cleansed!"); 
							  return false;
						  }
					}
					
					if( !S_.agentIsAlive()) {System.out.println("Player died!"); return false;}

					// if it is true, it does not get the item yet
					System.out.println("== The type of the item is given:" + targetItemOrShrine.snd);
					if (player.getPreviousState() != null) {
						var previousProperties = player.getPreviousState().properties;

						// To check the healing pot is used and health is increased
						if((targetItemOrShrine.fst.equals("type") && targetItemOrShrine.snd.equals(EntityType.HEALPOT) ) || (targetItemOrShrine.fst.equals("id") && targetItemOrShrine.snd.toString().contains("H")) ) {
							  var hpBefore = (int) previousProperties.get("hp"); 
							  var healpotsInBagBefore = (int) previousProperties.get("healpotsInBag"); 
							  var hp = (int)player.properties.get("hp");
							  var healpotsInBag = (int) player.properties.get("healpotsInBag"); 
							  
							  System.out.println("properties:" + hp
							  +"before: "+ hpBefore); System.out.println("properties:" + healpotsInBag
							  +"before: "+ healpotsInBagBefore);
							 
							  //if the id is given 						
							  //if the id is given 						
								if(targetItemOrShrine.fst.equals("id")) {
									var bagItems = previousProperties.get("itemsInBag").toString().contains(targetItemOrShrine.snd.toString());
									System.out.println("Lets check the data: " + bagItems + hpBefore + hp + healpotsInBagBefore + healpotsInBag);
									//the targeted id was used like heal or rage during survival
									var usedItems =  S_.triedItems.stream().filter(j-> j.id.equals(targetItemOrShrine.snd.toString())  && (boolean) j.properties.get("used")).collect(Collectors.toList());
									//S_.triedItems.forEach(es -> System.out.println("tried Items" + es.id));
									if(!usedItems.isEmpty() && !player.properties.get("itemsInBag").toString().contains(targetItemOrShrine.snd.toString())) {
										System.out.println("If it was selected and it is not anymore in the bag, it has used!");
										return false;
									}
									else if(bagItems && (hpBefore < hp) && healpotsInBagBefore > healpotsInBag) 
										  return false; 
									else
										return true;
								  } 
								//it is based on the type: there might be more than one 
							    if(additionalFeature != null) {	
								    var usedItems =  S_.triedItems.stream().filter(j-> j.type.equals(targetItemOrShrine.snd.toString())  && (boolean) j.properties.get("used")).collect(Collectors.toList());				    				    
								    System.out.println("used items " + usedItems );					    
									    for(WorldEntity s: usedItems) {						    	
							    			if( s.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd)) {
									    		return false;
								    		}
								    	}
								}
							
							if((hpBefore < hp) && healpotsInBagBefore > healpotsInBag) return false;
							 
					
						}else if((targetItemOrShrine.fst.equals("type") && targetItemOrShrine.snd.equals(EntityType.RAGEPOT) )  || (targetItemOrShrine.fst.equals("id") && targetItemOrShrine.snd.toString().contains("R"))) {
						 

							  // To check if the rage pot is used
							  //if the id is given 
							  var ragpotsInBagBefore = (int) previousProperties.get("ragepotsInBag");
							  var ragepotsInBag = (int) player.properties.get("ragepotsInBag");
							  
							  if(targetItemOrShrine.fst.equals("id")) { 
								  var bagItems = previousProperties.get("itemsInBag").toString().contains(targetItemOrShrine.snd.toString());	  
								  System.out.println("id is given " + ragepotsInBag +"before: "+
										  ragpotsInBagBefore + previousProperties.get("itemsInBag").toString());
								  
								//the targeted id was used like heal or rage during survival
									var usedItems =  S_.triedItems.stream().filter(j-> j.id.equals(targetItemOrShrine.snd.toString())  && (boolean) j.properties.get("used")).collect(Collectors.toList());
									//S_.triedItems.forEach(es -> System.out.println("tried Items" + es.id));
									if(!usedItems.isEmpty() && !player.properties.get("itemsInBag").toString().contains(targetItemOrShrine.snd.toString())) {
										System.out.println("If it was selected and it is not anymore in the bag, it has used!");
										return false;
									}
									else if(bagItems &&  ragpotsInBagBefore > ragepotsInBag) 
										  return false; 
									else
										return true;
								  
							  }  									  
							
							//it is based on the type: there might be more than one 
							    if(additionalFeature != null) {	
								    var usedItems =  S_.triedItems.stream().filter(j-> j.type.equals(targetItemOrShrine.snd.toString())  && (boolean) j.properties.get("used")).collect(Collectors.toList());				    				    
								    System.out.println("used items " + usedItems );					    
									    for(WorldEntity s: usedItems) {						    	
							    			if( s.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd)) {
									    		return false;
								    		}
								    	}
								}	
							  
							  System.out.println("agent properties:" + ragepotsInBag +"before: "+
							  ragpotsInBagBefore); 
							  if( ragpotsInBagBefore > ragepotsInBag) return false;
						  
						  
						}else {

						// To check if the shrine is clean and the scroll is used
	
						  var scrollsInBagBefore = previousProperties.get("scrollsInBag").hashCode();
						  var scrollsInBag = player.properties.get("scrollsInBag").hashCode();
						  System.out.println("Agent properties:" + scrollsInBag +"before: "+
						  scrollsInBagBefore );
						  if(targetItemOrShrine.fst.equals("id")) { 
							  var bagItems = previousProperties.get("itemsInBag").toString().contains(targetItemOrShrine.snd.toString());	  				 
							  
							//the targeted id was used like heal or rage during survival
								var usedItems =  S_.triedItems.stream().filter(j-> j.id.equals(targetItemOrShrine.snd.toString())  && (boolean) j.properties.get("used")).collect(Collectors.toList());
								//S_.triedItems.forEach(es -> System.out.println("tried Items" + es.id));
								if(!usedItems.isEmpty() && !player.properties.get("itemsInBag").toString().contains(targetItemOrShrine.snd.toString())) {
									System.out.println("If it was selected and it is not anymore in the bag, it has used!");
									return false;
								}
								else if(scrollsInBagBefore > scrollsInBag) 
									  return false; 
								else
									return true;
							  
						  }
						//it is based on the type: there might be more than one 
						    if(additionalFeature != null) {	
							    var usedItems =  S_.triedItems.stream().filter(j-> j.type.equals(targetItemOrShrine.snd.toString())  && (boolean) j.properties.get("used")).collect(Collectors.toList());				    				    
							    System.out.println("used items " + usedItems );					    
								    for(WorldEntity s: usedItems) {						    	
						    			if( s.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd)) {
								    		return false;
							    		}
							    	}
							}
						  //if we only want to check a scroll is used
						  
						  if(scrollsInBagBefore > scrollsInBag) { return false; }
						  
//						  var shrine = S.worldmodel.elements.values().stream().filter( s ->
//						  s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
//						  ; boolean clean = false; if(!shrine.isEmpty()) {
//						  System.out.println("Shrine is found!"); clean = (boolean)
//						  shrine.get(0).properties.get("cleansed");
//						  System.out.println("Shrine is cleansed!" + clean); } if(scrollsInBagBefore >
//						  scrollsInBag && clean) {
//						  System.out.println("Shrine is cleansed! and the bag is empty"); return false;
//						  } 
						}
					}

					return true;
				}, Policy.NEAREST_TO_TARGET);
		 //applying Random
		 RandomPlayTester randomplay = new RandomPlayTester() ;
		 var GWithRandom = SEQ(G, randomplay.randomPlay(agent)) ;
		
		agent.attachState(state).attachEnvironment(env).setGoal(GWithRandom);
		GameWorldModel model = new GameWorldModel(new GWState());
		// GameWorldModel model =
		// GameWorldModel.loadGameWorldModelFromFile("./tmp/modelMD1.json") ;

		ModelLearnerExtended modelLearner = new ModelLearnerExtended();
		agent.attachBehaviorModel(model, (S, m) -> {
			modelLearner.learn((MyAgentState) S, (GameWorldModel) m);
			return null;
		});

		var specs = new Specifications();
		var psi1 = specs.spec1();
		var psi2 = specs.spec2();
		var psi3 = specs.scenarioSpec1();
		var psi4 = specs.scenarioSpec2();
		var psi5 = specs.scenarioSpec3();
		agent.addLTL(psi1, psi2, psi3, psi4, psi5);
		agent.resetLTLs();

		Thread.sleep(1000);

		// why do we need this starting update?
		state.updateState(agentId);
		// Utils.printEntities(state);

		// Now we run the agent:
		int delay = 20;
		long time0 = System.currentTimeMillis();
		TestMiniDungeonWithAgent.runAndCheck(agent, GWithRandom, false, delay, 3000);

		System.out.println("=== exploration exhausted: " + tacticLib.explorationExhausted(state));

		Scanner scanner = new Scanner(System.in);
		// scanner.nextLine() ;
		// assertTrue(G.getStatus().success()) ;

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

		/* Check the specifications */
		var ok = agent.evaluateLTLs();

		System.out.println(">>>> LTL results: " + ok);
		System.out.println(">>>> psi1 : " + psi1.sat());
		System.out.println(">>>> psi2 : " + psi2.sat());
		System.out.println(">>>> psi3 : " + psi3.sat());
		System.out.println(">>>> psi4 : " + psi4.sat());
		System.out.println(">>>> psi5 : " + psi5.sat());
		
		//return model;
	}

}
