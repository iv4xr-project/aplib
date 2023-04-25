package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;


import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.Sa3Solver3;
import eu.iv4xr.framework.goalsAndTactics.Sa1Solver.Policy;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.GoalLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentStateExtended;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Specifications;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.TacticLib;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static nl.uu.cs.aplib.AplibEDSL.WHILE;

import java.io.IOException;
import java.util.*;

public class RunAllSpecifications {


	static boolean withGraphics = true;
	static boolean supressLogging = false;
	Pair<Integer, Tile> covertToMDLocation(Vec3 p) {
		int mapNr = Math.round(p.y);
		Tile t = new Tile(Math.round(p.x), Math.round(p.z));
		return new Pair<Integer, Tile>(mapNr, t);
	}


	static GoalStructure exhaustiveExplore(TestAgent agent, GoalLib goalLib, TacticLib tacticLib) {
		return WHILE(S -> !tacticLib.explorationExhausted(S), goalLib.smartExploring(agent, null, Integer.MAX_VALUE));
	}


	public static void main(String[] args) throws Exception{
	
		// Create an instance of the game, attach an environment to it:
				MiniDungeonConfig config = new MiniDungeonConfig();
				config.numberOfHealPots = 4;
				config.viewDistance = 4;
				config.numberOfMaze = 1;
				config.randomSeed = 79371;
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
				String agentId = "Frodo";
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
						S -> tacticLib.explorationExhausted(S), dummy -> exhaustiveExplore(agent, goalLib, tacticLib));
	
		
		//setting up all the goals
	Map<Pair<String,String>, Predicate<Iv4xrAgentState>> ScenarioSpecifications= new HashMap<Pair<String,String>, Predicate<Iv4xrAgentState>>();		
		
				
		//A specific healpot is selected and used
//		ScenarioSpecifications.put(new Pair("id", "H0_0"), 				
//				S -> {
//					var S_ = (MyAgentStateExtended) S;
//					var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
//					System.out.println("Checking the condition: phi ");
//					List<WorldEntity> e = null;
//					System.out.println("== The type of the item is given:" + "H0_0");
//					if (player.getPreviousState() != null) {
//						var previousProperties = player.getPreviousState().properties;
//						  //if the id is given 
//						var hpBefore = (int) previousProperties.get("hp"); 
//						var healpotsInBagBefore = (int) previousProperties.get("healpotsInBag"); 
//						var hp = (int) player.properties.get("hp"); 
//						var healpotsInBag = (int) player.properties.get("healpotsInBag");
//						
//						var bagItems = previousProperties.get("itemsInBag").toString().contains("H0_0");
//						System.out.println("Lets check the data: " +S_.selectedItem.properties.get("used") +S_.triedItems.toString()+ bagItems + hpBefore + hp + healpotsInBagBefore + healpotsInBag);
//						System.out.println("bag previous Items" + previousProperties.get("itemsInBag").toString() + player.properties.get("itemsInBag").toString());
//						//the target HP might be selected and used before because of the survival heuristic
//						if(S_.triedItems.contains("H0_0") &&  S_.worldmodel.getElement("H0_0") == null) {
//							System.out.println("If it was selected and it is not anymore in the bag, it has used!");
//							System.out.println("*******the goal is successfully acheived!!*****" + "H0_2");
//							return false;
//						}
//						if(bagItems && (hpBefore < hp) && healpotsInBagBefore >healpotsInBag) {
//							System.out.println("*******the goal is successfully acheived!!*****" + "H0_0");
//							return false;
//							}  
//					}
//					return true;		
//					}
//				);
		 
		
		//A specific healpot is selected and used
//		ScenarioSpecifications.put(new Pair("id", "H0_2"), 				
//				S -> {
//					var S_ = (MyAgentStateExtended) S;
//					var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
//					System.out.println("Checking the condition: phi ");
//					List<WorldEntity> e = null;
//					System.out.println("== The type of the item is given:" + EntityType.HEALPOT);
//					if (player.getPreviousState() != null) {
//						var previousProperties = player.getPreviousState().properties;
//						  //if the id is given 
//						var hpBefore = (int) previousProperties.get("hp"); 
//						var healpotsInBagBefore = (int) previousProperties.get("healpotsInBag"); 
//						var hp = (int) player.properties.get("hp"); 
//						var healpotsInBag = (int) player.properties.get("healpotsInBag");
//						
//						var bagItems = previousProperties.get("itemsInBag").toString().contains("H0_2");
//						System.out.println("Lets check the data: " +S_.selectedItem.properties.get("used") +S_.triedItems.toString()+ bagItems + hpBefore + hp + healpotsInBagBefore + healpotsInBag);
//						System.out.println("bag previous Items" + previousProperties.get("itemsInBag").toString() + player.properties.get("itemsInBag").toString());
//						//the target HP might be selected and used before because of the survival heuristic
//						if(S_.triedItems.contains("H0_2") &&  S_.worldmodel.getElement("H0_2") == null) {
//							System.out.println("If it was selected and it is not anymore in the bag, it has used!");
//							System.out.println("*******the goal is successfully acheived!!*****");
//							return false;
//						}
//						if(bagItems && (hpBefore < hp) && healpotsInBagBefore >healpotsInBag) {
//							System.out.println("*******the goal is successfully acheived!!*****" + "H0_2");
//							return false;  						
//						}
//					}
//					return true;		
//					}
//				);	
		
		
				//A  healpot is selected and used
		
