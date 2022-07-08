package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.IntVec2D;

public class Entity {
	
	public enum EntityType { 
		WALL, MONSTER, HEALPOT, RAGEPOT, SCROLL, FRODO, SMEAGOL, SHRINE
	}
	
	public String id ;
	/**
	 * Specifying in which maze the entity is located.
	 */
	public int mazeId ;
	
	public int x ;
	public int y ;
	public EntityType type ;
	
	public IntVec2D pos() {
		return new IntVec2D(x,y) ;
	}

	public static class Wall extends Entity{
		
		public Wall(int x, int y, String id) {
			this.x = x ; this.y = y ;
			this.id = id ;
			this.type = EntityType.WALL ;
		}

	}
	
	public enum ShrineType { SunShrine, MoonShrine, ShrineOfImmortals }
	
	public static class Shrine extends Entity{
		
		/**
		 * Cleansing an immortal shrine wins the game.
		 */
		public ShrineType shrineType = ShrineType.ShrineOfImmortals ;
		
		public boolean cleansed = false ;
		
		public Shrine(int x, int y) {
			this.x = x ; this.y = y ;
			id = "Shr" ;
			this.type = EntityType.SHRINE ;
		}

	}
	
	public static class CombativeEntity extends Entity{
		public int hp ;
		public int hpMax ;
		public int attackRating ;
	}
	
	public static class  Player extends CombativeEntity{
		public String name ;
		// when 0 the player is not raged; when raged, it AR counts double
		public int rageTimer = 0 ;
		public int maxBagSize  ;
		public List<Entity> bag = new LinkedList<>() ;
		
		private Player() { }
		
		public boolean dead() {
			return hp <= 0 ;
		}
		
		public List<Entity> itemsInBag(EntityType ety) {
			   return bag.stream()
				. filter(i -> i.type == ety)
				. collect(Collectors.toList()) ;
		}
			
	}
	
	public static class Frodo extends Player {
		public Frodo(int x, int y) {
			name = "Frodo" ;
			id = name ;
			this.type = EntityType.FRODO ;
			this.x = x ; this.y = y ;
			hp = 20 ;
			hpMax = 20 ;
			attackRating = 1 ;
			maxBagSize = 2 ;
			
		}
	}
	
	public static class Smeagol extends Player {
		public Smeagol(int x, int y) {
			// BUG found by unit-test
			// name = "Smaegol" ; 
			name = "Smeagol" ;
			id = name ;
			this.type = EntityType.SMEAGOL ;
			this.x = x ; this.y = y ;
			hp = 30 ;
			hpMax = hp ;
			attackRating = 2 ;
			maxBagSize = 1 ;
		}
	}
	
	public static class  Monster extends CombativeEntity{
		
		public boolean aggravated = false ; 
		int aggravateTimer = 0 ;
		
		public Monster(int x, int y, String id) {
			this.x = x ; this.y = y ;
			// BUG found by unit testing
			//hpMax = 20 ;
			//hp = 3 ;
			hp = 3 ;
			hpMax = hp ;
			attackRating = 1 ;
			this.id = id ;
			this.type = EntityType.MONSTER ;
		}
		
		public void aggrevate() {
			aggravated = true ;
			aggravateTimer = 5 ;
		}
		
		public void disAggrevate() {
			aggravated = false ;
			aggravateTimer = 0 ;
		}
	}
	
	public static class  HealingPotion extends Entity{
		public HealingPotion(int x, int y, String id) {
			this.x = x ; this.y = y ;
			this.id = id ;
			this.type = EntityType.HEALPOT ;
		}
	}
	
	public static class  RagePotion extends Entity{
		public RagePotion(int x, int y, String id) {
			this.x = x ; this.y = y ;
			this.id = id ;
			this.type = EntityType.RAGEPOT ;
		}
	}
	
	/**
	 * Only a a holy scroll can bless a shrine.
	 */
	public static class  Scroll extends Entity{
		
		boolean holy = false ;
		
		public Scroll(int x, int y, String id) {
			this.x = x ; this.y = y ;
			this.id = id ;
			this.type = EntityType.SCROLL ;
		}
	}

}