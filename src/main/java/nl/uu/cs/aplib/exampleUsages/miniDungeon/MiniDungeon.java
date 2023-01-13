package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.io.Console;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;
import nl.uu.cs.aplib.utils.Pair;

/**
 * A MiniDungeon is a game played by one or two players, played in a (simple) maze
 * in an NxN world. The world is tiled where a player can only move from one tile
 * to another. The tiles are arranged to form a square world, from tile (0,0) to
 * tile (N-1,N-1). The tiles on the border of this world are always walls to prevent
 * players from going off the world.
 * 
 * <p>Planned: a more advanced version where players can adventure through a series
 * of mazes through shrines.
 * 
 * <p>The first player is called Frodo. In a multi-player setup, there is a second player
 * called Smeagol. They can play cooperatively, or competitively. The game is turn-based,
 * where at every turn a player moves, then all the monsters move. The other player remains
 * idle during this turn. The game engine does not in itself limits players to move
 * in a strict alteration.
 * 
 * <p>In the current setup, there is only one maze. The players start somewhere in the
 * maze. There is also a shrine somewhere in the maze. A player wins if it manages
 * to bless the shrine.
 * 
 * <ul>
 * <li>Monsters are randomly placed in the dungeon. When a monster is adjacent to a player,
 * it will attack the player. Similarly, a player can attack an adjacent monster (or
 * another player). If a monster's hp reaches 0, it will be removed from the dungeon. 
 * If the player's hp reaches 0 the player dies and cannot participate in the game
 * anymore. If both players die, the game ends; the monsters win.
 * 
 * <li>There are scrolls and potions randomly dropped in the dungeon. 
 * When the player moves to a tile with an item on it, and provided there is
 * still some space in the player's bag, the item is picked and automatically placed in 
 * the  bag. Frodo's bag can hold two items. Smeagol has a smaller bag that can only
 * hold one item.
 * 
 * <li>The shrine is tainted, and must be cleansed by using a scroll. However, only
 * a holy scroll can do this. The player does not know up front which scroll is holy
 * until it uses it. Using a scroll consumes it.
 * 
 * <li> The player can use a potion, if his/her bag contains one. Using a heal potion
 * heals the player for some amount of health point. Using a rage potion double
 * its attack rating for some turns.
 * </ul>
 * 
 * You can run the main-method below to actually play the game. You can also control
 * the game from another class. You can do this 
 * 
 * 
 * @author wish
 */

public class MiniDungeon {
	
	public static class MiniDungeonConfig {
		public int worldSize = 20 ;
		public int numberOfMaze = 2 ;
		public int numberOfCorridors = 4 ;
		public float viewDistance = 100f ;
		public long randomSeed = 34793 ;
		public int numberOfMonsters = 6 ;
		public int numberOfHealPots = 2 ;
		public int numberOfRagePots = 2 ;
		public int numberOfScrolls = 3 ;
		public boolean enableSmeagol = true ;
		public String assetsLocation = Path.of("assets","minidungeon").toString() ; 
		
		/**
		 * Producing a default configuration.
		 */
		public MiniDungeonConfig() { }
		
		@Override
		public String toString() {
			String s = "" ;
			s += "randomseed   : " + randomSeed ;
			s += "\nWorldsize    : " + worldSize ;
			s += "\n#mazes       : " + numberOfMaze ;
			s += "\n#corridors   : " +  numberOfCorridors ;
			s += "\nview distance: " +  viewDistance ;
			s += "\n#monsters    : " +  numberOfMonsters ;
			s += "\n#heal-pots   : " +  numberOfHealPots ;
			s += "\n#rage-potss  : " +  numberOfRagePots ;
			s += "\n#scrolls     : " +  numberOfScrolls ;
			s += "\nSmeagol      : " +  enableSmeagol ;
			
			return s ;
		}
	}
	
	public enum GameStatus { INPROGRESS, FRODOWIN, SMEAGOLWIN, MONSTERSWIN }
	
	public MiniDungeonConfig config ;

	
	public List<Maze> mazes = new LinkedList<>();
	public List<Player> players = new LinkedList<>() ;
	public List<String> recentlyRemoved = new LinkedList<>() ;
	