				ScenarioSpecifications.put(new Pair("type", EntityType.HEALPOT), 				
					S -> {
						var S_ = (MyAgentStateExtended) S;
						var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
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
							//the target HP might be selected and used before because of the survival heuristic
							if((hpBefore < hp) && healpotsInBagBefore >healpotsInBag) {
								System.out.println("*******the goal is successfully acheived!!*****" + EntityType.HEALPOT);
								return false;  					
							}
						}
						return true;		
						}
					);
		
		
		
		
				// A specific rage pot is selected and used
//				ScenarioSpecifications.put(new Pair("id", "R0_1"), 				
//						S -> {
//							var S_ = (MyAgentStateExtended) S;
//							var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
//							System.out.println("Checking the condition: phi ");
//							List<WorldEntity> e = null;
//							System.out.println("== The type of the item is given:" + "R0_1");
//							if (player.getPreviousState() != null) {
//								  var previousProperties = player.getPreviousState().properties;
//								  //if the id is given 
//								  var bagItems = previousProperties.get("itemsInBag").toString().contains("R0_1");
//								  var ragpotsInBagBefore = (int) previousProperties.get("ragepotsInBag");
//								  var ragepotsInBag = (int) player.properties.get("ragepotsInBag");
//								  System.out.println("agent properties:" + ragepotsInBag +"before: "+ragpotsInBagBefore + "bagItems " + bagItems); 
//								  if( bagItems && ragpotsInBagBefore > ragepotsInBag) {
//									  System.out.println("*******the goal is successfully acheived!!*****" + "R0_1");
//									  return false; 
//								  }
//								 
//							}
//							return true;		
//							}
//						);
				
		
		
				// A rage pot is selected and used
				ScenarioSpecifications.put(new Pair("type", EntityType.RAGEPOT ), 				
						S -> {
							var S_ = (MyAgentStateExtended) S;
							var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
							System.out.println("Checking the condition: phi ");
							List<WorldEntity> e = null;
							System.out.println("== The type of the item is given:" + EntityType.RAGEPOT );
							if (player.getPreviousState() != null) {
								  var previousProperties = player.getPreviousState().properties;
								  //if the id is given   
								  var ragpotsInBagBefore = (int) previousProperties.get("ragepotsInBag");
								  var ragepotsInBag = (int) player.properties.get("ragepotsInBag");
								  System.out.println("agent properties:" + ragepotsInBag +"before: "+ragpotsInBagBefore); 
								  if( ragpotsInBagBefore > ragepotsInBag) {
									  System.out.println("*******the goal is successfully acheived!!*****" + EntityType.RAGEPOT);
									  return false; 
								  }
								 
							}
							return true;		
							}
						);
		
		
		
		
				// Check specific scroll is used, doe not matter if the shrine is cleansed
