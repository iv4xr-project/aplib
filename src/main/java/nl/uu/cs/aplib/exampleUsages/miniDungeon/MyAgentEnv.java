package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import eu.iv4xr.framework.mainConcepts.Iv4xrEnvironment;
import eu.iv4xr.framework.mainConcepts.*;
import eu.iv4xr.framework.spatial.IntVec2D;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class MyAgentEnv extends Iv4xrEnvironment{
	
	public DungeonApp app ;
	
	public MyAgentEnv(DungeonApp app) {
		this.app = app ;
	}
	
	public MiniDungeon thegame() {
		return app.dungeon ;
	}
	
	/**
	 * Observing does not advance the game turn.
	 */
	@Override
	public WorldModel observe(String agentId) {
		WorldModel wom = new WorldModel() ;
		Player player = null ;
		if(agentId.equals(Frodo.class.getSimpleName())) {
			player = thegame().frodo() ;
		}
		else if (agentId.equals(Smeagol.class.getSimpleName()) && thegame().config.enableSmeagol) {
			player = thegame().smeagol() ;
		}
		else {
			return null ;
		}
		wom.agentId = agentId ;
		wom.position = new Vec3(player.x,0,player.y) ;
		wom.timestamp = thegame().turnNr ;
		
		WorldEntity aux = mkGameAuxState() ;
		
		wom.elements.put(aux.id,aux) ;
		
		for (Player P : thegame().players) {
			wom.elements.put(P.id,toWorldEntity(P)) ;
		}
		
		// addint visible objects:
		var visibleTiles = thegame().visibleTiles() ;
		var world = app.dungeon.currentMaze(app.dungeon.frodo()).world ;
		for(var sq : visibleTiles) {
			Entity e = world[sq.x][sq.y] ;
			if (e != null) {
				wom.elements.put(e.id, toWorldEntity(e)) ;
			}
		}
		return wom ;
	}
	
	public WorldModel action(String agentId, Command cmd) {
		Player player = null ;
		Character c = null ; 
		if(agentId.equals(Frodo.class.getSimpleName())) {
			player = thegame().frodo() ;
			switch(cmd) {
			    case MOVEUP    :  c = 'w' ; break ;
			    case MOVEDOWN  :  c = 's' ; break ;
			    case MOVELEFT  :  c = 'a' ; break ;
			    case MOVERIGHT :  c = 'd' ; break ;
			    case USEHEAL   :  c = 'e' ; break ;
			    case USERAGE   :  c = 'r' ; break ;
			}
		}
		else if (agentId.equals(Smeagol.class.getSimpleName()) && thegame().config.enableSmeagol) {
			player = thegame().smeagol() ;
			switch(cmd) {
		    case MOVEUP    :  c = 'i' ; break ;
		    case MOVEDOWN  :  c = 'k' ; break ;
		    case MOVELEFT  :  c = 'j' ; break ;
		    case MOVERIGHT :  c = 'l' ; break ;
		    case USEHEAL   :  c = 'o' ; break ;
		    case USERAGE   :  c = 'p' ; break ;
			}
		}
		else {
			throw new IllegalArgumentException("Player " + agentId + " does not exist.") ;
		}
		if (c==null) {
			throw new UnsupportedOperationException("Command " + cmd + " is not supported") ;
		}
		app.keyPressedWorker(c);
		return observe(agentId) ;
	}
	

	WorldEntity toWorldEntity(Entity e) {
		if (e instanceof Wall) {
			var we = new WorldEntity(e.id, e.getClass().getSimpleName(),false) ;
			we.position = new Vec3(e.x,0,e.y) ;
			we.properties.put("maze",e.mazeId) ;
			return we ;
		}
		if (e instanceof Shrine) {
			var we = new WorldEntity(e.id,e.getClass().getSimpleName(),false) ;
			var shrine = (Shrine) e ;
			we.position = new Vec3(e.x,0,e.y) ;
			we.properties.put("maze",e.mazeId) ;
			we.properties.put("immortal",shrine.immortal) ;
			we.properties.put("cleansed",shrine.cleansed) ;
			return we ;
		}
		if (e instanceof Scroll || e instanceof HealingPotion || e instanceof RagePotion) {
			var we = new WorldEntity(e.id,e.getClass().getSimpleName(),false) ;
			we.position = new Vec3(e.x,0,e.y) ;
			we.properties.put("maze",e.mazeId) ;
			return we ;
		}
		if (e instanceof Player) {
			var we = new WorldEntity(e.id,e.getClass().getSimpleName(),true) ;
			we.position = new Vec3(e.x,0,e.y) ;
			we.properties.put("maze",e.mazeId) ;
			Player player = (Player) e ;	
			we.properties.put("hp",player.hp) ;
			we.properties.put("hpmax",player.hpMax) ;
			we.properties.put("ar",player.attackRating) ;
			we.properties.put("keysInBag",player.itemsInBag(Scroll.class).size()) ;
			we.properties.put("healpotsInBag",player.itemsInBag(HealingPotion.class).size()) ;
			we.properties.put("ragepotsInBag",player.itemsInBag(RagePotion.class).size()) ;
			we.properties.put("rageTimer",player.rageTimer) ;
			return we ;
		}
		if (e instanceof Monster) {
			var we = new WorldEntity(e.id,e.getClass().getSimpleName(),true) ;
			we.position = new Vec3(e.x,0,e.y) ;
			we.properties.put("maze",e.mazeId) ;
			Monster m = (Monster) e ;	
			we.properties.put("hp",m.hp) ;
			we.properties.put("ar",m.attackRating) ;
			return we ;
		}
		return null ;
	}
	
	WorldEntity mkGameAuxState() {
		WorldEntity aux = new WorldEntity("aux","aux",true) ;
		aux.properties.put("turn",thegame().turnNr) ;
		aux.properties.put("status",thegame().status) ;
		aux.properties.put("worldSize",thegame().config.worldSize) ;
		aux.properties.put("viewDist",thegame().config.viewDistance) ;
		aux.properties.put("smeagolOn",thegame().config.enableSmeagol) ;
		aux.properties.put("smeagolOn",thegame().config.enableSmeagol) ;
		
		// recently removed objects:
		String[] removed = new String[thegame().recentlyRemoved.size()] ;
		int k = 0 ;
		for(var id : thegame().recentlyRemoved) {
			removed[k] = id ;
			k++ ;
		}
		aux.properties.put("recentlyRemoved",removed) ;
		
		// currently visible tiles:
		var visibleTiles_ = thegame().visibleTiles() ;
		Serializable[] visibleTiles = new Serializable[visibleTiles_.size()] ;
		k = 0 ;
		var world = app.dungeon.currentMaze(app.dungeon.frodo()).world ;
		for(var tile : visibleTiles_) {
			String etype = "" ;
			Entity e = world[tile.x][tile.y] ;
			if (e != null) {
				etype = e.getClass().getSimpleName() ;
			}
			Serializable[] entry = { tile, etype } ;
			visibleTiles[k] = entry ;
			k++ ;
		}
		aux.properties.put("visibleTiles",visibleTiles) ;
		
		return aux ;
	}
	
	public static void main(String[] args) {
		
		//System.out.println(">>>" + Frodo.class.getSimpleName()) ;
		
		MiniDungeonConfig config = new MiniDungeonConfig() ;
		config.viewDistance = 4 ;
		System.out.println(">>> Configuration:\n" + config) ;
		DungeonApp app = new DungeonApp(config) ;
		DungeonApp.deploy(app);
		MyAgentEnv env = new MyAgentEnv(app) ;
		var wom = env.observe("Frodo") ;
		env.action("Frodo", Command.MOVEUP) ;
		env.action("Frodo", Command.MOVEUP) ;
		env.action("Frodo", Command.MOVEUP) ;
	}

}