	// using separate random generators to make the dungeon creation more controlable for experiments
	
	/**
	 * Random generator for combats, monster movements etc.
	 */
	Random mainRnd  ;
	/**
	 * Random generator for mazes creation and seeding.
	 */
	Random rndForMazeGen ;
	/**
	 * Random generator for selection of the holy-scrolls.
	 */
	Random holyScrollRnd ;
	
	public int turnNr = 0 ;
	public GameStatus status = GameStatus.INPROGRESS ;
	
	public MiniDungeon(MiniDungeonConfig config) {
		this.config = config ;
		int size = config.worldSize ;
		if (size < 8) throw new IllegalArgumentException("size too small") ;
		if (config.numberOfCorridors > size/3) 
			throw new IllegalArgumentException("too many corridors") ;
		int numberOfItems = config.numberOfHealPots + config.numberOfRagePots + config.numberOfScrolls ;
		if (config.numberOfMonsters + numberOfItems > (size-2)*(size-2)/3) 
			throw new IllegalArgumentException("too many monsters and items") ;
		
		mainRnd = new Random(config.randomSeed) ;
		rndForMazeGen = new Random(config.randomSeed) ;
		holyScrollRnd = new Random(config.randomSeed) ;
		
		var firstMaze = Maze.buildSimpleMaze(0, rndForMazeGen, size, config.numberOfCorridors) ;
		firstMaze.id = 0 ;
		mazes.add(firstMaze) ;
		var world = firstMaze.world ;
		
		seedMaze(firstMaze) ;
		
		// place players (in the first maze):
		int center = size/2 ;
		for (int x=center-1 ; x<=center+1; x++) {
			boolean placed = false ;
			for (int y=center-1; y<=center+1; y++) {
				if ((world[x][y] == null)) {
					var frodo = new Frodo(x,y) ;
					frodo.mazeId = firstMaze.id ;
					world[x][y] = frodo ;
					players.add(frodo) ;
					placed = true ;
					break ;
				}
			}
			if (placed) break ;
		}
		if (config.enableSmeagol) {
			var smeagol = new Smeagol(1,1) ;
			smeagol.mazeId = firstMaze.id ;
			world[1][1] = smeagol ;
			players.add(smeagol) ;
		}
	}
	
