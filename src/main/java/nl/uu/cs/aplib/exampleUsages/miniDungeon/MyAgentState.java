package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.io.Serializable;

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
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Frodo;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Smeagol;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.utils.Pair;

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
		navg.maxX = env().app.dungeon.config.worldSize ;
		navg.maxY = navg.maxX ;
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
		WorldEntity aux = auxState() ;
		var seenTiles = (Serializable[]) aux.properties.get("visibleTiles") ;
		for (var entry_ : seenTiles) {
			var entry = (Serializable[]) entry_ ;
			int mazeId = (int) entry[0] ;
			var tile = (IntVec2D) entry[1] ;
			var type = (String) entry[2] ;
			//System.out.println(">>> registering " + tile + ": " + type) ;
			if (mazeId > multiLayerNav.areas.size()) {
				// detecting a new maze, need to allocate a nav-graph for this maze:
				Sparse2DTiledSurface_NavGraph newNav = new Sparse2DTiledSurface_NavGraph() ;
				int N = env().app.dungeon.config.worldSize ;
				Tile lowPortal  = new Tile(N-1,1);
				Tile highPortal = new Tile(1,1) ;
				multiLayerNav.addNextArea(newNav, lowPortal, highPortal, false);
			}
				
			multiLayerNav.markAsSeen(new Pair<>(mazeId,new Tile(tile.x,tile.y)));
			switch (type) {
			   case "WALL" :
				   multiLayerNav.addObstacle(new Pair<>(mazeId, new Wall(tile.x,tile.y))) ;
				   break ;
			   case "" :
				   multiLayerNav.removeObstacle(new Pair<>(mazeId, new Tile(tile.x,tile.y))) ;
				   break ;
			   case "MONSTER" : 
				   // not going to represent monsters as non-navigable
				   // nav.addNonNavigable(new Door(tile.x,tile.y,true));
				   break ;
			   default:
				   // representing potions, scrolls and shrines as doors that we can
				   // open or close to enable navigation onto them or not:
				   multiLayerNav.addObstacle(new Pair<>(mazeId, new Door(tile.x,tile.y))) ;
				   break ;			   
			}	
		}	
		// removing entities that are no longer in the game-board, except players:
		var removedEntities = (Serializable[]) aux.properties.get("recentlyRemoved") ;
		for (var entry_ : removedEntities) {
			var id = (String) entry_ ;
			if (id.equals(Frodo.class.getSimpleName()) || id.equals(Smeagol.class.getSimpleName())) {
				continue ;
			}
			this.worldmodel.elements.remove(id) ;
		}
		
	}
	
	
	// just for testing:
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
	
}
