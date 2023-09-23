package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWObject;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWZone;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import static nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provide a model learner for the MiniDungeon game to produce
 * a model in the form of an instance of {@link GameWorldModel}.
 * 
 * This learner can be attached to a test-agent.
 */
public class ModelLearner {
	
	String SCROLL = "" + EntityType.SCROLL ;
	String SHRINE = "" + EntityType.SHRINE ;
	String FRODO = "" + EntityType.FRODO ;
	String SMEAGOL = "" + EntityType.SMEAGOL ;
	
	
	boolean isInModel(WorldEntity e, GameWorldModel model) {
		return model.defaultInitialState.objects.get(e.id) != null ;
	}
	
	List<String> scrollsPreviouslyInBag = new LinkedList<>() ;
	
	
	boolean isFrodo(WorldEntity e) {
		return e.type.equals(FRODO) ;
	}
	
	boolean isSmeagol(WorldEntity e) {
		return e.type.equals(SMEAGOL) ;
	}
	
	boolean isScroll(WorldEntity e) {
		return e.type.equals(SCROLL) ;
	}
	
	boolean isShrine(WorldEntity e) {
		return e.type.equals(SHRINE) ;
	}
	
	boolean isMoonShrine(WorldEntity e) {
		if (!isShrine(e)) return false ;
		var shTy = (ShrineType) e.properties.get("shrinetype") ;
		return shTy == ShrineType.MoonShrine ;
	}
	
	boolean isSunShrine(WorldEntity e) {
		if (!isShrine(e)) return false ;
		var shTy = (ShrineType) e.properties.get("shrinetype") ;
		return shTy == ShrineType.SunShrine ;
	}
	
	boolean isImmortalShrine(WorldEntity e) {
		if (!isShrine(e)) return false ;
		var shTy = (ShrineType) e.properties.get("shrinetype") ;
		return shTy == ShrineType.ShrineOfImmortals ;
	}

