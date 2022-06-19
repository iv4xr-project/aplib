package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.utils.Pair; 

public class UnitTestMiniDungeon {
	
	@Test
	public void testConstructorTwoPlayers() {
		
		var config = new MiniDungeonConfig() ;
		var dungeon = new MiniDungeon(config) ;
		
		assertTrue(dungeon.config == config) ;
		assertTrue(dungeon.players.size() == 2) ;
		assertTrue(dungeon.players.stream().anyMatch(p -> p instanceof Frodo)) ;
		assertTrue(dungeon.players.stream().anyMatch(p -> p instanceof Smeagol)) ;
		assertTrue(dungeon.frodo() != null) ;
		assertTrue(dungeon.smeagol() != null) ;
		assertTrue(dungeon.status == GameStatus.INPROGRESS) ;
		assertTrue(dungeon.mazes.size() == 1) ;
		assertTrue(dungeon.recentlyRemoved.size() == 0) ;
		assertTrue(dungeon.turnNr == 0) ;
		assertTrue(dungeon.aPlayerHasAttacked == false) ;
		
	}
	
	@Test
	public void testConstructorOnePlayers() {
		var config = new MiniDungeonConfig() ;
		config.enableSmeagol = false ;
		var dungeon = new MiniDungeon(config) ;
		
		assertTrue(dungeon.config == config) ;
		assertTrue(dungeon.players.size() == 1) ;
		assertTrue(dungeon.players.stream().anyMatch(p -> p instanceof Frodo)) ;
		assertTrue(dungeon.frodo() != null) ;
		assertTrue(dungeon.smeagol() == null) ;
	}
	
	@Test
	public void testSeeding() {
		
		var config = new MiniDungeonConfig() ;
		
		int N = config.worldSize ;
		
		var dungeon = new MiniDungeon(config) ;
		
		var maze0 = dungeon.mazes.get(0) ;
		assertTrue(maze0.world.length == N) ;

		assertTrue(maze0.id == 0) ;

		assertTrue(dungeon.frodo().mazeId == 0) ;
		assertTrue(dungeon.smeagol().mazeId == 0) ;
		assertTrue(dungeon.smeagol().x == 1 && dungeon.smeagol().y == 1) ;
		
		var shrine = (Shrine) maze0.world[N-2][1] ;
		
		assertTrue(shrine.mazeId == 0 && shrine.cleansed == false 
				   && shrine.shrineType == ShrineType.MoonShrine) ;
		
		int scrollCount = 0 ;
		int healPotCount = 0 ;
		int ragePotCount = 0 ;
		int monsterCount = 0 ;
		for (int x = 0; x<N; x++) {
			for (int y=0; y<N; y++) {
				Entity e = maze0.world[x][y] ;
				if (e==null) continue ;
				assertTrue(e.mazeId == 0 && e.x == x && e.y == y) ;
				if (e instanceof Scroll) {
					scrollCount++ ;
				}
				else if (e instanceof HealingPotion) {
					healPotCount++ ;
				}
				else if (e instanceof RagePotion) {
					ragePotCount++ ;
				}
				else if (e instanceof Monster) {
					monsterCount++ ;
				}
			}
		}
		assertTrue(scrollCount == config.numberOfKeys
				&& healPotCount == config.numberOfHealPots
				&& ragePotCount == config.numberOfRagePots
				&& monsterCount == config.numberOfMonsters
				) ;
	}
	
	@Test
	public void testVisibility() {

		var config = new MiniDungeonConfig();
		config.viewDistance = 4 ;
		int N = config.worldSize;
		var dungeon = new MiniDungeon(config);
		var maze0 = dungeon.mazes.get(0) ;
		
		var visibles = dungeon.visibleTiles() ;
		
		var fp = dungeon.frodo().pos() ;
		var sp = dungeon.smeagol().pos() ;
	    
		
		for (int x = 0; x<N; x++) {
			for (int y=0; y<N; y++) {
				var t = new Pair<>(0, new IntVec2D(x,y)) ;
				
				if (visibles.contains(t)) {
					assertTrue(IntVec2D.dist(t.snd,fp) <= 4 
							|| IntVec2D.dist(t.snd,sp) <= 4 ) ;
				}
				else {
					assertTrue(IntVec2D.dist(t.snd,fp) > 4 
							&& IntVec2D.dist(t.snd,sp) > 4 ) ;
				}
				
			}
		}

	}
	

}
