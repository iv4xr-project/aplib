package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.io.Console;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.IntVec2D;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;
import nl.uu.cs.aplib.utils.Pair;

/**
 * A mini Dungeon crawl game. The dungeon is a 2D square consisting of NxN tiles. 
 * Outermost tiles are walls. Walls are also placed inside the world to form a simple
 * zig-zag maze. The player starts in the dungeon-center and there is a goal-marker placed
 * at location (N-2,1). The player wins if he/she manages to reach this goal.
 * 
 * <ul>
 * <li>Monsters are randomly placed in the dungeon. When a monster is adjacent to the player,
 * it will attack the player. Similarly, a player can attack an adjacent monster. If
 * a monster's hp reaches 0, it will be removed from the dungeon. If the player's hp reaches
 * 0 the player dies and the game is lost.
 * 
 * <li>There are keys and  healing potions randomly dropped in the dungeon. 
 * When the player moves to a tile with an item on it, and provided there is
 * still some space in the player's bag, the item is picked and automatically placed in 
 * the  bag. The default capacity of the bag is 2.
 * 
 * <li>The goal-marker must be 'unlocked' to actually be reached. This needs a key.
 * Among all keys dropped in the dungeon, there is one that would match the goal.
 * Which key it is, is not known to the player until he tries it on the goal. Trying
 * a key consumes it.
 * 
 * <li> The player can use a potion, if his/her bag contains one. This heals his hp with
 * some amount. Using a potion consumes it.
 * </ul>
 * 
 * You can run the main-method below to actually play the game. You can also control
 * the game from another class. You can do this 
 * 
 * 
 * @author iswbprasetya
 *
 */

public class MiniDungeon {
	
	public static class MiniDungeonConfig {
		public int worldSize = 20 ;
		public int numberOfCorridors = 4 ;
		public float viewDistance = 100f ;
		public long randomSeed = 34793 ;
		public int numberOfMonsters = 6 ;
		public int numberOfHealPots = 2 ;
		public int numberOfRagePots = 2 ;
		public int numberOfKeys = 3 ;
		public boolean enableSmeagol = true ;
		
		/**
		 * Producing a default configuration.
		 */
		public MiniDungeonConfig() { }
		
		@Override
		public String toString() {
			String s = "" ;
			s += "randomseed   : " + randomSeed ;
			s += "\nWorldsize    : " + worldSize ;
			s += "\n#corridors   : " +  numberOfCorridors ;
			s += "\nview distance: " +  viewDistance ;
			s += "\n#monsters    : " +  numberOfMonsters ;
			s += "\n#heal-pots   : " +  numberOfHealPots ;
			s += "\n#rage-potss  : " +  numberOfRagePots ;
			s += "\n#keys        : " +  numberOfKeys ;
			s += "\nSmeagol      : " +  enableSmeagol ;
			
			return s ;
		}
	}
	
	public enum GameStatus { INPROGRESS, FRODOWIN, SMEAGOLWIN, MONSTERSWIN }
	
	public MiniDungeonConfig config ;

	
	public Entity[][] world ;
	public List<Player> players = new LinkedList<>() ;
	public List<String> recentlyRemoved = new LinkedList<>() ;
	Random rnd  ;
	public int turnNr = 0 ;
	
	public GameStatus status = GameStatus.INPROGRESS ;
	
