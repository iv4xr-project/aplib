package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonIOException;

import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.LTL;
import static eu.iv4xr.framework.extensions.ltl.LTL.* ;

import eu.iv4xr.framework.mainConcepts.WorldEntity;
import nl.uu.cs.aplib.utils.Pair;

/**
 * Provides the semantic-function (alpha) for interactions, to be used to construct a model
 * for the Lab Recruits game using {@link GameWorldModel}.
 * 
 * <p>See <a href="https://github.com/iv4xr-project/labrecruits">the Lab Recruits</a> game,
 * and <a href="https://github.com/iv4xr-project/iv4xrDemo">its Java-interface</a>.
 */
public class LabRecruitsModel {
	
	static public final String SWITCH = "Switch" ;
	static public final String DOOR = "Door" ;
	
	public static GWObject mkButton(String id) {
		GWObject button = new GWObject(id,SWITCH) ;
		return button ;
	}
	
	public static GWObject mkClosedDoor(String id) {
		GWObject door = new GWObject(id,DOOR) ;
		door.properties.put(GameWorldModel.IS_OPEN_NAME,false) ;
		return door ;
	}
	
	public static GWObject mkOpenDoor(String id) {
		GWObject door = new GWObject(id,DOOR) ;
		door.properties.put(GameWorldModel.IS_OPEN_NAME,true) ;
		return door ;
	}
	
	public static void alphaFunction(String button, Set<String>  affectedDoors, GWState S) {
		if (affectedDoors == null || affectedDoors.isEmpty()) return ;
		GWObject B = S.objects.get(button) ;
		Set<GWObject> doors = affectedDoors.stream().map(id -> S.objects.get(id)).collect(Collectors.toSet()) ;
 		for (var door : doors) {
 			//System.out.println(">>> door " + door.id) ;
			boolean doorState = (Boolean) door.properties.get(GameWorldModel.IS_OPEN_NAME) ;
			door.properties.put(GameWorldModel.IS_OPEN_NAME, !doorState) ; 
		}
	}
	
	/**
	 * Attach default alpha-component typical for LabRecruits models.
	 */
	public static GameWorldModel attachLabRecruitsAlpha(GameWorldModel model) {
		model.alpha = (button,affected) -> S -> { alphaFunction(button,affected,S); return null ; }  ;
		return model ;
	}
	
	/**
	 * Construct a model of the Lab Recruits' ButtonDoor1 level.
	 */
	public static GameWorldModel mk_ButtonDoor1Level() {
		// create the objects:
		GWObject button1 = mkButton("button1") ;
		GWObject button2 = mkButton("button2") ;
		GWObject button3 = mkButton("button3") ;
		GWObject button4 = mkButton("button4") ;
		GWObject door1 = mkClosedDoor("door1") ;
		GWObject door2 = mkClosedDoor("door2") ;
		GWObject door3 = mkClosedDoor("door3") ;
		// create an initial state, adding those objects to it:
		GWState initialstate = new GWState() ;
		initialstate.addObjects(
				button1,button2,button3,button4,
				door1,door2,door3);
		// set the agent's initial location:
		initialstate.currentAgentLocation = button2.id ;
		
		// create the zones:
		GWZone zoneMain = new GWZone("Main") ;
		GWZone zoneB3 = new GWZone("ThiefCatcher") ;
		GWZone zoneB4 = new GWZone("Closet") ;
		GWZone zoneTreasure = new GWZone("Treasure") ;
		// populate the zones:
		zoneMain.addMembers(button1.id, button2.id, door1.id, door3.id);
		zoneB3.addMembers(button3.id, door1.id, door2.id);
		zoneB4.addMembers(button4.id, door2.id);
		zoneTreasure.addMembers(door3.id);
		
		// create the model, add zones to it, etc:
		GameWorldModel buttondoor1 = new GameWorldModel(initialstate) 
		   . addZones(zoneMain, zoneB3, zoneB4, zoneTreasure)
		   . markAsBlockers(door1.id, door2.id, door3.id)
		// objects-connection:
		   . registerObjectLinks(button1.id, door1.id)
		   . registerObjectLinks(button3.id, door1.id, door2.id, door3.id)
		   . registerObjectLinks(button4.id, door1.id);
		// set alpha-function:
		buttondoor1 = attachLabRecruitsAlpha(buttondoor1) ;
		buttondoor1.name = "ButtonDoors_1" ;
		
		return buttondoor1 ;
	}
	