	/**
	 * Seed a maze with items, monsters, and shrine.
	 */
	void seedMaze(Maze maze) {
		// place the Shrine:
		var world = maze.world;
		int size = world.length;

		// place shrine a moon-shrine or immortal-shrine:
		Shrine S = new Shrine(size - 2, 1);
		S.mazeId = maze.id;
		S.shrineType = ShrineType.MoonShrine ;
		S.id = "SM" + maze.id ;
		world[size - 2][1] = S;
		if (maze.id == config.numberOfMaze-1) {
			// this is the final maze
			S.shrineType = ShrineType.ShrineOfImmortals ;
			S.id = "SI" + maze.id ;
		}
		
		// place a sun-shrine:
		if (maze.id>0) {
			S = new Shrine(1,1);
			S.mazeId = maze.id;
			S.shrineType = ShrineType.SunShrine ;
			S.id = "SS" + maze.id ;
			// a sun-shrine is always clean:
			S.cleansed = true ;
			world[1][1] = S;
		}

		List<Pair<Integer, Integer>> freeSquares = maze.getFreeTiles() ;
		
		// BUG found during system-testing: 
		// reserve squares in area of the bottom low corner, either to place
		// Smaegol, or to teleport
		freeSquares = freeSquares.stream()
		   .filter(sq -> !(sq.fst <= 3 && sq.snd <= 3))
		   .collect(Collectors.toList());
		
		// reserve squares for Frodo:
		int mid = config.worldSize / 2 ;
		if (maze.id == 0 && config.enableSmeagol) {
			freeSquares = freeSquares.stream()
			   .filter(sq -> !(sq.fst >= mid-1 && sq.fst <= mid+1 
			   				   && sq.snd >= mid-1 && sq.snd <= mid+1))
			   .collect(Collectors.toList());
		}
		

		Maze.removeThoseWithTooManyOccupiedNeighbours(freeSquares,maze,4);

		// seed heal-potions:
		int h = 0;
		while (h < config.numberOfHealPots && freeSquares.size() > 0) {
			int k = rndForMazeGen.nextInt(freeSquares.size());
			var sq = freeSquares.remove(k);
			String id = "H" + maze.id + "_" + h ;
			HealingPotion H = new HealingPotion(sq.fst, sq.snd, id);
			H.mazeId = maze.id;
			world[sq.fst][sq.snd] = H;
			h++;
		}

		// seed rage-potions:
		int r = 0;
		while (r < config.numberOfRagePots && freeSquares.size() > 0) {
			int k = rndForMazeGen.nextInt(freeSquares.size());
			var sq = freeSquares.remove(k);
			String id = "R" + maze.id + "_" + r ;
			RagePotion R = new RagePotion(sq.fst, sq.snd, id);
			R.mazeId = maze.id;
			world[sq.fst][sq.snd] = R;
			r++;
		}

		// seed scrolls:
		int ky = 0;
		List<Scroll> scrolls = new LinkedList<>();
		while (ky < config.numberOfScrolls && freeSquares.size() > 0) {
			int k = rndForMazeGen.nextInt(freeSquares.size());
			var sq = freeSquares.remove(k);
			String id = "S" + maze.id + "_" + ky ;
			Scroll K = new Scroll(sq.fst, sq.snd, id);
			K.mazeId = maze.id;
			world[sq.fst][sq.snd] = K;
			scrolls.add(K);
			ky++;
		}
		// choose one holy scroll:
		Scroll holyScroll = scrolls.get(holyScrollRnd.nextInt(scrolls.size()));
		holyScroll.holy = true;
		
		// seed monsters:
		int m = 0;
		while (m < config.numberOfMonsters && freeSquares.size() > 0) {
			int k = rndForMazeGen.nextInt(freeSquares.size());
			var sq = freeSquares.remove(k);
			String id = "M" + maze.id + "_" + m ;
			Monster M = new Monster(sq.fst, sq.snd, id);
			M.mazeId = maze.id;
			world[sq.fst][sq.snd] = M;
			m++;
		}
	}
	
	
	public boolean showConsoleIO = true ;
	
	Scanner scanner ;
	
	void consolePrint(String s) {
		if (showConsoleIO) System.out.println(s) ;
	}
	
	public Frodo frodo() {
		return (Frodo) players.get(0) ;
	}
	
	public Smeagol smeagol() {
		if (players.size()>1) {
			return (Smeagol) players.get(1) ;
		}
		return null ;
	}
	
	public enum Command { MOVEUP, MOVEDOWN, MOVELEFT, MOVERIGHT, USEHEAL, USERAGE , DONOTHING } 
	
	/**
	 * Remove the entity e from the maze where it is is.
	 */	
	void removeFromMaze(Entity e) {
		var maze = mazes.get(e.mazeId) ;
		maze.world[e.x][e.y] = null ;
		recentlyRemoved.add(e.id) ;
	}
	
	void attack(CombativeEntity attacker, CombativeEntity defender) {
		if(attacker.hp <= 0 || defender.hp <=0) throw new IllegalArgumentException() ;
		int ar = attacker.attackRating ;
		if (attacker instanceof Player && ((Player) attacker).rageTimer>0 ) {
			ar = ar*2 ;
		}
		defender.hp = defender.hp - ar ;
		if (defender.hp <=0) {
			removeFromMaze(defender) ;
		}
	}
	
	
	boolean allPlayersDead() {
		for(var pq : players) {
			if (!pq.dead())  {
				return false ;
			}
		}
		return true ;
	}
	
	Maze currentMaze(Player player) {
		return mazes.get(player.mazeId) ;
	}
	