	public MiniDungeon(MiniDungeonConfig config) {
		this.config = config ;
		int size = config.worldSize ;
		if (size < 8) throw new IllegalArgumentException("size too small") ;
		if (config.numberOfCorridors > size/3) 
			throw new IllegalArgumentException("too many corridors") ;
		int numberOfItems = config.numberOfHealPots + config.numberOfRagePots + config.numberOfKeys ;
		if (config.numberOfMonsters + numberOfItems > (size-2)*(size-2)/3) 
			throw new IllegalArgumentException("too many monsters and items") ;
		
		rnd = new Random(config.randomSeed) ;
		world = new Entity[size][size] ;
		
		// walls around the arena:
		for(int i=0; i<size; i++) {
			world[0][i] = new Wall(0,i) ;
			world[size-1][i] = new Wall(size-1,i) ;
			world[i][0] = new Wall(i,0) ;
			world[i][size-1] = new Wall(i,size-1) ;
		}
		
		// walls that build the maze (just a simple maze) :
		int corridorWidth = size/config.numberOfCorridors ;
		int coridorX = size ;
		boolean alt = true ;
		for(int cr = 1 ; cr < config.numberOfCorridors; cr++) {
			coridorX = coridorX - corridorWidth ;
			if(alt) {
				for(int y=1; y<size-2;y++) {
					world[coridorX][y] = new Wall(coridorX,y) ;
				}
			}
			else {
				for(int y=size-2; 1<y ; y--) {
					world[coridorX][y] = new Wall(coridorX,y) ;
				}
			}
			alt = !alt ;
		}
		
		// place players:
		int center = size/2 ;
		for (int x=center-1 ; x<=center+1; x++) {
			boolean placed = false ;
			for (int y=center-1; y<=center+1; y++) {
				if ((world[x][y] == null)) {
					var frodo = new Frodo(x,y) ;
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
			world[1][1] = smeagol ;
			players.add(smeagol) ;
		}
		
		
		// place the goal-flag:
		world[size-2][1] = new GoalFlag(size-2,1) ;
		
	
		List<Pair<Integer,Integer>> freeSquares = new LinkedList<>() ;
		for(int x=1; x<size-1;x++) {
			for (int y=1; y<size-1; y++) {
				if(world[x][y] == null) {
					freeSquares.add(new Pair<Integer,Integer>(x,y)) ;
				}
			}
		}
		
		// seed monsters:
		int m = 0 ;
		while(m<config.numberOfMonsters && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			Monster M = new Monster(sq.fst,sq.snd,m) ;
			world[sq.fst][sq.snd] = M ;
			m++ ;
		}
		
		// seed heal-potions:
		int h = 0 ;
		while(h<config.numberOfHealPots && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			HealingPotion H = new HealingPotion(sq.fst,sq.snd,h) ;
			world[sq.fst][sq.snd] = H ;
			h++ ;
		}
		
		// seed rage-potions:
		int r = 0 ;
		while(r<config.numberOfRagePots && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			RagePotion R = new RagePotion(sq.fst,sq.snd,r) ;
			world[sq.fst][sq.snd] = R ;
			r++ ;
		}
		
		// seed keys:
		int ky = 0 ;
		List<Key> keys = new LinkedList<>() ;
		while(ky < config.numberOfKeys && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			Key K = new Key(sq.fst,sq.snd,ky) ;
			world[sq.fst][sq.snd] = K ;
			keys.add(K) ;
			ky++ ;
		}
		// choose one blessed key:
		Key blessedKey = keys.get(rnd.nextInt(keys.size())) ;
		blessedKey.blessed = true ;
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
	
		
	
	
	void removeFromWorld(Entity e) {
		world[e.x][e.y] = null ;
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
			removeFromWorld(defender) ;
		}
	}
	
	public Player getPlayer(String name) {
		for(var p : players) {
			if (p.equals(name)) return p ;
		}
		return null ;
	}
	
	boolean allPlayersDead() {
		for(var pq : players) {
			if (!pq.dead())  {
				return false ;
			}
		}
		return true ;
	}
	
	String monstersMoves() {
		List<Monster> monsters = new LinkedList<>();
		String msg = "";
		for (int x = 1; x < config.worldSize - 1; x++) {
			for (int y = 1; y < config.worldSize - 1; y++) {
				var e = world[x][y];
				if (e instanceof Monster) {
					Monster m = (Monster) e;

					// the player is next to m; attack the player:
					boolean thereWasPlayerToAttack = false ;
					for (Player player : players) {
						if (player.dead()) continue ;
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
					candidates.add(null);
					var sq = candidates.get(rnd.nextInt(candidates.size()));
					if (sq == null)
						// the monster decides to stay where it is:
						continue;
					// move the monster:
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
		msg += monstersMoves();
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
	
	/**
	 * Execute a command for the given player. The method returns null if the command
	 * cannot be executed (e.g. the player try to move off the board); the game state
	 * will not change in this case. If the command is successful, the game state is
	 * changed accordingly, and a msg to print is returned.
	 */
	String doCommandWorker(Player player, Command command) {
		
		if (player.dead())
			return null ;

		int xx = player.x;
		int yy = player.y;

		// illegal wasd are ignored
		switch (command) {
		case DONOTHING:
			return "> " + player.name + " cowers in fear.";

		case USEHEAL:
			var hpotions = player.itemsInBag(HealingPotion.class) ;
			if (hpotions.size() == 0) {
				return "> " + player.name + " does not have any heal-potion to use.";
			} else {
				var P = hpotions.get(0);
				player.bag.remove(P);
				player.hp = Math.min(player.hp + 5, player.hpMax);
				return "> That tastes good!";
			}
		case USERAGE:
			var rpotions = player.itemsInBag(RagePotion.class) ;
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

		var target = world[xx][yy];
		if (target == null) {
			// target is clear, move there
			world[player.x][player.y] = null;
			player.x = xx;
			player.y = yy;
			world[xx][yy] = player;
			return "" ;
		} 
		if (target instanceof Wall) {
			return null ;
		}
		if (target instanceof HealingPotion || target instanceof RagePotion || target instanceof Key) {
			if (player.bag.size() == player.maxBagSize) {
				return "> " + player.name + ", your bag has no space left.";
			}
			world[player.x][player.y] = null;
			player.x = xx;
			player.y = yy;
			world[xx][yy] = player;
			player.bag.add(target);
			recentlyRemoved.add(target.id) ;
			if (target instanceof HealingPotion)
				return "> " + player.name + " found a small vial of greed liquid.";
			if (target instanceof RagePotion)
				return "> " + player.name + " found a vial of liquid. It smells bad.";
			else
				return "> " + player.name + " found a key.";
			
		}
		if (target instanceof Monster) {
			var m = (Monster) target;
			String msg = "> " + player.name + " attacked monster " + m.id;
			attack(player, m);
			if (m.hp <= 0) {
				msg += ". The monster is killed!";
			}
			return msg ;
		} 
		if (target instanceof Player) {
			Player otherPlayer = (Player) target;
			String msg = "> " + player.name + " attacked " + otherPlayer.name;
			attack(player, otherPlayer);
			if (otherPlayer.hp <= 0) {
				msg += ". " + otherPlayer.name + " is killed!";
			}
			return msg ;
		} 
		if (target instanceof GoalFlag) {
			var keys = player.itemsInBag(Key.class) ;
			if (keys.size() == 0) {
				return "> " + player.name + ", you don't have any key to unlock the goal.";
			}
			var goldenKey = keys.stream()
					. map( k -> (Key) k)
					. filter(k -> k.blessed)
					. collect(Collectors.toList());
			if (goldenKey.size() > 0) {
				if (player == frodo()) {
					status = GameStatus.FRODOWIN;
				}
				else {
					status = GameStatus.SMEAGOLWIN ;
				}
				player.bag.remove(goldenKey.get(0));
				return "> " + player.name + " unlock the goal. " + player.name + " WIN!";
			} else {
				player.bag.remove(keys.get(0));
				return "> " + player.name + " tried a key, but sadly it does not fit the goal.";

			}
		}
		throw new IllegalArgumentException() ;
	}
	
	
	
	char toChar(Entity e) {
		if (e == null) return '.' ; 
		if (e instanceof Wall) return '#' ;
		if (e instanceof Frodo) return '@' ;
		if (e instanceof Smeagol) return '&' ;
		if (e instanceof Monster) return 'm' ;
		if (e instanceof HealingPotion) return '+' ;
		if (e instanceof RagePotion) return 'x' ;
		if (e instanceof Key) return 'k' ;
		if (e instanceof GoalFlag) return 'G' ;
		/*
		if (e instanceof Door) {
			Door d = (Door) e ;
			if (d.isOpen) return '-' ;
			return 'X' ;
		}
		*/
		throw new IllegalArgumentException() ;		
	}
	
	boolean isVisible(Player player, int x, int y) {
		if (player.dead()) return false ;
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
	public List<IntVec2D> visibleTiles() {
		List<IntVec2D> visible = new LinkedList<>() ;
		for(int row = config.worldSize-1 ; 0<=row; row--) {
			for(int x = 0; x<config.worldSize; x++) {
				if (isVisible(frodo(),x,row)) {
					visible.add(new IntVec2D(x,row)) ; 
				}
				else if (config.enableSmeagol && isVisible(smeagol(),x,row)) {
					visible.add(new IntVec2D(x,row)) ;
				}
			}
		}
		return visible ;
	}
	
	@Override
	public String toString() {
		float viewDistanceSq = config.viewDistance*config.viewDistance ;
		StringBuffer z = new StringBuffer() ;
		for(int row = config.worldSize-1 ; 0<=row; row--) {
			for(int x = 0; x<config.worldSize; x++) {
				boolean isVisible = 
						isVisible(frodo(),x,row) 
						|| (config.enableSmeagol && isVisible(smeagol(),x,row)) ;
				
				if (isVisible) {
					z.append(toChar(world[x][row])) ;					
				}
				else 
					z.append(" ") ;
			}
			z.append("\n") ;
		}
		z.append("[" + turnNr + "] Frodo hp: " + frodo().hp + "/" + frodo().hpMax
				+ ", AR:" + frodo().attackRating 
				+ (frodo().rageTimer>0 ? " [ENRAGED]" : "")
				+ "\n#heal-pots:" + frodo().itemsInBag(HealingPotion.class).size()
				+ ", #rage-pots:" + frodo().itemsInBag(RagePotion.class).size()
				+ ", #keys:" + frodo().itemsInBag(Key.class).size()
				) ;
		if (config.enableSmeagol) {
			z.append("\nSmeagol hp: " + smeagol().hp + "/" + smeagol().hpMax
					+ ", AR:" + smeagol().attackRating 
					+ (smeagol().rageTimer>0 ? " [ENRAGED]" : "")
					+ "\n#heal-pots:" + smeagol().itemsInBag(HealingPotion.class).size()
					+ ", #rage-pots:" + smeagol().itemsInBag(RagePotion.class).size()
					+ ", #keys:" + smeagol().itemsInBag(Key.class).size()
					) ;
		}
		
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
