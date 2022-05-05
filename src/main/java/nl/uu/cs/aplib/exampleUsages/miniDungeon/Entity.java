package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.IntVec2D;

public class Entity {
	
	public String id ;
	/**
	 * Specifying in which maze the entity is located.
	 */
	public String mazeId ;
	public int x ;
	public int y ;
	
	public IntVec2D pos() {
		return new IntVec2D(x,y) ;
	}

	
	
	public static class Wall extends Entity{
		
		public Wall(int x, int y) {
			this.x = x ; this.y = y ;
			id = "W" + x + "-" + y ;
		}

	}
	
	public static class Shrine extends Entity{
		
		/**
		 * True if this shrine is an immortal shrine. Cleansing an immortal shrine wins 
		 * the game.
		 */
		public boolean immortal = true ;
		
		public boolean cleansed = false ;
		
		public Shrine(int x, int y) {
			this.x = x ; this.y = y ;
			id = "Shr" ;
		}

	}
	
	public static class CombativeEntity extends Entity{
		public int hp ;
		public int hpMax ;
		public int attackRating ;
	}
	
	public static class  Player extends CombativeEntity{
		public List<Entity> bag = new LinkedList<>() ;
		// when 0 the player is not raged; when raged, it AR counts double
		public String name = "Frodo" ;
		public int rageTimer = 0 ;
		public int maxBagSize = 2 ;
		public Player(int x, int y) {
			this.x = x ; this.y = y ;
			hpMax = 20 ;
			hp = 20 ;
			attackRating = 1 ;
			id = "P" ;
		}
		
		public boolean dead() {
			return hp <= 0 ;
		}
		
		public List<Entity> itemsInBag(Class C) {
			   return bag.stream()
				. filter(i -> i.getClass() == C)
				. collect(Collectors.toList()) ;
		}
			
	}
	
	public static class Frodo extends Player {
		public Frodo(int x, int y) {
			super(x,y) ;
			id = name ;
		}
	}
	
	public static class Smeagol extends Player {
		public Smeagol(int x, int y) {
			super(x,y) ;
			name = "Smaegol" ;
			id = name ;
			hp = (hp * 3)/2 ;
			hpMax = hp ;
			attackRating = attackRating*2 ;
			maxBagSize = 1 ;
		}
	}
	
	public static class  Monster extends CombativeEntity{
		public Monster(int x, int y, int id) {
			this.x = x ; this.y = y ;
			hpMax = 20 ;
			hp = 3 ;
			attackRating = 1 ;
			this.id = "M" + id ;
		}
	}
	
	public static class  HealingPotion extends Entity{
		public HealingPotion(int x, int y, int id) {
			this.x = x ; this.y = y ;
			this.id = "H" + id ;
		}
	}
	
	public static class  RagePotion extends Entity{
		public RagePotion(int x, int y, int id) {
			this.x = x ; this.y = y ;
			this.id = "R" + id ;
		}
	}
	
	/**
	 * Only a a holy scroll can bless a shrine.
	 */
	public static class  Scroll extends Entity{
		
		boolean holy = false ;
		
		public Scroll(int x, int y, int id) {
			this.x = x ; this.y = y ;
			this.id = "S" + id ;
		}
	}

}