	String monstersMoves(Player playerThatJustMoved) {
		List<Monster> monsters = new LinkedList<>();
		String msg = "";
		var world = currentMaze(playerThatJustMoved).world ;
		for (int x = 1; x < config.worldSize - 1; x++) {
			for (int y = 1; y < config.worldSize - 1; y++) {
				var e = world[x][y];
				if (e instanceof Monster) {
					Monster m = (Monster) e;
					
					if (!m.aggravated) {
						// the logic for triggering aggravated-state. The monster
						// becomes aggravated if there is an enraged player within
						// 8 radius:
						for (Player player : players) {
							if (player.dead()) continue ;
							if (player.mazeId != m.mazeId) continue ;
							if (player.rageTimer>0) {
								// the player is enraged:
					    		var sqDist = IntVec2D.dist(new IntVec2D(player.x,player.y), new IntVec2D(m.x,m.y)) ;
					    		if (sqDist <= 64f) {
					    			m.aggrevate();
					    			break ;
					    		}
							}
						}
					}
					else {
						// handlin the aggravation timer:
						if (m.aggravateTimer <= 0) {
							m.disAggrevate();
						}
						else  {
							m.aggravateTimer-- ;
						}
					}
					

					// the player is next to m; attack the player:
					boolean thereWasPlayerToAttack = false ;
					for (Player player : players) {
						if (player.dead()) continue ;
						if (player.mazeId != m.mazeId) continue ;
						// in case there are multiple players around m, m will just pick the first one
						// in this list:
						if ((m.x == player.x && Math.abs(m.y - player.y) == 1)
								|| (m.y == player.y && Math.abs(m.x - player.x) == 1)) {
							attack(m, player);
							thereWasPlayerToAttack = true ;
							msg += "\n> Monster " + m.id + " attacked " + player.name + "!";
							if (player.hp <= 0) {
								msg += ". " + player.name + " DIED.";
								if (allPlayersDead()) {
									msg += "\n> All heroes have perished. MUAHAHAHAHA! (evil laugh)";
									status = GameStatus.MONSTERSWIN;
								}
								;
								return msg;
							}
						}
					}
					
					if(thereWasPlayerToAttack) continue ;

					// the case where there is no player next to m:

					// move randomly
					List<Pair<Integer, Integer>> candidates = new LinkedList<>();
					if (m.y < config.worldSize - 2 && world[m.x][m.y + 1] == null) {
						candidates.add(new Pair<>(m.x, m.y + 1));
					}
					if (1 < m.y && world[m.x][m.y - 1] == null) {
						candidates.add(new Pair<>(m.x, m.y - 1));
					}
					if (m.x < config.worldSize - 2 && world[m.x + 1][m.y] == null) {
						candidates.add(new Pair<>(m.x + 1, m.y));
					}
					if (1 < m.x && world[m.x - 1][m.y] == null) {
						candidates.add(new Pair<>(m.x - 1, m.y));
					}
					
					if (candidates.isEmpty()) continue ;
					
					
					Pair<Integer,Integer> sq ;
					
					if(m.aggravated) {
						// the monster is aggravated
						Player fr = frodo() ;
					    Player sm = smeagol() ;
					    float minSqDist = Float.MAX_VALUE ;
					    Pair<Integer,Integer> minSq = null ; 
					    for (var c : candidates) {
					    	float sqDist = Float.MAX_VALUE ;
					    	if (sm != null && !sm.dead() && m.mazeId == sm.mazeId) {
					    		sqDist = IntVec2D.dist(new IntVec2D(c.fst,c.snd), new IntVec2D(sm.x,sm.y)) ;
					    		if (sqDist < minSqDist) {
					    			minSq = c ;
					    			minSqDist = sqDist ;
					    			continue ;
					    		}
					    	}
					    	if (fr != null && !fr.dead() && m.mazeId == fr.mazeId) {
					    		sqDist = IntVec2D.dist(new IntVec2D(c.fst,c.snd), new IntVec2D(fr.x,fr.y)) ;
					    		if (sqDist < minSqDist) {
					    			minSq = c ;
					    			minSqDist = sqDist ;
					    		}
					    	}
					    }
						sq = minSq ;
					}
					else {
						// the monster is not aggravated
						// logic to make the monster aggravated here (for now none):
						// ....
						//  e.g. when there is an enraged player in radius 6
						//
						// Then move
						candidates.add(null);
						sq = candidates.get(mainRnd.nextInt(candidates.size()));
						if (sq == null)
							// the monster decides to stay where it is:
							continue;
					}

					// move the monster to sq:
					world[m.x][m.y] = null;
					m.x = sq.fst;
					m.y = sq.snd;
					world[m.x][m.y] = m;
				}
			}
		}
		return msg;
	}
	
