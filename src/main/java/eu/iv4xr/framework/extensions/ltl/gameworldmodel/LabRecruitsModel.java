package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.Set;

import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.LTL;
import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

import eu.iv4xr.framework.mainConcepts.WorldEntity;

public class LabRecruitsModel {
	
	static String SWITCH = "switch" ;
	static String DOOR = "door" ;
	
	public static WorldEntity mkButton(String id) {
		WorldEntity button = new WorldEntity(id,SWITCH,true) ;
		return button ;
	}
	
	public static WorldEntity mkClosedDoor(String id) {
		WorldEntity door = new WorldEntity(id,DOOR,true) ;
		door.properties.put(GameWorldModel.IS_OPEN_NAME,false) ;
		return door ;
	}
	
	public static void alphaFunction(WorldEntity button, Set<WorldEntity>  affectedDoors) {
		for (var door : affectedDoors) {
			boolean doorState = (Boolean) door.properties.get(GameWorldModel.IS_OPEN_NAME) ;
			door.properties.put(GameWorldModel.IS_OPEN_NAME, !doorState) ; 
		}
	}
	
	public static GameWorldModel mk_ButtonDoor1Level() {
		// create the objects:
		WorldEntity button1 = mkButton("button1") ;
		WorldEntity button2 = mkButton("button2") ;
		WorldEntity button3 = mkButton("button3") ;
		WorldEntity button4 = mkButton("button4") ;
		WorldEntity door1 = mkClosedDoor("door1") ;
		WorldEntity door2 = mkClosedDoor("door2") ;
		WorldEntity door3 = mkClosedDoor("door3") ;
		// create an initial state, adding those objects to it:
		GWState initialstate = new GWState() ;
		initialstate.addObjects(
				button1,button2,button3,button4,
				door1,door2,door3);
		// set the agent's initial location:
		initialstate.currentAgentLocation = button2.id ;
		
		// create the zones:
		GWZone zoneMain = new GWZone("zmain") ;
		GWZone zoneB3 = new GWZone("zb3") ;
		GWZone zoneB4 = new GWZone("zb4") ;
		GWZone zoneTreasure = new GWZone("ztreasure") ;
		// populate the zones:
		zoneMain.addMembers(button1.id, button2.id, door1.id, door3.id);
		zoneB3.addMembers(button3.id, door1.id, door2.id);
		zoneB4.addMembers(button4.id, door2.id);
		zoneTreasure.addMembers(door3.id);
		
		// create the model, add zones to it, etc:
		GameWorldModel buttondoor1 = new GameWorldModel(initialstate) ;
		buttondoor1.addZones(zoneMain, zoneB3, zoneB4, zoneTreasure);
		buttondoor1.markAsBlockers(door1.id, door2.id, door3.id);
		// objects-connection:
		buttondoor1.registerObjectLinks(button1.id, door1.id);
		buttondoor1.registerObjectLinks(button3.id, door1.id, door2.id, door3.id);
		buttondoor1.registerObjectLinks(button4.id, door1.id);
		// set alpha-function:
		buttondoor1.alpha = (button -> (affected -> { alphaFunction(button,affected); return null ; } )) ;

		return buttondoor1 ;
	}
	
	static public void main(String[] args) {
		
		var buttondoor1 = mk_ButtonDoor1Level() ;
		buttondoor1.travelTo("button1");
		//System.out.println(buttondoor1.toString()) ;
		buttondoor1.interact("button1");
		buttondoor1.travelTo("door1");
		buttondoor1.travelTo("button3");
		buttondoor1.interact("button3");
		buttondoor1.travelTo("door2");
		buttondoor1.travelTo("button4");
		buttondoor1.interact("button4");
		buttondoor1.travelTo("door2");
		buttondoor1.travelTo("door1");
		buttondoor1.travelTo("door3");
	
		//System.out.println(buttondoor1.toString()) ;
		
		buttondoor1 = mk_ButtonDoor1Level() ;
		var mc = new BuchiModelChecker(buttondoor1) ;
		LTL<IExplorableState> solved = eventually(S -> {
			GWState st = (GWState) S ;
			boolean door3isOpen = (Boolean) st.objects.get("door3").properties.get(GameWorldModel.IS_OPEN_NAME) ;
			return st.currentAgentLocation.equals("door3") && door3isOpen ;
		});
		var sequence = mc.find(solved,13) ;
		System.out.println(">>> solution: " + sequence.path.size()) ;
		int k = 0 ;
		for (var step : sequence.path) {
			System.out.print(">> step " + k + ":") ;
			GWTransition tr = (GWTransition) step.fst ;
			if (tr!=null) System.out.print("" + tr.getId()) ;
			System.out.println("") ;
			k++ ;	
		}
		System.out.println(">>> count=" + buttondoor1.count) ;
		System.out.println(buttondoor1.toString()) ;
	}
	

}
