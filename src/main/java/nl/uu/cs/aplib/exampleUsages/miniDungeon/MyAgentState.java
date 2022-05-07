package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.io.Serializable;

import eu.iv4xr.framework.extensions.pathfinding.AStar;
import eu.iv4xr.framework.extensions.pathfinding.Pathfinder;
import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph;
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

public class MyAgentState extends Iv4xrAgentState<Sparse2DTiledSurface_NavGraph.Tile> {
	
	@Override
	public MyAgentEnv env() {
		return (MyAgentEnv) super.env() ;
	}
	
	@Override
	public MyAgentState setEnvironment(Environment env) {
		super.setEnvironment(env) ;
		// creating an instance of navigation graph; setting its
		// configuration etc.
		// The graph is empty when created.
		Sparse2DTiledSurface_NavGraph navg = new Sparse2DTiledSurface_NavGraph() ;
		navg.maxX = env().app.dungeon.config.worldSize ;
		navg.maxY = navg.maxX ;
		this.setWorldNavigation(navg) ;
		return this ;
	}
	
	@Override
	public Sparse2DTiledSurface_NavGraph worldNavigation() {
		return (Sparse2DTiledSurface_NavGraph) super.worldNavigation() ;
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
		Sparse2DTiledSurface_NavGraph nav = worldNavigation()  ;
		var seenTiles = (Serializable[]) aux.properties.get("visibleTiles") ;
		for (var entry_ : seenTiles) {
			var entry = (Serializable[]) entry_ ;
			var tile = (IntVec2D) entry[0] ;
			var type = (String) entry[1] ;
			//System.out.println(">>> registering " + tile + ": " + type) ;
			nav.markAsSeen(new Tile(tile.x,tile.y));
			switch (type) {
			   case "Wall" :
				   nav.addNonNavigable(new Wall(tile.x,tile.y));
				   break ;
			   case "" :
				   nav.removeNonNavigable(tile.x,tile.y);
				   break ;
			   case "Monster" : 
				   // not going to represent monsters as non-navigable
				   // nav.addNonNavigable(new Door(tile.x,tile.y,true));
				   break ;
			   default:
				   nav.addNonNavigable(new Door(tile.x,tile.y));
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
	public static void main(String[] args) {

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
