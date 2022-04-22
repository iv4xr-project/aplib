package nl.uu.cs.aplib.exampleUsages.miniDungeon;

import java.io.Console;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.Entity.*;
import nl.uu.cs.aplib.utils.Pair;

/**
 * A mini Dungeon crawl game. The dungeon is a 2D square consisting of NxN tiles. 
 * Outermost tiles are walls. Walls are also placed inside the world to form a simple
 * zig-zag maze. The player starts at location (1,1) and there is a goal-marker placed
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
	
	public enum GameStatus { INPROGRESS, PLAYERWIN, PLAYERLOST }
	
	public int worldSize = 20 ;
	public int numberOfCorridors = 4 ;
	public int maxBagSize = 2 ;
	public float viewDistance = 100f ;
	
	public Entity[][] world ;
	public Player player ;
	
	Random rnd = new Random(34793) ;
	
	public int turnNr = 0 ;
	
	public GameStatus status = GameStatus.INPROGRESS ;
	
	
	
	public MiniDungeon(int size, 
			int numberOfCorridors, 
			int numberOfMonsters, 
			int numberOfPots,
			int numberOfKeys) {
		if (size < 8) throw new IllegalArgumentException("size too small") ;
		if (numberOfCorridors > size/3) 
			throw new IllegalArgumentException("too many corridors") ;
		if (numberOfMonsters + numberOfPots > (size-2)*(size-2)/3) 
			throw new IllegalArgumentException("size many monsters or items") ;
		
		worldSize = size ;
		this.numberOfCorridors = numberOfCorridors ;
		
		world = new Entity[size][size] ;
		
		// walls around the arena:
		for(int i=0; i<size; i++) {
			world[0][i] = new Wall(0,i) ;
			world[size-1][i] = new Wall(size-1,i) ;
			world[i][0] = new Wall(i,0) ;
			world[i][size-1] = new Wall(size-1,i) ;
		}
		
		// walls that build the maze (just a simple maze) :
		int corridorWidth = size/numberOfCorridors ;
		int coridorX = size ;
		boolean alt = true ;
		for(int cr = 1 ; cr < numberOfCorridors; cr++) {
			coridorX = coridorX - corridorWidth ;
			if(alt) {
				for(int y=1; y<size-2;y++) {
					world[coridorX][y] = new Wall(coridorX,y) ;
				}
			}
			else {
				for(int y=size-1; 1<y ; y--) {
					world[coridorX][y] = new Wall(coridorX,y) ;
				}
			}
			alt = !alt ;
		}
		
		player = new Player(1,1) ;
		world[1][1] = player ;
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
		while(m<numberOfMonsters && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			Monster M = new Monster(sq.fst,sq.snd,m) ;
			world[sq.fst][sq.snd] = M ;
			m++ ;
		}
		
		// see potions:
		int h = 0 ;
		while(h<numberOfPots && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			HealingPotion H = new HealingPotion(sq.fst,sq.snd,h) ;
			world[sq.fst][sq.snd] = H ;
			h++ ;
		}
		
		// seed keys:
		
		int ky = 0 ;
		List<Key> keys = new LinkedList<>() ;
		while(ky < numberOfKeys && freeSquares.size()>0) {
			int k = rnd.nextInt(freeSquares.size()) ;
			var sq = freeSquares.remove(k) ;
			Key K = new Key(sq.fst,sq.snd,ky) ;
			world[sq.fst][sq.snd] = K ;
			keys.add(K) ;
			ky++ ;
		}
		// choose one golden key:
		Key goldenKey = keys.get(rnd.nextInt(keys.size())) ;
		goldenKey.goldenKey = true ;
	}
	
	public boolean showConsoleIO = true ;
	
	Scanner scanner ;
	
	void consolePrint(String s) {
		if (showConsoleIO) System.out.println(s) ;
	}
	
	public static String MOVEUP = "w" ;
	public static String MOVEDOWN = "s" ;
	public static String MOVELEFT = "a" ;
	public static String MOVERIGHT = "d" ;
	public static String USEITEM = "u" ;
	
	String readCommandFromConsole() {
		System.out.println("Commands: wasd | u:use-item") ;
		return null ;
	}
	
	List<Key> keysInBag() {
	   return player.bag.stream()
		. filter(i -> i instanceof Key)
		. map(i -> (Key) i)
		. collect(Collectors.toList()) ;
	}
	
	List<HealingPotion> potionsInBag() {
		   return player.bag.stream()
			. filter(i -> i instanceof HealingPotion)
			. map(i -> (HealingPotion) i)
			. collect(Collectors.toList()) ;
		}
	
	void removeFromWorld(Entity e) {
		world[e.x][e.y] = null ;
	}
	
	void attack(CombativeEntity attacker, CombativeEntity defender) {
		if(attacker.hp <= 0 || defender.hp <=0) throw new IllegalArgumentException() ;
		defender.hp = defender.hp - attacker.attackRating ;
		if (defender.hp <=0) {
			removeFromWorld(defender) ;
		}
	}
	
	String monstersMoves() {
		List<Monster> monsters = new LinkedList<>() ;
		String msg = "" ;
		for(int x=1; x<worldSize-1; x++) {
			for(int y=1; y<worldSize-1 ; y++) {
				var e = world[x][y] ;
				if (e instanceof Monster) {
					Monster m = (Monster) e ;
					// the player is next to m; attack the player:
					if((m.x == player.x && Math.abs(m.y - player.y) == 1)
						|| (m.y == player.y && Math.abs(m.x - player.x) == 1)
						) {
						attack(m,player) ;
						msg += "\n> Monster " + m.id + " attacked you!" ;
						if(player.hp <= 0) {
							status = GameStatus.PLAYERLOST ;
							msg  += ". You DIED." ;
							return msg ;
						}
					}
					else {
						// move randomly
						List<Pair<Integer,Integer>> candidates = new LinkedList<>() ;
						if(m.y < worldSize-1 && world[m.x][m.y+1]==null) {
							candidates.add(new Pair<>(m.x,m.y+1)) ;
						}
						if(1<m.y && world[m.x][m.y-1]==null) {
							candidates.add(new Pair<>(m.x,m.y-1)) ;
						}
						if(m.x < worldSize-1 && world[m.x+1][m.y]==null) {
							candidates.add(new Pair<>(m.x+1,m.y)) ;
						}
						if(1<m.x && world[m.x-1][m.y]==null) {
							candidates.add(new Pair<>(m.x-1,m.y)) ;
						}
						candidates.add(null) ;
						var sq = candidates.get(rnd.nextInt(candidates.size())) ;
						if (sq == null) 
							// the monster decides to stay where it is:
							continue ;
						// move the monster:
						world[m.x][m.y] = null ;
						m.x = sq.fst ;
						m.y = sq.snd ;
						world[m.x][m.y] = m ;
					}
				}
			}
		}
		return msg ;
	}
	
	
	public synchronized String doCommand(String command) {
		// if the game is over, the command is ignored:
		String msg = "" ;
		if (status != GameStatus.INPROGRESS) {
			return "> The game is already over." ;
		}
		// illegal moves wasd are ignored:
		int xx = player.x ;
		int yy = player.y ;
		if (command.equals(MOVEUP)) {
			if (player.y == worldSize-2) return "" ;
			yy++ ;
		}
		if (command.equals(MOVEDOWN)) {
			if (player.y == 1) return "" ;
			yy-- ;
		}
		if (command.equals(MOVERIGHT)) {
			if (player.x == worldSize-2) return "" ;
			xx++ ;
		}
		if (command.equals(MOVELEFT)) {
			if (player.x == 1) return "" ;
			xx-- ;
		}
		// handle use command:
		if (command.equals(USEITEM)) {
			var potions = potionsInBag() ;
			if(potions.size() == 0) {
				return "> Your don't have any potion to use." ;
			}
			else {
				var P = potions.get(0) ;
				player.bag.remove(P) ;
				player.hp = Math.min(player.hp + 5,player.hpMax) ;
				msg = "> That tastes good!" ;
			}
		}
		else {
			// then it mustbe wasd command:
			var target = world[xx][yy] ;
			if (target == null) {
				world[player.x][player.y] = null ;
				player.x = xx ;
				player.y = yy ;
				world[xx][yy] = player ;
			}
			else if (target instanceof HealingPotion || target instanceof Key){
				if (player.bag.size() == maxBagSize) {
					return "> Your bag has no space left." ;
				}
				player.bag.add(target) ;
				if(target instanceof HealingPotion)
					msg = "> Your found a small vial of greed liquid." ;
				else
					msg = "> Your found a key." ;
				world[player.x][player.y] = null ;
				player.x = xx ;
				player.y = yy ;
				world[xx][yy] = player ;
			}
			else if (target instanceof Monster){
				var m = (Monster) target ;
				msg = "> You attack monster " + m.id ;
				attack(player,m) ;
				if (m.hp <= 0) {
					msg += ". You killed it!" ;
				}
			}
			else if (target instanceof GoalFlag){
				var keys = keysInBag() ;
				if(keys.size() == 0) {
					return "> Your don't have any key to unlock the goal.";
				}
				var goldenKey = keys.stream()
						.filter(k -> k.goldenKey)
						.collect(Collectors.toList()) ;
				if(goldenKey.size()>0) {
					status = GameStatus.PLAYERWIN ;
					player.bag.remove(goldenKey.get(0)) ;
					turnNr++ ;
					return "> You unlock the goal. You WIN!";
				}
				else {
					msg = "> You tried a key, but sadly it does not fit the goal." ;
					player.bag.remove(keys.get(0)) ;
				}
			}
		}
		msg += monstersMoves() ;
		turnNr++ ;
		return msg ;
	}
	
	public void update() {
		if (status != GameStatus.INPROGRESS) return ;
		consolePrint(toString()) ;
		consolePrint("Commands: wasd | u:use") ;
		if(scanner == null) {
			scanner = new Scanner(System.in) ;
		}
		String cmd = scanner.nextLine() ;
		char[] commands = cmd.toCharArray() ;
		for (int c=0; c<commands.length; c++) {
			String msg = doCommand("" + commands[c]) ;
			consolePrint(msg) ;
			if(status != GameStatus.INPROGRESS) return ;
		}
	}
	
	char toChar(Entity e) {
		if (e == null) return '.' ; 
		if (e instanceof Wall) return '#' ;
		if (e instanceof Player) return '@' ;
		if (e instanceof Monster) return 'm' ;
		if (e instanceof HealingPotion) return '+' ;
		if (e instanceof Key) return 'k' ;
		if (e instanceof GoalFlag) return 'G' ;
		if (e instanceof Door) {
			Door d = (Door) e ;
			if (d.isOpen) return '-' ;
			return 'X' ;
		}
		throw new IllegalArgumentException() ;		
	}
	
	@Override
	public String toString() {
		float viewDistanceSq = viewDistance*viewDistance ;
		StringBuffer z = new StringBuffer() ;
		for(int row = worldSize-1 ; 0<=row; row--) {
			for(int x = 0; x<worldSize; x++) {
				float dx = (float) (x - player.x) ;
				float dy = (float) (row - player.y) ;
				float distSq = dx*dx + dy*dy ;
				if (distSq <= viewDistanceSq ) {
					z.append(toChar(world[x][row])) ;					
				}
				else 
					z.append(" ") ;
			}
			z.append("\n") ;
		}
		z.append("[" + turnNr + "] Player hp: " + player.hp + "/" + player.hpMax
				+ ", #pots:" + potionsInBag().size()
				+ ", #keys:" + keysInBag().size()
				+ ", AR:" + player.attackRating 
				) ;
		
		return z.toString() ;
	}
	
	
	public static void main(String[] args) {
		MiniDungeon dg = new MiniDungeon(20,4,5,4,3) ;
		//dg.viewDistance = 4 ;
		while(dg.status == GameStatus.INPROGRESS) {
			dg.update();
		}
	}
	

}