	/**
	 * Do a one turn update. Either of the player can move, and then all monsters
	 * will move. Then the turn number is increased.
	 * The method returns a string, which reports the effect of the player's and
	 * monsters' moves. This is a string to be printed.
	 * If the player's move is invalid, the game-state will remain the same, and
	 * the turn number is not increased.
	 */
	public synchronized String doCommand(char c) {

		// if the game is over, the command is ignored too, but msg is printed:
		if (status != GameStatus.INPROGRESS) {
			return "> The game is already over.";
		}

		var c_ = toCommand(c) ;
		if (c_ == null) return "" ;
		
		Player player = c_.fst ;
		boolean wasEnraged = player.rageTimer > 0;	
		List<String> copyOfRemoved = new LinkedList<>() ;
		copyOfRemoved.addAll(recentlyRemoved) ;
		recentlyRemoved.clear();
		String msg = doCommandWorker(player,c_.snd) ;
		//xSystem.out.println(">>> " + recentlyRemoved) ;
		if (msg == null) {
			recentlyRemoved.addAll(copyOfRemoved) ;
			return "" ;
		}
		// putting the logic for rage time-out here:
		if (player.rageTimer > 0)
			player.rageTimer--;
		if (player.rageTimer == 0 && wasEnraged) {
			msg += "\n> " + player.name + " is no longer enraged.";
		}
		msg += monstersMoves(player);
		turnNr++;
		return msg ;
	}
	
	Pair<Player,Command> toCommand(char c) {
		switch(c) {
		   case 'w' : return new Pair<Player,Command>(frodo(),Command.MOVEUP) ;
		   case 'a' : return new Pair<Player,Command>(frodo(),Command.MOVELEFT) ;
		   case 's' : return new Pair<Player,Command>(frodo(),Command.MOVEDOWN) ;
		   case 'd' : return new Pair<Player,Command>(frodo(),Command.MOVERIGHT) ;
		   case 'e' : return new Pair<Player,Command>(frodo(),Command.USEHEAL) ;
		   case 'r' : return new Pair<Player,Command>(frodo(),Command.USERAGE) ;
		}
		if(!config.enableSmeagol) return null ;
		switch(c) {
		   case 'i' : return new Pair<Player,Command>(smeagol(),Command.MOVEUP) ;
		   case 'j' : return new Pair<Player,Command>(smeagol(),Command.MOVELEFT) ;
		   case 'k' : return new Pair<Player,Command>(smeagol(),Command.MOVEDOWN) ;
		   case 'l' : return new Pair<Player,Command>(smeagol(),Command.MOVERIGHT) ;
		   case 'o' : return new Pair<Player,Command>(smeagol(),Command.USEHEAL) ;
		   case 'p' : return new Pair<Player,Command>(smeagol(),Command.USERAGE) ;
		}
		return null ;
	}
	
	public boolean aPlayerHasAttacked = false ;
	
