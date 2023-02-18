package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.pathfinding.AStar;
import eu.iv4xr.framework.extensions.pathfinding.CanDealWithDynamicObstacle;
import eu.iv4xr.framework.extensions.pathfinding.LayeredAreasNavigation;
import eu.iv4xr.framework.extensions.pathfinding.Navigatable;
import eu.iv4xr.framework.extensions.pathfinding.Pathfinder;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph;
import eu.iv4xr.framework.extensions.pathfinding.XPathfinder;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Door;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Wall;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Frodo;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.ShrineType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Smeagol;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.utils.Pair;

/**
 * Provides an implementation of agent state {@link nl.uu.cs.aplib.mainConcepts.SimpleState}.
 * The state defined here inherits from {@link Iv4xrAgentState}, so it also keeps a
 * historical WorldModel.
 * 
 * @author wish
 *
 */
public class MyAgentState extends Iv4xrAgentState<Void> {
	
	public LayeredAreasNavigation<Tile,Sparse2DTiledSurface_NavGraph>  multiLayerNav ;
	
	@Override
	public MyAgentEnv env() {
		return (MyAgentEnv) super.env() ;
	}
	
	/**
	 * We are not going to keep an Nav-graph, but will instead keep a layered-nav-graphs.
	 */
	@Override
	public Navigatable<Void> worldNavigation() {
		throw new UnsupportedOperationException() ;
	}
	/**
	 * We are not going to keep an Nav-graph, but will instead keep a layered-nav-graphs.
	 */
	@Override
	public MyAgentState setWorldNavigation(Navigatable<Void> nav) {
		throw new UnsupportedOperationException() ;
	}
	
	@Override
	public MyAgentState setEnvironment(Environment env) {
		super.setEnvironment(env) ;
		// creating an instance of navigation graph; setting its
		// configuration etc.
		// The graph is empty when created.
		Sparse2DTiledSurface_NavGraph navg = new Sparse2DTiledSurface_NavGraph() ;
		multiLayerNav = new LayeredAreasNavigation<>() ;
		navg.sizeX = env().app.dungeon.config.worldSize ;
		navg.sizeY = navg.sizeX ;
		multiLayerNav.addNextArea(navg, null, null, false);
		return this ;
	}

	public WorldEntity auxState() {
		return worldmodel().elements.get("aux") ;
	}
	