	/**
	 * Read the current state of MiniDungeon, if to see if it contains new elements
	 * or relations that can be added to the given model. Invoke this method at
	 * every game-update to gradually learn a model of the game (or game level).
	 */
	public void learn(MyAgentState state, GameWorldModel model) {
		
		String scrollJustUsed = null ;
		// well cheating here ... since we get into the SUT state rather than via
		// WOM. For now we keep it like this for convenience:
		List<String> scrollsCurrentlyInBags = new LinkedList<>() ;
		for (Player P : state.env().app.dungeon.players) {
			scrollsCurrentlyInBags.addAll(
					P.bag.stream()
						.filter(i -> i.type == EntityType.SCROLL)
						.map(i -> i.id)
						.collect(Collectors.toList())) ;
		}
		for(String scroll : scrollsPreviouslyInBag) {
			if (! scrollsCurrentlyInBags.contains(scroll)) {
				scrollJustUsed = scroll ;
				break ;
			}
		}
		scrollsPreviouslyInBag = scrollsCurrentlyInBags ;
		
		
		for(WorldEntity e : state.worldmodel.elements.values()) {
			
			if (!isScroll(e) && !isShrine(e) && !isFrodo(e) && !isSmeagol(e)) continue ;
			
			// we will only put players, scrolls and shrines in the model.
				
			if (!isInModel(e,model)) {
				// e has not been registered before
				GWObject o = new GWObject(e.id,e.type) ;
				o.position = e.position ;
				// adding the entity to the model:
				model.defaultInitialState.addObjects(o);
				// adding needed properties to o:
				if (isScroll(e)) {
					// no property to add...
				}
				else if (isShrine(e)){
					// storing enum-type will later on be interpreted as string in json,
					// so let's store string then:
					o.properties.put("shrinetype", "" + e.properties.get("shrinetype")) ;
					if (isMoonShrine(e)) {
						// moon-shrine is a blocker, and add a block-property:
						model.blockers.add(o.id) ;
						o.properties.put(GameWorldModel.IS_OPEN_NAME,false) ;
					}
					if (isMoonShrine(e) || isImmortalShrine(e)) {
						o.properties.put("cleansed", e.properties.get("cleansed")) ;
					}
					else {
						// a sun-shrine, we make it a blocker because it need to connect the teleport
						// room to the next room, but it is always non-blocking:
						model.blockers.add(o.id) ;
						o.properties.put(GameWorldModel.IS_OPEN_NAME,true) ;
					}
					
				}
				else if (isSmeagol(e)) {
					o.position = null ;
					o.properties.put("bagslot1","xxx") ;
				}
				else if (isFrodo(e)){
					o.position = null ;
					o.properties.put("bagslot1","xxx") ;
					o.properties.put("bagslot2","xxx") ;
				}
				
				// update the zones as well:
				Integer mazeNr = (Integer) e.properties.get("maze") ;
				String mazeId = "" + mazeNr ;
				GWZone zone = model.getZone(mazeId) ;
				if (zone == null) {
					// so, we see a new maze, add it:
					zone = new GWZone(mazeId) ;
					model.addZones(zone) ;
					// adding some dummy object :
					if (mazeNr>0) {
						GWObject somewhere = new GWObject("SOMEWHERE_" + mazeNr, "DUMMY") ;
						somewhere.destroyed = true ;
						model.defaultInitialState.addObjects(somewhere) ;
						zone.members.add(somewhere.id) ;
					}				
				}
				// players are not put in any zone, but we create fake objects representing
				// their start location, and put it in the zone:
				if (isSmeagol(e)) {
					GWObject startLocation = new GWObject("STARTSmeagol","STARTLOC") ;
					startLocation.position = e.position ;
					startLocation.destroyed = true ;
					model.defaultInitialState.addObjects(startLocation) ;
					zone.addMembers(startLocation.id) ;
				}
				else if (isFrodo(e) ) {
					GWObject startLocation = new GWObject("STARTFrodo","STARTLOC") ;
					startLocation.position = e.position ;
					startLocation.destroyed = true ;
					model.defaultInitialState.addObjects(startLocation) ;
					zone.addMembers(startLocation.id) ;
				}
				else{
					// e is not a player, so it is a scroll or a shrine. Always add it to the zone:
					zone.addMembers(o.id);	
				}
				
				// special case for shrine, we add an artificial "teleport-room" zone
				// connecting moon and sun shrines:
				if (isMoonShrine(e)) {
					// a moon-shrine seen for the first time, add the corresponding teleport-room:
					int nextMaze = mazeNr + 1 ;
					var teleportRoom = new GWZone("Teleport_" + mazeNr + "_" + nextMaze) ;
					model.addZones(teleportRoom) ;
					teleportRoom.addMembers(o.id);
				}
				else if (isSunShrine(e)) {
					// a sun-shrine seen for the first time; a teleport-room for it must have been
					// already created; add the shrine to it:
					int prevMaze = mazeNr - 1 ;
					var teleportRoom = model.getZone("Teleport_" + prevMaze + "_" + mazeNr) ;
					teleportRoom.addMembers(o.id) ;
				}
			}

			// if e is a moon or immortal shrine and it is just cleansed, then add a new link:
			if ((isMoonShrine(e) || isImmortalShrine(e))
					&& scrollJustUsed != null
					&& e.lastStutterTimestamp  < 0
					&& e.timestamp == state.worldmodel.timestamp
					&& (Boolean) e.properties.get("cleansed")
					) {
				// we register the link in both directions; it links shrine to scroll that unlocks it:
				//   the alpha-function needs the shrine->scroll link
				//   the SA2 algorithm needs the scroll->scrine link
				model.registerObjectLinks(e.id, scrollJustUsed) ;
				model.registerObjectLinks(scrollJustUsed,e.id) ;
				
			}
		}
	}
	
}