//				ScenarioSpecifications.put(new Pair("id", "S0_1"), 				
//						S -> {
//							var S_ = (MyAgentStateExtended) S;
//							var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
//							System.out.println("Checking the condition: phi ");
//							System.out.println("== The type of the item is given:" + "S0_1");
//							if (player.getPreviousState() != null) {
//								  var previousProperties = player.getPreviousState().properties;
//								  var scrollsInBagBefore = previousProperties.get("scrollsInBag").hashCode();
//								  var scrollsInBag = player.properties.get("scrollsInBag").hashCode();
//								  System.out.println("Agent properties:" + scrollsInBag +"before: "+ scrollsInBagBefore );
//								  
//								  //if we only want to check a scroll is used
//								  var bagItems = previousProperties.get("itemsInBag").toString().contains("S0_1");
//								  if(bagItems && scrollsInBagBefore > scrollsInBag) { 
//									  System.out.println("*******the goal is successfully acheived!!*****" + "S0_1");
//									  return false; }  
//							}
//							return true;		
//							}
//						);	
		
		// Check if a scroll is picked up and used, does not matter if it is not cleanse the shrine
				ScenarioSpecifications.put(new Pair("type", EntityType.SCROLL), 				
						S -> {
							var S_ = (MyAgentStateExtended) S;
							var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
							System.out.println("Checking the condition: phi ");
							System.out.println("== The type of the item is given:" + EntityType.SCROLL);
							if (player.getPreviousState() != null) {
								  var previousProperties = player.getPreviousState().properties;
								  var scrollsInBagBefore = previousProperties.get("scrollsInBag").hashCode();
								  var scrollsInBag = player.properties.get("scrollsInBag").hashCode();
								  System.out.println("Agent properties:" + scrollsInBag +"before: "+
								  scrollsInBagBefore );
								  
								  //if we only want to check a scroll is used
								  
								  if(scrollsInBagBefore > scrollsInBag) { 
									  System.out.println("*******the goal is successfully acheived!!*****" + EntityType.SCROLL);
									  return false; }
								  								  
							}
							return true;		
							}
						);	
		
		
		
				// Check a type scroll is used and the shrine is cleanse, it will continue selecting scroll until 
				// the shrine is cleanse. 
				//this is the same as cleansing shrine
//				ScenarioSpecifications.put(new Pair("type", EntityType.SCROLL), 				
//						S -> {
//							var S_ = (MyAgentStateExtended) S;
//							var player = S_.worldmodel.elements.get(S_.worldmodel.agentId);
//							System.out.println("Checking the condition: phi ");
//							System.out.println("== The type of the item is given:" + EntityType.SCROLL);
//							if (player.getPreviousState() != null) {
//								  var previousProperties = player.getPreviousState().properties;
//								  var scrollsInBagBefore = previousProperties.get("scrollsInBag").hashCode();
//								  var scrollsInBag = player.properties.get("scrollsInBag").hashCode();
//								  System.out.println("Agent properties:" + scrollsInBag +"before: "+
//								  scrollsInBagBefore );
//								  
//								  var shrine = S.worldmodel.elements.values().stream().filter( s ->
//								  s.type.contains(EntityType.SHRINE.toString()) ).collect(Collectors.toList());
//								  boolean clean = false; if(!shrine.isEmpty()) {
//									  System.out.println("Shrine is found!"); 
//									  clean = (boolean) shrine.get(0).properties.get("cleansed");
//									  System.out.println("Shrine is cleansed!" + clean); 
//									  } 
//								  if(scrollsInBagBefore > scrollsInBag && clean) {
//									  System.out.println("Shrine is cleansed! and the bag is empty"); 
//									  System.out.println("*******the goal is successfully acheived!!*****" + EntityType.SCROLL);
//									  return false;
//								  }
//							}
//							return true;		
//							}
//						);	
				
				
		
		
		//call specifications 
		Test_AllSpecifications runAll = new Test_AllSpecifications();
	//	runAll.testFullPlay(agentId,agent,ScenarioSpecifications,sa3Solver,state,env);
		
		for (Map.Entry<Pair<String,String>, Predicate<Iv4xrAgentState>> goal : ScenarioSpecifications.entrySet()) {
			Pair targetItemOrShrine1  = goal.getKey();
			Predicate<Iv4xrAgentState> sp = goal.getValue();	
			runAll.testFullPlay(agentId,agent,targetItemOrShrine1, sp,sa3Solver,state,env);
			
		}
		
		}
	}