	@Override
	public void updateState(String agentId) {
		super.updateState(agentId);
		// Updating the navigation graph:
		// System.out.println(">>> updateState") ;
		WorldEntity aux = auxState();
		var seenTiles = (Serializable[]) aux.properties.get("visibleTiles");
		for (var entry_ : seenTiles) {
			var entry = (Serializable[]) entry_;
			int mazeId = (int) entry[0];
			var tile = (IntVec2D) entry[1];
			var type = (String) entry[2];
			//System.out.println(">>> registering maze " + mazeId + ", tile " + tile + ": " + type) ;
			if (mazeId >= multiLayerNav.areas.size()) {
				// detecting a new maze, need to allocate a nav-graph for this maze:
				Sparse2DTiledSurface_NavGraph newNav = new Sparse2DTiledSurface_NavGraph();
				newNav.sizeX = env().app.dungeon.config.worldSize ;
				newNav.sizeY = newNav.sizeX ;
				int N = env().app.dungeon.config.worldSize;
				Tile lowPortal = new Tile(N - 2, 1);
				Tile highPortal = new Tile(1, 1);
				multiLayerNav.addNextArea(newNav, lowPortal, highPortal, true);
			}

			multiLayerNav.markAsSeen(new Pair<>(mazeId, new Tile(tile.x, tile.y)));
			switch (type) {
			case "WALL":
				multiLayerNav.addObstacle(new Pair<>(mazeId, new Wall(tile.x, tile.y)));
				break;
			case "":
				multiLayerNav.removeObstacle(new Pair<>(mazeId, new Tile(tile.x, tile.y)));
				break;
			case "MONSTER":
				// not going to represent monsters as non-navigable
				// nav.addNonNavigable(new Door(tile.x,tile.y,true));
				break;
			default:
				// representing potions, scrolls and shrines as doors that we can
				// open or close to enable navigation onto them or not:
				multiLayerNav.addObstacle(new Pair<>(mazeId, new Door(tile.x, tile.y)));
				break;
			}
		}
		// removing entities that are no longer in the game-board, except players:
		var removedEntities = (Serializable[]) aux.properties.get("recentlyRemoved");
		for (var entry_ : removedEntities) {
			var id = (String) entry_;
			if (id.equals("Frodo") || id.equals("Smeagol")) {
				continue;
			}
			this.worldmodel.elements.remove(id);
			//System.out.println(">>>> " + this.worldmodel.agentId 
			//		+ " removing " + id + ", in wom: " + this.worldmodel.elements.get(id)) ; ;
		}
		//System.out.println(">>>> " + this.worldmodel.agentId
		//		+ " check M0_5: "+ this.worldmodel.elements.get("M0_5")) ; ;

		// set the obstacle-state of cleansed shrine to "open" (and its nav-teleport too):
		for (var e : worldmodel.elements.values()) {
			if (e.type.equals(EntityType.SHRINE.toString()) && (boolean) e.properties.get("cleansed")) {
				int e_i = (int) e.properties.get("maze");
				Tile et = new Tile(e.position.x, e.position.z);
				var blocker = new Pair<>(e_i, et) ;
				// just unblock it again :)
				//System.out.println("=== unblocking " + e.id + " in maze " + e_i);
				multiLayerNav.toggleBlockingOff(blocker);
				var shrineType = (ShrineType) e.properties.get("shrinetype") ;
				if (shrineType == ShrineType.MoonShrine && multiLayerNav.areas.size() > e_i+1) {
					// open the portal (again) :
					multiLayerNav.setPortal(e_i, e_i + 1 , true) ;
				}
				else if(shrineType == ShrineType.SunShrine) {
					multiLayerNav.setPortal(e_i, e_i - 1 , true) ;					
				}
				
			}
		}

	}
	
	/**
	 * Return the game status (as registered in this state).
	 */
	public GameStatus gameStatus() {
		var aux = worldmodel.elements.get("aux") ;
		var status = (GameStatus) aux.properties.get("status") ;
		return status ;
	}
	
	/**
	 * Check if the agent that owns this state is alive in the game
	 * (its hp>0).
	 */
	public boolean agentIsAlive() {
		var a = worldmodel.elements.get(worldmodel.agentId) ;
		if (a==null) {
			throw new IllegalArgumentException() ;
		}
		var hp = (Integer) a.properties.get("hp") ;
		if (hp==null) {
			throw new IllegalArgumentException() ;
		}
		return hp>0 ;
	}
	
	/**
	 * Return a list of monsters which are currently adjacent to the agent that
	 * owns this state.
=	 */
	public List<WorldEntity> adajcentMonsters() {
		var player = worldmodel.elements.get(worldmodel.agentId) ;
		Tile p = Utils.toTile(player.position) ;
		List<WorldEntity> ms = worldmodel.elements.values().stream()
				.filter(e -> e.type.equals(EntityType.MONSTER.toString())
						     && Utils.mazeId(player) == Utils.mazeId(e)
						 	 && Utils.adjacent(p,Utils.toTile(e.position)))
				.collect(Collectors.toList()) ;
		return ms ;
	}


	// just for testing:
	/*
	public static void main(String[] args) throws Exception {

		// System.out.println(">>>" + Frodo.class.getSimpleName()) ;

		MiniDungeonConfig config = new MiniDungeonConfig();
		config.viewDistance = 4;
		System.out.println(">>> Configuration:\n" + config);
		DungeonApp app = new DungeonApp(config);
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState() ;
		state.setEnvironment(env) ;
		state.updateState("Frodo");
		state.updateState("Frodo");
		state.updateState("Frodo");
		env.action("Frodo", Command.MOVEUP);
		env.action("Frodo", Command.MOVEUP);
		env.action("Frodo", Command.MOVEUP);
	}
	*/
	
}
