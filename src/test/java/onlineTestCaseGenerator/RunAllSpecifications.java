package onlineTestCaseGenerator;


import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TestMiniDungeonWithAgent;
import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.MD_invs;
import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.RandomPlayTester;
import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.SATVerdict;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLibExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentStateExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Specifications;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.SimpleState;
import nl.uu.cs.aplib.utils.Pair;
import onlineTestCaseGenerator.Sa3Solver3;
import onlineTestCaseGenerator.Sa1Solver.Policy;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static nl.uu.cs.aplib.AplibEDSL.WHILE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.*;

public class RunAllSpecifications {


	static boolean withGraphics = false;
	static boolean supressLogging = false;
	Pair<Integer, Tile> covertToMDLocation(Vec3 p) {
		int mapNr = Math.round(p.y);
		Tile t = new Tile(Math.round(p.x), Math.round(p.z));
		return new Pair<Integer, Tile>(mapNr, t);
	}


	static GoalStructure exhaustiveExplore(TestAgent agent, GoalLib goalLib, TacticLib tacticLib) {
		return WHILE(S -> !tacticLib.explorationExhausted(S), goalLib.smartExploring(agent, null, Integer.MAX_VALUE));
	}

	
	@Test
	public void test0() throws Exception {
		boolean random = false;
		int seed = 79371;
		int maze = 0;
		Pair additionalFeature = null;
		testAll("Frodo", random, seed, maze, additionalFeature);
	}
	
	
	public Pair<Boolean, Long> testAll(String agentId,boolean random, int seed, int maze, Pair additionalFeature) throws Exception{
	
		// Create an instance of the game, attach an environment to it:
				MiniDungeonConfig config = new MiniDungeonConfig();
				config.numberOfHealPots = 4;
				config.viewDistance = 5;
				config.numberOfMaze = maze;
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
				var goalLibExtented  =  new GoalLibExtended();
				// create an agent:
				var agent = new TestAgent(agentId, "tester");

				// should be after create the agent, else the constructor sets the visibility
				// again
				if (supressLogging) {
					Logging.getAPLIBlogger().setLevel(Level.OFF);
				}

	
		
		//setting up all the goals
	Map<Pair<String,String>, Predicate<Iv4xrAgentState>> ScenarioSpecifications= new LinkedHashMap<Pair<String,String>, Predicate<Iv4xrAgentState>>();		
		
				
		//scenario where a heal pot, a rage pot, a scroll is used and the shrine is check to be cleansed	
//		ScenarioSpecifications.put(AHealPot(additionalFeature).fst,AHealPot(additionalFeature).snd);	
//		ScenarioSpecifications.put(ARagePot(additionalFeature).fst,ARagePot(additionalFeature).snd);	
//		ScenarioSpecifications.put(AScroll(additionalFeature).fst,AScroll(additionalFeature).snd);	
//		ScenarioSpecifications.put(cleansShrin().fst,cleansShrin().snd);
		
		//scenario where a rage pot, a scroll is used and shrine is cleansed
//		ScenarioSpecifications.put(AHealPot(additionalFeature).fst,AHealPot(additionalFeature).snd);	
		ScenarioSpecifications.put(ARagePot(additionalFeature).fst,ARagePot(additionalFeature).snd);	
		ScenarioSpecifications.put(AScroll(additionalFeature).fst,AScroll(additionalFeature).snd);	
		ScenarioSpecifications.put(cleansShrin().fst,cleansShrin().snd);
		
		
		var specs = new Specifications();
		var psi1 = specs.spec1();
		var psi2 = specs.spec2();
		var psi3 = specs.spec3();
		var psi4 = specs.spec4();
		var psi5 = specs.spec5();
		var psiSp1 = specs.scenarioSpec1();
		var psiSp2 = specs.scenarioSpec2();
		var psiSp3 = specs.scenarioSpec3();
		var psiSp4 = specs.scenarioSpec4();
		var psiSp5 = specs.scenarioSpec5();
		var psiSp6 = specs.scenarioSpec6();
		agent.addLTL(psi1, psi2, psi3, psi4, psi5, psiSp1, psiSp2, psiSp3, psiSp4, psiSp5, psiSp6);
		
		var invs = new MD_invs() ;
		List<LTL<SimpleState>> invs2 = new LinkedList<>() ;
		for (var i : invs.allInvs) {
			var ltl = LTL.always(i) ;
			invs2.add(ltl) ;
			agent.addLTL(ltl) ;
		}
		
		agent.resetLTLs();
		//call specifications 
		Test_AllSpecifications runAll = new Test_AllSpecifications();
	    //runAll.testFullPlay(agentId,agent,ScenarioSpecifications,sa3Solver,state,env);
		long time0 = System.currentTimeMillis();
		Pair<Boolean, Long> result = null;
		for (Map.Entry<Pair<String,String>, Predicate<Iv4xrAgentState>> goal : ScenarioSpecifications.entrySet()) {
			Pair targetItemOrShrine1  = goal.getKey();
			Predicate<Iv4xrAgentState> sp = goal.getValue();	
			System.out.println("which goal to call :: " + targetItemOrShrine1);
			result  = runAll.testFullPlay(agentId,agent,targetItemOrShrine1,additionalFeature, sp,state,env);	
		}
		
		// Run random algorithm after finishing the sequence of specification
		if(random) runRandom(agentId, agent, state, env);
		
		var totalTime  = (System.currentTimeMillis() - time0) /1000;
		System.out.println("total time :: " + totalTime);
		

//
//		
		/* Check the specifications */
		var ok = agent.evaluateLTLs();
		System.out.println(">>>> LTL results: ttttttt ");
		System.out.println(">>>> LTL results: " + ok);
		System.out.println(">>>> psi1 : " + psi1.sat());
		System.out.println(">>>> psi2 : " + psi2.sat());
		System.out.println(">>>> psi3 : " + psi3.sat());
		System.out.println(">>>> psi4 : " + psi4.sat());
		System.out.println(">>>> psi5 : " + psi5.sat());
		System.out.println(">>>> psi6 : " + psiSp1.sat());
		System.out.println(">>>> psi7 : " + psiSp2.sat());
		System.out.println(">>>> psi8 : " + psiSp3.sat());
		System.out.println(">>>> psi9 : " + psiSp4.sat());
		System.out.println(">>>> psi9 : " + psiSp5.sat());
		System.out.println(">>>> psi9 : " + psiSp6.sat());
		assertTrue(psi1.sat() == SATVerdict.SAT	
				&& psi2.sat() == SATVerdict.SAT
				&& psi3.sat() == SATVerdict.SAT
				&& psi4.sat() == SATVerdict.SAT
				&& psiSp1.sat() == SATVerdict.SAT
				&& psiSp2.sat() == SATVerdict.SAT
				&& psiSp3.sat() == SATVerdict.SAT
				) ;
		
		for(var inv: invs2) {
			System.out.println(">>>> Inv : " + inv.sat());
			assertTrue(inv.sat() == SATVerdict.SAT	);
		}
		
		return result;
		
		}
	
	
	
