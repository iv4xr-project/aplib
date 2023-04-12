package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import eu.iv4xr.framework.mainConcepts.Iv4xrEnvironment;
import eu.iv4xr.framework.mainConcepts.*;
import eu.iv4xr.framework.spatial.IntVec2D;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Frodo;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Monster;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Shrine;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Smeagol;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides an implementation of {@link nl.uu.cs.aplib.mainConcepts.Environment}
 * to connect iv4xr/aplib agents to the game MiniDungeon.
 * 
 * @author wish
 */
public class MyAgentEnv extends Iv4xrEnvironment{
	
	public DungeonApp app ;
	
	public MyAgentEnv(DungeonApp app) {
		this.app = app ;
		this.turnOnDebugInstrumentation() ;
	}
	
	public MiniDungeon thegame() {
		return app.dungeon ;
	}
	
	/**
	 * Observing does not advance the game turn.
	 */
	@Override
	public WorldModel observe(String agentId) {
		// instrument a non-command to env:
		this.instrument(null);
		
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
		
		// adding visible objects:
		var visibleTiles = thegame().visibleTiles() ;
		for(var sq : visibleTiles) {
			int mazeId = sq.fst ;
			var world = app.dungeon.mazes.get(mazeId).world ;
 			Entity e = world[sq.snd.x][sq.snd.y] ;
			if (e != null) {
				wom.elements.put(e.id, toWorldEntity(e)) ;
			}
		}
		// time-stamp the elements:
		for(var e : wom.elements.values()) {
			e.timestamp = wom.timestamp ;
		}
		return wom ;
	}
	
	/**
	 * Do move up/down/left/right and use a potion. It does not do quite nor
	 * reload.
	 */
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
		var obs = observe(agentId) ;
		// to remember the command that is sent; has to be done after observe :| 
		this.instrument(new EnvOperation(agentId,null,"" + cmd,null,null));
		return  obs ;
	}
	

	WorldEntity toWorldEntity(Entity e) {
		switch(e.type) {
			case WALL: 
			case SCROLL:
			case HEALPOT:
			case RAGEPOT:
				WorldEntity we = new WorldEntity(e.id,"" + e.type,false) ;
				we.position = new Vec3(e.x,0,e.y) ;
				we.properties.put("maze",e.mazeId) ;
				return we ;
			case SHRINE:
				we = new WorldEntity(e.id,"" + e.type,true) ;
				var shrine = (Shrine) e ;
				we.position = new Vec3(e.x,0,e.y) ;
				we.properties.put("maze",e.mazeId) ;
				we.properties.put("shrinetype",shrine.shrineType) ;
				we.properties.put("cleansed",shrine.cleansed) ;
				return we ;
			case FRODO:
			case SMEAGOL:
				we = new WorldEntity(e.id,"" + e.type,true) ;
				we.position = new Vec3(e.x,0,e.y) ;
				we.properties.put("maze",e.mazeId) ;
				Player player = (Player) e ;	
				we.properties.put("hp",player.hp) ;
				we.properties.put("hpmax",player.hpMax) ;
				we.properties.put("ar",player.attackRating) ;
				we.properties.put("score",player.score) ;
				we.properties.put("bagUsed",player.bag.size()) ;
				we.properties.put("maxBagSize",player.maxBagSize) ;
				we.properties.put("scrollsInBag",player.itemsInBag(EntityType.SCROLL).size()) ;
				we.properties.put("healpotsInBag",player.itemsInBag(EntityType.HEALPOT).size()) ;
				we.properties.put("ragepotsInBag",player.itemsInBag(EntityType.RAGEPOT).size()) ;
				we.properties.put("rageTimer",player.rageTimer) ;
				return we ;
			case MONSTER:
				//System.out.println(">>>> observing: " + e.id) ;
 				we = new WorldEntity(e.id,"" + e.type,true) ;
				we.position = new Vec3(e.x,0,e.y) ;
				we.properties.put("maze",e.mazeId) ;
				Monster m = (Monster) e ;	
				we.properties.put("hp",m.hp) ;
				we.properties.put("ar",m.attackRating) ;
				we.properties.put("aggravated",m.aggravated) ;
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
		
		// recently removed objects:
		String[] removed = new String[thegame().removed.size()] ;
		int k = 0 ;
		for(var id : thegame().removed) {
			removed[k] = id ;
			k++ ;
		}
		aux.properties.put("recentlyRemoved",removed) ;
		
		// currently visible tiles:
		var visibleTiles_ = thegame().visibleTiles() ;
		Serializable[] visibleTiles = new Serializable[visibleTiles_.size()] ;
		k = 0 ;
		//var world = app.dungeon.currentMaze(app.dungeon.frodo()).world ;
		for(var tile : visibleTiles_) {
			String etype = "" ;
			int mazeId = tile.fst ;
			var world =  app.dungeon.mazes.get(mazeId).world ;
			Entity e = world[tile.snd.x][tile.snd.y] ;
			if (e != null) {
				etype = e.type.toString() ;
			}
			Serializable[] entry = { mazeId, tile.snd , etype } ;
			visibleTiles[k] = entry ;
			k++ ;
		}
		aux.properties.put("visibleTiles",visibleTiles) ;
		
		return aux ;
	}
	
	// just for testing:
	/*
	public static void main(String[] args) throws Exception {
		
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
	*/

}