	/**
	 * Execute a command for the given player. The method returns null if the command
	 * cannot be executed (e.g. the player try to move off the board); the game state
	 * will not change in this case. If the command is successful, the game state is
	 * changed accordingly, and a msg to print is returned.
	 */
	String doCommandWorker(Player player, Command command) {
		
		aPlayerHasAttacked = false ;
		
		if (player.dead())
			return null ;

		int xx = player.x;
		int yy = player.y;

		// illegal wasd are ignored
		switch (command) {
		case DONOTHING:
			return "> " + player.name + " cowers in fear.";

		case USEHEAL:
			var hpotions = player.itemsInBag(EntityType.HEALPOT) ;
			if (hpotions.size() == 0) {
				return "> " + player.name + " does not have any heal-potion to use.";
			} else {
				var P = hpotions.get(0);
				player.bag.remove(P);
				int oldHp = player.hp ;
				player.hp = Math.min(player.hp + 5, player.hpMax);
				if (oldHp <  player.hpMax && player.hp == player.hpMax) 
					player.score += 5 ;
				return "> That tastes good!";
			}
		case USERAGE:
			var rpotions = player.itemsInBag(EntityType.RAGEPOT) ;
			if (rpotions.size() == 0) {
				return "> " + player.name + " does not have any rage-potion to use.";
			} else {
				var P = rpotions.get(0);
				player.bag.remove(P);
				player.rageTimer = 10;
				return "> " + player.name + " is ENRAGED!";
			}
		case MOVEUP:
			if (player.y == config.worldSize - 2)
				return null ;
			yy++;
			break;
		case MOVEDOWN:
			if (player.y == 1)
				return null ;
			yy--;
			break;
		case MOVERIGHT:
			if (player.x == config.worldSize - 2)
				return null ;
			xx++;
			break;
		case MOVELEFT:
			if (player.x == 1)
				return null ;
			xx--;
			break;
		}

		var world = currentMaze(player).world ;
		var target = world[xx][yy];
		if (target == null) {
			// target is clear, move there
			world[player.x][player.y] = null;
			player.x = xx;
			player.y = yy;
			world[xx][yy] = player;
			return "" ;
		} 
		if (target.type == EntityType.WALL) {
			Wall w = (Wall) target ;
			if (w.brokenwall) {
				world[player.x][player.y] = null;
				player.x = xx;
				player.y = yy;
				world[xx][yy] = player;
				return "" ;
			}
			return null ;
		}
		if (target.type == EntityType.HEALPOT 
				|| target.type == EntityType.RAGEPOT 
				|| target.type == EntityType.SCROLL) {
			if (player.bag.size() == player.maxBagSize) {
				return "> " + player.name + ", your bag has no space left.";
			}
			removeFromMaze(target) ;
			world[player.x][player.y] = null;
			player.x = xx;
			player.y = yy;
			world[xx][yy] = player;
			player.bag.add(target);
			if (target instanceof HealingPotion)
				return "> " + player.name + " found a small vial of greed liquid.";
			if (target instanceof RagePotion)
				return "> " + player.name + " found a vial of liquid. It smells bad.";
			else
				return "> " + player.name + " found an old scroll.";
			
		}
		if (target.type == EntityType.MONSTER) {
			var m = (Monster) target;
			String msg = "> " + player.name + " attacked monster " + m.id;
			attack(player, m);
			aPlayerHasAttacked = true ;
			if (m.hp <= 0) {
				player.killCount++ ;
				if (player.killCount == player.nextKillThrophy) {
					player.score += 100 ;
					player.nextKillThrophy = 2 * player.nextKillThrophy ;
				}
				msg += ". The monster is killed!";
			}
			return msg ;
		} 
		if (target instanceof Player) {
			Player otherPlayer = (Player) target;
			String msg = "> " + player.name + " attacked " + otherPlayer.name;
			attack(player, otherPlayer);
			if (otherPlayer.hp <= 0) {
				// you lose point if you deliberately kill other player
				player.score = Math.round(0.75f * (float) player.score) ;
				if (player.score > otherPlayer.score)
					player.score = otherPlayer.score ;
				msg += ". " + otherPlayer.name + " is killed!";
			}
			return msg ;
		} 
		if (target.type == EntityType.SHRINE) {
			var shrine = (Shrine) target ;
			if (shrine.cleansed) {
				if (shrine.shrineType == ShrineType.ShrineOfImmortals) {
					// should not happen, because then the game would have been over
					throw new IllegalArgumentException() ;
				}
				// the shrine is cleansed; this will teleport the player to
				// the connected shrine 
				
				// remove the player from the current maze:
				removeFromMaze(player) ;
				
				// get the next maze; create it if it has not been created:
				Maze maze = currentMaze(player) ;
				int nextMazeId = shrine.shrineType == ShrineType.SunShrine ? maze.id-1 : maze.id+1 ;
				if (mazes.size() == nextMazeId) {
					// generate the next maze:
					Maze nextmaze = Maze.buildSimpleMaze(nextMazeId, rndForMazeGen, config.worldSize, config.numberOfCorridors) ;
					seedMaze(nextmaze) ;
					mazes.add(nextmaze) ;
				}
				Maze nextmaze = mazes.get(nextMazeId) ;
				// now teleport the player to some free location near the Sun-shrine
				Pair<Integer,Integer> targetShrineLocation = new Pair<>(1,1) ;
				if (shrine.shrineType == ShrineType.SunShrine) {
					var size = nextmaze.world.length ;
					targetShrineLocation = new Pair<>(size-2,1) ;
				}
				// bug here, if there is no free location:
				var teleportLocation = nextmaze.getClosestFreeSquare(targetShrineLocation.fst, targetShrineLocation.snd) ;
				player.mazeId = nextmaze.id ;
				player.x = teleportLocation.fst ;
				player.y = teleportLocation.snd ;
				nextmaze.world[player.x][player.y] = player ;
				if (shrine.shrineType == ShrineType.MoonShrine) {
					return "> " + player.name + " touched a shrine and winked out. Is this a new place..?";
				}
				else {
					return "> " + player.name + " touched a shrine and winked out. Fleeing back already?";					
				}

			}
			// the shrine is not cleansed yet:
			var scrolls = player.itemsInBag(EntityType.SCROLL) ;
			if (scrolls.size() == 0) {
				return "> " + player.name + ", you don't have any scroll to cleanse the shrine.";
			}
			List<Scroll> holyScroll = scrolls.stream()
					. map(s -> (Scroll) s)
					. filter(s -> s.holy)
					. collect(Collectors.toList());
			
			if (holyScroll.size() > 0) {
				player.bag.remove(holyScroll.get(0));
				shrine.cleansed = true ;
				if (shrine.shrineType == ShrineType.ShrineOfImmortals) {
					if (player == frodo()) {
						status = GameStatus.FRODOWIN;
					}
					else {
						status = GameStatus.SMEAGOLWIN ;
					}
					player.score += 1000 ;
					return "> ROAR! " 
								+ player.name + " blessed an IMORTAL shrine. " 
								+ player.name + " WINS!" ;
				}
				else {
					player.score += 100 ;
					return "> Alkabra! " + player.name + " blessed a shrine." ;
				}

			} else {
				player.bag.remove(scrolls.get(0));
				return "> " + player.name + " read a scroll: Mellon! Nothing else happended.";

			}
			
		}
		throw new IllegalArgumentException() ;
	}
	
	
	
	
	
