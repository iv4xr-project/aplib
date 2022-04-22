package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.util.LinkedList;
import java.util.List;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.EntityType;

public class Entity {
	
	public enum EntityType { PLAYER, MONSTER, GOALFLAG, HEALPOT, WALL }
	
	public String id ;
	public int x ;
	public int y ;

	
	
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
		public Player(int x, int y) {
			this.x = x ; this.y = y ;
			hpMax = 20 ;
			hp = 20 ;
			attackRating = 1 ;
			id = "P" ;
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
	
	public static class  Key extends Entity{
		
		boolean goldenKey = false ;
		
		public Key(int x, int y, int id) {
			this.x = x ; this.y = y ;
			this.id = "K" + id ;
		}
	}
	
	public static class  Door extends Entity{
		public boolean isOpen = false ;
		public Door(int x, int y, int id) {
			this.x = x ; this.y = y ;
			this.id = "B" + id ;
		}
	}

}