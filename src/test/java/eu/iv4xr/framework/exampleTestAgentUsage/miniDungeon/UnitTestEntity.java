package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;

class UnitTestEntity {
	
	@Test
	void testEntity() {
		Entity f = new Frodo(9,0) ;
		assertTrue(f.pos().equals(new IntVec2D(9,0))) ;
	}
	
	@Test
	void testFrodo() {
		Frodo f = new Frodo(0,0) ;
		assertTrue(f.name.equals("Frodo")) ;
		assertTrue(f.id.equals("Frodo")) ;
		assertTrue(f.x == 0 && f.y == 0 && f.type == EntityType.FRODO);
		assertTrue(f.hp == f.hpMax  && f.hp >= 20) ;
		assertTrue(f.attackRating > 0) ;
		assertTrue(f.maxBagSize == 2) ;
		assertTrue(f.rageTimer == 0) ;
	}
	
	@Test
	void testSmeagol() {
		Smeagol s = new Smeagol(1,1) ;
		assertTrue(s.name.equals("Smeagol")) ;
		assertTrue(s.id.equals("Smeagol")) ;
		assertTrue(s.x == 1 && s.y == 1 && s.type == EntityType.SMEAGOL);
		assertTrue(s.hp == s.hpMax && s.hp >= 20) ;
		assertTrue(s.attackRating > 0) ;
		assertTrue(s.maxBagSize == 1) ;
		assertTrue(s.rageTimer == 0) ;
	}
	
	@Test
	void testMonster() {
		Monster m = new Monster(1,2,"M0") ;
		assertTrue(m.id.equals("M0")) ;
		assertTrue(m.x == 1 && m.y == 2 && m.type == EntityType.MONSTER);
		assertTrue(m.hp == m.hpMax) ;
		assertTrue(m.attackRating > 0) ;
		assertTrue(!m.aggravated) ;
	}
	
	@Test
	void testMonsterAggravation() {
		Monster m = new Monster(1,2,"M0") ;
		m.aggrevate();
		assertTrue(m.aggravated) ;
		// can't test the aggravate-timer as it is not accessible
		m.disAggrevate(); 
		assertFalse(m.aggravated) ;	
	}
	
	@Test
	void testPlayer() {
		Player f = new Frodo(0,0) ;
		assertTrue(! f.dead()) ;
		f.hp = 0 ;
		assertTrue(f.dead()) ;
		assertTrue(f.itemsInBag(EntityType.HEALPOT).isEmpty()) ;	
	}
	
	@Test
	void testPlayerBag() {
		Player f = new Frodo(0,0) ;
		assertTrue(! f.dead()) ;
		var h = new HealingPotion(9,1,"H0") ;
		f.bag.add(h) ;
		assertTrue(f.itemsInBag(EntityType.HEALPOT).contains(h)) ;	
		assertTrue(f.itemsInBag(EntityType.SCROLL).isEmpty()) ;	
	}
	
	@Test
	void testHealPot() {
		var h = new HealingPotion(9,1,"H0") ;
		assertTrue(h.id.equals("H0")) ;
		assertTrue(h.x == 9 && h.y == 1 && h.type == EntityType.HEALPOT);
	}
	
	@Test
	void testRagePot() {
		var r = new RagePotion(9,1,"R0") ;
		assertTrue(r.id.equals("R0")) ;
		assertTrue(r.x == 9 && r.y == 1 && r.type == EntityType.RAGEPOT);
	}
	
	@Test
	void testScroll() {
		var s = new Scroll(9,1,"S0") ;
		assertTrue(s.id.equals("S0")) ;
		assertTrue(s.x == 9 && s.y == 1 && s.type == EntityType.SCROLL);
	}
	
	@Test
	void testWall() {
		var w = new Wall(9,1,"W0") ;
		assertTrue(w.id.equals("W0")) ;
		assertTrue(w.x == 9 && w.y == 1 && w.type == EntityType.WALL);
	}
	
	@Test
	void testShrine() {
		var s = new Shrine(9,1) ;
		// assertTrue(w.id.equals("...")) ; shrine-id will be overriden anyway
		assertTrue(s.x == 9 && s.y == 1 && s.type == EntityType.SHRINE);
		assertTrue(!s.cleansed && s.shrineType == ShrineType.ShrineOfImmortals) ;
	}

}