	char toChar(Entity e) {
		if (e == null) return '.' ; 
		switch(e.type) {
		    case WALL    : return '#' ;
		    case FRODO   : return '@' ;
		    case SMEAGOL : return '&' ;
		    case MONSTER : return 'm' ;
		    case HEALPOT : return '%' ;
		    case RAGEPOT : return '!' ;
		    case SCROLL  : return '?' ;
		    case SHRINE  : return 'S' ;
		}
		/*
		if (e instanceof Door) {
			Door d = (Door) e ;
			if (d.isOpen) return '-' ;
			return 'X' ;
		}
		*/
		throw new IllegalArgumentException() ;		
	}
	
	boolean isVisible(Player player, int mazeid, int x, int y) {
		if (player.dead()) return false ;
		if (player.mazeId != mazeid) return false ;
		float viewDistanceSq = config.viewDistance*config.viewDistance ;
		float dx = (float) (x - player.x) ;
		float dy = (float) (y - player.y) ;
		float distSq = dx*dx + dy*dy ;
		return distSq <= viewDistanceSq ;
	}
	
	/**
	 * Tiles that are visible to the players (so, visible to either Frodo
	 * or Smeagol).
	 */
	public List<Pair<Integer,IntVec2D>> visibleTiles() {
		List<Pair<Integer,IntVec2D>> visible = new LinkedList<>() ;
		for(int row = config.worldSize-1 ; 0<=row; row--) {
			for(int x = 0; x<config.worldSize; x++) {
				var location = new IntVec2D(x,row) ;
				if (isVisible(frodo(),frodo().mazeId,x,row)) {
					visible.add(new Pair<>(frodo().mazeId, location)) ; 
				}
				// BUG found by system test.
				// else if (config.enableSmeagol && isVisible(smeagol(),smeagol().mazeId,x,row)) {
				if (config.enableSmeagol && isVisible(smeagol(),smeagol().mazeId,x,row)) {
					visible.add(new Pair<>(smeagol().mazeId, location)) ;
				}
			}
		}
		return visible ;
	}
	
