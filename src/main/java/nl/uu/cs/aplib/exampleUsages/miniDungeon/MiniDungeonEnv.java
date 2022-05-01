package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import eu.iv4xr.framework.mainConcepts.Iv4xrEnvironment;
import eu.iv4xr.framework.mainConcepts.*;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

import static nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;

import java.util.LinkedList;
import java.util.List;

public class MiniDungeonEnv extends Iv4xrEnvironment{
	
	public MiniDungeon thegame ;
	
	public Pair<Integer,List<Entity>> entitiesInPreviousTurn ;
	
	public WorldModel observe(String agentId) {
		WorldModel wom = new WorldModel() ;
		Player player = null ;
		if(agentId.equals(Frodo.class.getName())) {
			player = thegame.frodo() ;
		}
		else if (agentId.equals(Smeagol.class.getName()) && thegame.config.enableSmeagol) {
			player = thegame.smeagol() ;
		}
		else {
			return null ;
		}
		wom.agentId = agentId ;
		wom.position = new Vec3(player.x,0,player.y) ;
		wom.timestamp = thegame.turnNr ;
		
		WorldEntity aux = mkGameAuxState() ;
		
		wom.elements.put(aux.id,aux) ;
		
		for (Player P : thegame.players) {
			wom.elements.put(P.id,toWorldEntity(P)) ;
		}
		
		var visibleTiles = thegame.visibleTiles() ;
		for(var sq : visibleTiles) {
			Entity e = thegame.world[sq.x][sq.y] ;
			if (e != null) {
				wom.elements.put(e.id, toWorldEntity(e)) ;
			}
		}
		return wom ;
	}
	

	WorldEntity toWorldEntity(Entity e) {
		if (e instanceof Wall) {
			var we = new WorldEntity(e.id, e.getClass().getName(),false) ;
			we.position = new Vec3(e.x,0,e.y) ;
			return we ;
		}
		if (e instanceof GoalFlag) {
			var we = new WorldEntity(e.id,e.getClass().getName(),false) ;
			we.position = new Vec3(e.x,0,e.y) ;
			return we ;
		}
		if (e instanceof Key || e instanceof HealingPotion || e instanceof RagePotion) {
			var we = new WorldEntity(e.id,e.getClass().getName(),false) ;
			we.position = new Vec3(e.x,0,e.y) ;
			return we ;
		}
		if (e instanceof Player) {
			var we = new WorldEntity(e.id,e.getClass().getName(),true) ;
			we.position = new Vec3(e.x,0,e.y) ;
			Player player = (Player) e ;	
			we.properties.put("hp",player.hp) ;
			we.properties.put("hpmax",player.hpMax) ;
			we.properties.put("ar",player.attackRating) ;
			we.properties.put("keysInBag",player.itemsInBag(Key.class).size()) ;
			we.properties.put("healpotsInBag",player.itemsInBag(HealingPotion.class).size()) ;
			we.properties.put("ragepotsInBag",player.itemsInBag(RagePotion.class).size()) ;
			we.properties.put("rageTimer",player.rageTimer) ;
			return we ;
		}
		if (e instanceof Monster) {
			var we = new WorldEntity(e.id,e.getClass().getName(),true) ;
			we.position = new Vec3(e.x,0,e.y) ;
			Monster m = (Monster) e ;	
			we.properties.put("hp",m.hp) ;
			we.properties.put("ar",m.attackRating) ;
			return we ;
		}
		return null ;
	}
	
	WorldEntity mkGameAuxState() {
		WorldEntity aux = new WorldEntity("aux","aux",true) ;
		aux.properties.put("turn",thegame.turnNr) ;
		aux.properties.put("status",thegame.status) ;
		aux.properties.put("worldSize",thegame.config.worldSize) ;
		aux.properties.put("viewDist",thegame.config.viewDistance) ;
		aux.properties.put("smeagolOn",thegame.config.enableSmeagol) ;
		aux.properties.put("smeagolOn",thegame.config.enableSmeagol) ;
		String removed = "" ;
		int k = 0 ;
		for(var id : thegame.recentlyRemoved) {
			if (k>0) removed += "\n" ;
			removed += id ;
			k++ ;
		}
		aux.properties.put("recentlyRemoved",removed) ;
		return aux ;
	}

}
