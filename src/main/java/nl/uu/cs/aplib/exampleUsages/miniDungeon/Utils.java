package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Shrine;

public class Utils {
	
	
	public static void printEntities(MyAgentState state) {
		printEntities(state.worldmodel) ;
	}
	
	public static void printEntities(WorldModel wom) {
		System.out.println("====== non-wall entities in wom:") ;
		int k = 0 ;
		for(var e : wom.elements.values()) {
			if (e.type.equals("aux") || e.type.equals("WALL")) continue ;
			System.out.print(">>> " + e.id + ", maze: " + TacticLib.mazeId(e) 
				+ ", @" + TacticLib.toTile(e.position)
				+ ", timestamp: " + e.timestamp) ;
			if (e.type.equals(EntityType.SHRINE.toString())) {
				System.out.print(", cleansed: " + e.properties.get("cleansed"));
			}
			System.out.println("") ;
			k++ ;
		}
		System.out.println(">>> #entities=" + k) ;
	}
	
	public static void printInternalEntityState(MiniDungeon game) {
		for(int mazeId=0; mazeId<game.mazes.size(); mazeId++) {
			Maze m = game.mazes.get(mazeId) ;
			for(int x=1; x<game.config.worldSize-1; x++) {
				for(int y=1; y<game.config.worldSize-1; y++) {
					var e = m.world[x][y] ;
					if (e == null || e.type == EntityType.WALL) continue ;
					if (e.type == EntityType.SHRINE) {
						var ss = (Shrine) e ;
						System.out.println("  " + e.id 
								+ ", shrine, cleansed: " + ss.cleansed) ;
					}
					
				}
			}
		}
		
	}

}