	public String showGameStatus() {
		StringBuffer z = new StringBuffer() ;
		z.append("[" + turnNr + "] Frodo hp: " + frodo().hp + "/" + frodo().hpMax
				+ ", AR:" + frodo().attackRating 
				+ ", score:" + frodo().score
				+ (frodo().rageTimer>0 ? " [ENRAGED]" : "")
				+ "\n#heal-pots:" + frodo().itemsInBag(EntityType.HEALPOT).size()
				+ ", #rage-pots:" + frodo().itemsInBag(EntityType.RAGEPOT).size()
				+ ", #scrolls:" + frodo().itemsInBag(EntityType.SCROLL).size()
				) ;
		if (config.enableSmeagol) {
			z.append("\nSmeagol hp: " + smeagol().hp + "/" + smeagol().hpMax
					+ ", AR:" + smeagol().attackRating 
					+ ", score:" + smeagol().score
					+ (smeagol().rageTimer>0 ? " [ENRAGED]" : "")
					+ "\n#heal-pots:" + smeagol().itemsInBag(EntityType.HEALPOT).size()
					+ ", #rage-pots:" + smeagol().itemsInBag(EntityType.RAGEPOT).size()
					+ ", #scrolls:" + smeagol().itemsInBag(EntityType.SCROLL).size()
					) ;
		}
		return z.toString() ;
	}
	
	@Override
	public String toString() {
		float viewDistanceSq = config.viewDistance*config.viewDistance ;
		StringBuffer z = new StringBuffer() ;
		var world = currentMaze(frodo()).world ;
		Entity[][] world2 = null ;
		if (config.enableSmeagol) {
			world2 = currentMaze(smeagol()).world ;
		}
		for(int row = config.worldSize-1 ; 0<=row; row--) {
			for(int x = 0; x<config.worldSize; x++) {
				boolean isVisible = 
						isVisible(frodo(),frodo().mazeId,x,row) 
						|| (config.enableSmeagol && world2==world && isVisible(smeagol(),smeagol().mazeId,x,row)) ;
				
				if (isVisible) {
					z.append(toChar(world[x][row])) ;					
				}
				else 
					z.append(" ") ;
			}
			if (world2 != null && world2 != world) {
				z.append("    ") ;
				for(int x = 0; x<config.worldSize; x++) {
					if (isVisible(smeagol(),smeagol().mazeId,x,row)) {
						z.append(toChar(world2[x][row])) ;					
					}
					else 
						z.append(" ") ;
				}
			}
			z.append("\n") ;
		}
		z.append(showGameStatus()) ;
		
		return z.toString() ;
	}
	
	
	
	/**
	 * An instance of the game with just simple console.
	 */
	public static void main(String[] args) {
		
		MiniDungeon dg = new MiniDungeon(new MiniDungeonConfig()) ;
		//aaadg.config.viewDistance = 4 ;
		while(dg.status == GameStatus.INPROGRESS) {
			dg.consolePrint(dg.toString()) ;
			dg.consolePrint("Commands Frodo: wasd | e:use-healpot | r:use-ragepot") ;
			dg.consolePrint("       Smeagol: ijkl | o:use-healpot | p:use-ragepot") ;
			if(dg.scanner == null) {
				dg.scanner = new Scanner(System.in) ;
			}
			String cmd = dg.scanner.nextLine() ;
			char[] commands = cmd.toCharArray() ;
			for (int c=0; c<commands.length; c++) {
				String msg = dg.doCommand(commands[c]) ;
				if (msg!=null && msg!="") dg.consolePrint(msg) ;
				if(dg.status != GameStatus.INPROGRESS) return ;
			}
		}
	}

}
