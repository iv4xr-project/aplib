package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.LinkedList;
import java.util.List;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.Player;

public class Entity {
	
	public enum EntityType { PLAYER, MONSTER, GOALFLAG, HEALPOT, RAGEPOT, WALL }
	
	public String id ;
	public int x ;
	public int y ;
	
	public Pos2D pos() {
		return new Pos2D(x,y) ;
	}

	
	
	public static class Wall extends Entity{
		
		public Wall(int x, int y) {
			this.x = x ; this.y = y ;
			id = "W" + x + "-" + y ;
		}

	}
	
	public static class GoalFlag extends Entity{
		
		public GoalFlag(int x, int y) {
			this.x = x ; this.y = y ;
			id = "G" ;
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
	}
	
	public static class Frodo extends Player {
		public Frodo(int x, int y) {
			super(x,y) ;
		}
	}
	
	public static class Smeagol extends Player {
		public Smeagol(int x, int y) {
			super(x,y) ;
			name = "Smaegol" ;
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
	 * Only a blessed key can open a door.
	 */
	public static class  Key extends Entity{
		
		boolean blessed = false ;
		
		public Key(int x, int y, int id) {
			this.x = x ; this.y = y ;
			this.id = "K" + id ;
		}
	}

}