	/**
	 * Random algorithm 
	 * @param agentId
	 * @param agent
	 * @param state
	 * @param env
	 * @throws Exception
	 */
	
	public static void runRandom(String agentId, TestAgent agent, MyAgentState state,MyAgentEnv env) throws Exception {
		
		    //Add Random algorithm 
		    RandomPlayTester randomplay = new RandomPlayTester() ;
		    var G = randomplay.randomPlay(agent);
		    agent.attachState(state).attachEnvironment(env).setGoal(G);
		    Thread.sleep(1000);

			// why do we need this starting update?
			state.updateState(agentId);
			// Utils.printEntities(state);

			// Now we run the agent:
			int delay = 20;
			long time0 = System.currentTimeMillis();
			TestMiniDungeonWithAgent.runAndCheck(agent, G, false, delay, 3000);
	}
	

	
	/**
	 * Check a type scroll is used and the shrine is cleanse, it will continue selecting scroll until 
		the shrine is cleanse. 
	 	this is the same as cleansing shrine
	 * @return
	 */
	
	public Pair<Pair<String,String>,Predicate<Iv4xrAgentState>>  cleansShrin(){
		Pair<String,String> phi  = new Pair("type", EntityType.SCROLL.toString()); 				
		Predicate<Iv4xrAgentState> psi = 		S -> {
					var S_ = (MyAgentStateExtended) S;
					var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
					System.out.println("Checking the condition: phi cleaning the shrine" );
					System.out.println("== The type of the item is given: shrine" + phi.snd);
					var shrine = S.worldmodel.elements.values().stream().filter( s ->
				  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
					if(!shrine.isEmpty()) {  
						  var immortalShrine = shrine.stream().filter(s -> isImmortalShrine(s) && (boolean) s.properties.get("cleansed")).findFirst();
						  if(!immortalShrine.isEmpty()) {
							  System.out.println("Immortal Shrine is cleansed!"); 
							  S_.triedItems.clear();					  
							  return false;
						  }
					}
					
					if( !S_.agentIsAlive()) {System.out.println("Player died!"); return false;}
					if (player.getPreviousState() != null) {
						  var previousProperties = player.getPreviousState().properties;
						  var scrollsInBagBefore = previousProperties.get("scrollsInBag").hashCode();
						  var scrollsInBag = player.properties.get("scrollsInBag").hashCode();
						  
						  boolean clean = false; if(!shrine.isEmpty()) {
							  System.out.println("Shrine is found!"); 
							  clean = (boolean) shrine.get(0).properties.get("cleansed");
							  System.out.println("Shrine is cleansed!" + clean); 
							  } 
						  if(scrollsInBagBefore > scrollsInBag && clean) {
							  System.out.println("Shrine is cleansed! and the bag is empty"); 
							  System.out.println("*******the goal is successfully acheived!!***** Shine is cleansed" );												
							  return false;
						  }
					}
					return true;		
					}
			;
			Pair<Pair<String,String>,Predicate<Iv4xrAgentState>> goal = new Pair(phi,psi);
		return goal;
	}
	
	
	
	/**
	 *  Check if a scroll is picked up and used, does not matter if it is not cleanse the shrine
	 */

	public Pair<Pair<String,String>,Predicate<Iv4xrAgentState>>  AScroll(Pair additionalFeature){
		Pair<String,String> phi = new Pair("type", EntityType.SCROLL.toString());				
		Predicate<Iv4xrAgentState> psi = S -> {
		var S_ = (MyAgentStateExtended) S;
		var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
		System.out.println("Checking the condition: phi ");
		System.out.println("== The type of the item is given:" + phi.snd + "this is a scroll");
		var shrine = S.worldmodel.elements.values().stream().filter( s ->
	  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
		if(!shrine.isEmpty()) {  
			  var immortalShrine = shrine.stream().filter(s -> isImmortalShrine(s) && (boolean) s.properties.get("cleansed")).findFirst();
			  if(!immortalShrine.isEmpty()) {
				  System.out.println("Immortal Shrine is cleansed!");
				  S_.triedItems.clear();
				  return false;
			  }
		}
		
		if( !S_.agentIsAlive()) {System.out.println("Player died!"); return false;}
		if (player.getPreviousState() != null) {
			  var previousProperties = player.getPreviousState().properties;
			  var scrollsInBagBefore = previousProperties.get("scrollsInBag").hashCode();
			  var scrollsInBag = player.properties.get("scrollsInBag").hashCode();
			  System.out.println("Agent properties:" + scrollsInBag +"before: "+
			  scrollsInBagBefore );
			  
			  //if we only want to check a scroll is used
			  if(additionalFeature != null) {	
				    var usedItems =  S_.triedItems.stream().filter(j-> j.type.equals(phi.snd)  && (boolean) j.properties.get("used") && j.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd)).collect(Collectors.toList());				    				    
				    if(!usedItems.isEmpty() && scrollsInBagBefore > scrollsInBag) {
						System.out.println("*******the goal is successfully acheived!!*****" + phi.snd);						
						return false;  					
					}
				}
			  else if(scrollsInBagBefore > scrollsInBag) { 
					  System.out.println("*******the goal is successfully acheived!!*****" + EntityType.SCROLL);					  
					  return false; 
				  }
			  								  
		}
		return true;		
		};
			
		Pair<Pair<String,String>,Predicate<Iv4xrAgentState>> goal = new Pair(phi,psi);
		return goal;
	}
	

	
	/**
	 * A rage pot is selected and used
	 * @return
	 */
	public Pair<Pair<String,String>,Predicate<Iv4xrAgentState>>  ARagePot(Pair additionalFeature){
		Pair<String,String> phi = new Pair("type", EntityType.RAGEPOT.toString() );				
		Predicate<Iv4xrAgentState> psi =	S -> {
				var S_ = (MyAgentStateExtended) S;
				var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
				System.out.println("Checking the condition: phi ");
				List<WorldEntity> e = null;
				System.out.println("== The type of the item is given:" + phi.snd );
				if (player.getPreviousState() != null) {
					  var previousProperties = player.getPreviousState().properties;
					  //if the id is given   
					  var ragpotsInBagBefore = (int) previousProperties.get("ragepotsInBag");
					  var ragepotsInBag = (int) player.properties.get("ragepotsInBag");
					  System.out.println("agent properties:" + ragepotsInBag +"before: "+ragpotsInBagBefore); 
						if(additionalFeature != null) {	
						    var usedItems =  S_.triedItems.stream().filter(j-> j.type.equals(phi.snd)  && (boolean) j.properties.get("used") && j.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd)).collect(Collectors.toList());				    				    
						    if(!usedItems.isEmpty() && ragpotsInBagBefore > ragepotsInBag) {
								System.out.println("*******the goal is successfully acheived!!*****" + phi.snd);								
								return false;  					
							}
						}
					  else if( ragpotsInBagBefore > ragepotsInBag) {
						  System.out.println("*******the goal is successfully acheived!!*****" + phi.snd);
						  return false; 
					  }						 
				}
				return true;		
				};
	
		Pair<Pair<String,String>,Predicate<Iv4xrAgentState>> goal = new Pair(phi,psi);
		return goal;
	}


	
	
	
	/**
	 * A specific rage pot is selected and used
	 * @return
	 */
	public Pair<Pair<String,String>,Predicate<Iv4xrAgentState>>  ARagePotWithId(){
		Pair<String,String> phi = new Pair("id", "R0_1"); 				
		Predicate<Iv4xrAgentState> psi = S -> {
				var S_ = (MyAgentStateExtended) S;
				var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
				System.out.println("Checking the condition: phi ");
				var shrine = S.worldmodel.elements.values().stream().filter( s ->
			  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
				if(!shrine.isEmpty()) {  
					  var immortalShrine = shrine.stream().filter(s -> isImmortalShrine(s) && (boolean) s.properties.get("cleansed")).findFirst();
					  if(!immortalShrine.isEmpty()) {
						  System.out.println("Immortal Shrine is cleansed!"); 
						  S_.triedItems.clear();
						  return false;
					  }
				}
				
				if( !S_.agentIsAlive()) {System.out.println("Player died!"); return false;}
				List<WorldEntity> e = null;				
				if (player.getPreviousState() != null) {
					  var previousProperties = player.getPreviousState().properties;
					  //if the id is given 
					  var bagItems = previousProperties.get("itemsInBag").toString().contains(phi.snd.toString());
					  var ragpotsInBagBefore = (int) previousProperties.get("ragepotsInBag");
					  var ragepotsInBag = (int) player.properties.get("ragepotsInBag");
					  System.out.println("agent properties:" + ragepotsInBag +"before: "+ragpotsInBagBefore + "bagItems " + bagItems); 
					  if( bagItems && ragpotsInBagBefore > ragepotsInBag) {
						  System.out.println("*******the goal is successfully acheived!!*****" + phi.snd.toString());						 
						  return false; 
					  }
					 
				}
				return true;		
				};
	
		Pair<Pair<String,String>,Predicate<Iv4xrAgentState>> goal = new Pair(phi,psi);
		return goal;
	}
	
	static boolean isImmortalShrine(WorldEntity e) {		
		var shTy = (ShrineType) e.properties.get("shrinetype") ;
		return shTy == ShrineType.ShrineOfImmortals ;
	}
	
	/**
	 * A  healpot is selected and used
	 * @return
	 */
	public Pair<Pair<String,String>,Predicate<Iv4xrAgentState>>  AHealPot(Pair additionalFeature){
		Pair<String,String> phi = new Pair("type", EntityType.HEALPOT.toString()); 				
		Predicate<Iv4xrAgentState> psi = S -> {
			var S_ = (MyAgentStateExtended) S;
			var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
			System.out.println("Checking the condition: phi ");
			List<WorldEntity> e = null;
			System.out.println("== The type of the item is given:" + phi.snd);
			var shrine = S.worldmodel.elements.values().stream().filter( s ->
		  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
			if(!shrine.isEmpty()) {  
				  var immortalShrine = shrine.stream().filter(s -> isImmortalShrine(s) && (boolean) s.properties.get("cleansed")).findFirst();
				  if(!immortalShrine.isEmpty()) {
					  System.out.println("Immortal Shrine is cleansed!"); 
					  S_.triedItems.clear();
					  return false;
				  }
			}
			
			if( !S_.agentIsAlive()) {System.out.println("Player died!"); return false;}
			if (player.getPreviousState() != null) {
				var previousProperties = player.getPreviousState().properties;
				  //if the id is given 
				var hpBefore = (int) previousProperties.get("hp"); 
				var healpotsInBagBefore = (int) previousProperties.get("healpotsInBag"); 
				var hp = (int) player.properties.get("hp"); 
				var healpotsInBag = (int) player.properties.get("healpotsInBag");
				//the target HP might be selected and used before because of the survival heuristic
				System.out.println("== The type of the item is given:" + phi.snd  + "heal bag" + healpotsInBagBefore + "after "+ healpotsInBag);
				if(additionalFeature != null) {	
				    var usedItems =  S_.triedItems.stream().filter(j-> j.type.equals(phi.snd)  && (boolean) j.properties.get("used") && j.properties.get(additionalFeature.fst.toString()).equals(additionalFeature.snd)).collect(Collectors.toList());				    				    
				    System.out.println("== The type of the item is given:!!!!" + phi.snd +  usedItems.isEmpty() + usedItems.size() + usedItems );
				    S_.triedItems.forEach(es->{System.out.println("tried items:: " + es.id + "used:: " + es.properties.get("used"));});
				    if(!usedItems.isEmpty() && hpBefore < hp && healpotsInBagBefore >healpotsInBag) {
						System.out.println("*******the goal is successfully acheived!!*****" + phi.snd);						
						return false;  					
					} 
				}
				else if((hpBefore < hp) && healpotsInBagBefore >healpotsInBag) {
					System.out.println("*******the goal is successfully acheived!!*****" + EntityType.HEALPOT);					
					return false;  					
				}
				
			}
				return true;		
			};
	
		Pair<Pair<String,String>,Predicate<Iv4xrAgentState>> goal = new Pair(phi,psi);
		return goal;
	}
	
	
	/**
	 * A specific healpot is selected and used
	 * @return
	 */
	
	
	public Pair<Pair<String,String>,Predicate<Iv4xrAgentState>>  AHealPotWithId(){
		Pair<String,String> phi = new Pair("id", "H0_2"); 				
		Predicate<Iv4xrAgentState> psi = S -> {
				var S_ = (MyAgentStateExtended) S;
				var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
				var shrine = S.worldmodel.elements.values().stream().filter( s ->
			  	s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
				if(!shrine.isEmpty()) {  
					  var immortalShrine = shrine.stream().filter(s -> isImmortalShrine(s) && (boolean) s.properties.get("cleansed")).findFirst();
					  if(!immortalShrine.isEmpty()) {
						  System.out.println("Immortal Shrine is cleansed!"); 
						  S_.triedItems.clear();
						  return false;
					  }
				}
				
				if( !S_.agentIsAlive()) {System.out.println("Player died!"); return false;}
				System.out.println("Checking the condition: phi ");
				List<WorldEntity> e = null;
				System.out.println("== The type of the item is given:" + EntityType.HEALPOT);
				if (player.getPreviousState() != null) {
					var previousProperties = player.getPreviousState().properties;
					  //if the id is given 
					var hpBefore = (int) previousProperties.get("hp"); 
					var healpotsInBagBefore = (int) previousProperties.get("healpotsInBag"); 
					var hp = (int) player.properties.get("hp"); 
					var healpotsInBag = (int) player.properties.get("healpotsInBag");
					
					var bagItems = previousProperties.get("itemsInBag").toString().contains(phi.snd.toString());
					System.out.println("Lets check the data: " +S_.selectedItem.properties.get("used") +S_.triedItems.toString()+ bagItems + hpBefore + hp + healpotsInBagBefore + healpotsInBag);
					System.out.println("bag previous Items" + previousProperties.get("itemsInBag").toString() + player.properties.get("itemsInBag").toString());
					//the target HP might be selected and used before because of the survival heuristic
					if(S_.triedItems.contains(phi.snd) &&  S_.worldmodel.getElement(phi.snd) == null) {
						System.out.println("If it was selected and it is not anymore in the bag, it has used!");
						System.out.println("*******the goal is successfully acheived!!*****");						
						return false;
					}
					if(bagItems && (hpBefore < hp) && healpotsInBagBefore >healpotsInBag) {
						System.out.println("*******the goal is successfully acheived!!*****" + phi.snd.toString());					
						return false;  						
					}
				}
				return true;		
				};
	
		Pair<Pair<String,String>,Predicate<Iv4xrAgentState>> goal = new Pair(phi,psi);
		return goal;
	}

}