	/**
	 * Construct a model of a prison-like LabRecruits level.
	 */
	public static GameWorldModel mk_PrisonLevel(int numberOfPrisonGroups, int numberOfCellsPerGroup) {
		
		GameWorldModel model = new GameWorldModel() ;
		GWState initialstate = new GWState() ;
		model.initialState = initialstate ;
		Pair<GWState,GWTransition> st0 = new Pair<>(initialstate, null) ;
		model.history.add(st0) ;
		
		GWObject connectingDoor = null ;
		// create the prison-groups:
		for (int g=0; g<numberOfPrisonGroups; g++) {
			GWZone zone = new GWZone("Prison_" + g) ;
			model.addZones(zone) ;
			// connect to previous group:
			if (g>0) {
				GWObject openerSouth = mkButton("bS_" + g) ;
				initialstate.addObjects(openerSouth);
				zone.addMembers(connectingDoor.id,openerSouth.id);
				model.registerObjectLinks(openerSouth.id, connectingDoor.id) ;
			}
			// connection to next group:
			if (g < numberOfPrisonGroups-1) {
				GWObject openerNorth = mkButton("bN_" + g) ;
				connectingDoor = mkClosedDoor("D_" + g) ;
				initialstate.addObjects(openerNorth,connectingDoor);
				zone.addMembers(openerNorth.id,connectingDoor.id);
				model.markAsBlockers(connectingDoor.id) ;
				model.registerObjectLinks(openerNorth.id, connectingDoor.id) ;
			}
			// create the cells:
			for (int c=0; c<numberOfCellsPerGroup; c++) {
				GWZone cell = new GWZone("C_" + g + "_" + c) ;
				model.addZones(cell) ;
				GWObject button = mkButton("bc_" + g + "_" + c) ;
				GWObject door   = mkClosedDoor("dc_" + g + "_" + c) ;
				initialstate.addObjects(button,door);
				zone.addMembers(button.id,door.id);
				cell.addMembers(door.id);
				model.markAsBlockers(door.id) ;
				model.registerObjectLinks(button.id, door.id) ;
				if (g==0 && c==0)
					initialstate.setAsCurrentLocation(button);
			}
			
		}
		
		model.defaultInitialState = initialstate ;
		model.setInitialState(initialstate);
		
		attachLabRecruitsAlpha(model) ;
		model.name = "Prison" + numberOfPrisonGroups + "_" + numberOfCellsPerGroup ;
		return model ;
	}
	
	// just for test:
	static public void main(String[] args) throws JsonIOException, IOException {
		
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
		
	    System.out.println(buttondoor1.toString()) ;
		
	    buttondoor1.reset();
		
		var mc = new BuchiModelChecker(buttondoor1) ;
		LTL<IExplorableState> solved = eventually(S -> {
			GWState st = (GWState) S ;
			boolean door3isOpen = (Boolean) st.objects.get("door3").properties.get(GameWorldModel.IS_OPEN_NAME) ;
			return st.currentAgentLocation.equals("door3") && door3isOpen ;
		});
		var sequence = mc.findShortest(solved,13) ;
		System.out.println(">>> solution: " + sequence.path.size()) ;
		int k = 0 ;
		for (var step : sequence.path) {
			System.out.print(">> step " + k + ":") ;
			GWTransition tr = (GWTransition) step.fst ;
			if (tr!=null) System.out.print("" + tr.getId()) ;
			System.out.println("") ;
			k++ ;	
		}
		//System.out.println(">>> count=" + buttondoor1.count) ;
		System.out.println(buttondoor1.toString()) ;

	}
	

}
