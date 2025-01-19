package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import static nl.uu.cs.aplib.AplibEDSL.action;

import java.util.Arrays;

import eu.iv4xr.framework.extensions.pathfinding.Sparse2DTiledSurface_NavGraph.Tile;
import eu.iv4xr.framework.goalsAndTactics.AQalg;
import eu.iv4xr.framework.goalsAndTactics.BasicSearch;
import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.Command;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.Action;

/** 
 * Provides a factory method that produces a prepped action-level Q alg. 
 * The algortihm itself was implemented in {@link AQalg}.
 */
public class MyQAlg {
	
	
	/**
	 * An (abstract) representation of MD state for Q-algorithm. This one is suitable
	 * for low-level Q. We use byte-array to keep its size small.
	 */
	public static class MDQstate2 {
		
		public byte[] state ;
			
		/**
		 * Construct a Q-state. 
		 * 
		 * <p> "windowSize" defines a rectangle area around the agent,
		 * where observation will be obtained. That is, only things within this rectangle,
		 * and moreover visible to the agent (within its visibility range) will be included
		 * in the constructed Q-state.
		 */
		public MDQstate2(MiniDungeonConfig mdconfig, int windowSize, Iv4xrAgentState agentstate) {
			
			if (windowSize % 2 != 1) 
				throw new IllegalArgumentException("WindowSize should be an odd number.") ;
			
			int N = mdconfig.worldSize ;
			int N__ = N-2 ;
			int W = Math.min(N-2,windowSize) ;
			int half_W = (W-1)/2 ;
			var wom = agentstate.worldmodel ;
			// for now, using only Frodo:
			var frodo = wom.elements.get("Frodo") ;
			// 7 properties
			int agent_x = (int) frodo.position.x ;
			int agent_y = (int) frodo.position.z ;
			int agent_mazeId = (Integer) frodo.properties.get("maze") ;
			int hp = (Integer) frodo.properties.get("hp") ;
			int numOfScrollsInBag = (Integer) frodo.properties.get("scrollsInBag") ;
			int numOfHealPotsInBag = (Integer) frodo.properties.get("healpotsInBag") ;
			int numOfRagePotsInBag = (Integer) frodo.properties.get("ragepotsInBag") ;
			int numOfProperties = 7 ;
			int arraySize = numOfProperties + W * W ;
			state = new byte[arraySize] ;
			Arrays.fill(state, (byte) 0) ;
			state[0] = (byte) agent_x ;
			state[1] = (byte) agent_y ;
			state[2] = (byte) agent_mazeId ;
			state[3] = (byte) hp ;
			state[4] = (byte) numOfScrollsInBag ;
			state[5] = (byte) numOfHealPotsInBag ;
			state[6] = (byte) numOfRagePotsInBag ;
			int windowBottomLeft_x = Math.max(1, agent_x - half_W) ;
			int windowBottomLeft_y = Math.max(1, agent_y - half_W) ;
			int windowTopRight_x = Math.min(N__, agent_x + half_W) ;
			int windowTopRight_y = Math.min(N__, agent_y
					+ half_W) ;
			
			for (var e : wom.elements.values()) {
				var U = e.properties.get("maze") ;
				if (U == null) continue ;
				int e_mazeId = (Integer) U ;
				if (e_mazeId != agent_mazeId)
					continue ;
				int code = -1 ;
				if (e.id.startsWith("W")) {
					// wall
					code = 1 ;
				}
				else if (e.id.startsWith("H")) {
					code = 2 ;
				}
				else if (e.id.startsWith("R")) {
					code = 3 ;
				}
				else if (e.id.startsWith("S_")) {
					code = 4 ;
				}
				else if (e.id.startsWith("SS")) {
					code = 5 ;
				}
				else if (e.id.startsWith("SI")) {
					code = 6 ;
				}
				else if (e.id.startsWith("SM")) {
					// moonshrine
					var cleansed = (Boolean) e.properties.get("cleansed") ;
					if (cleansed)
						code = 5 ;
					else
						code = 7 ;
				}
				else if (e.id.startsWith("M")){
					int e_x = (int) e.position.x ;
					int e_y = (int) e.position.z ;
					if (Math.abs(e_x - agent_x) == 1 || Math.abs(e_y - agent_y) == 1) {
						code = 9 ;
					}
				}
				if (code >0) {
					int e_x = (int) e.position.x ;
					int e_y = (int) e.position.z ;
					if (windowBottomLeft_x <= e_x && e_x <= windowTopRight_x
							&& windowBottomLeft_y <= e_y && e_y <= windowTopRight_y) {
						int index = numOfProperties 
								+ (e_x - windowBottomLeft_x) 
								+ W * (e_y - windowBottomLeft_y) ;
						state[index] = (byte) code ;
					}
				}
			}		
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof MDQstate2)) return false ;
			var o_ = (MDQstate2) o ;
			
			return Arrays.equals(this.state, o_.state) ;
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(state) ;
		}
	}
	
	
	/**
	 * A function that specifies the value of a given MD agent-state. Suitable for
	 * low-level algorithms that operate at primitive action level.
	 */
	static float valueFunctionOfMDState2(BasicSearch alg, Iv4xrAgentState state) {
		if (alg.topGoalPredicate.test(state))
			return alg.maxReward ;
		if (alg.agentIsDead.test(state))
			return -100f ;
		var numOfScrollsInArea = (int) state.worldmodel.elements.values().stream()
			.filter(e -> e.type.equals("SCROLL"))
			.count();
		
		var frodo = state.worldmodel.elements.get("Frodo") ;
		
		var scrollsInBag = (Integer) frodo.properties.get("scrollsInBag") ;
		
		var healpotsInBag = (Integer) frodo.properties.get("healpotsInBag") ;
		
		var score = (Integer) frodo.properties.get("score") ;
		
		var hp = (Integer) frodo.properties.get("hp") ;
		
		//return 10f - (float) numOfScrollsInArea - 0.5f * (float) scrollsInBag ;
		var r1 = (float) (hp 
				+ score
				//+ (scrollsInBag ==1 ? 100 : (scrollsInBag ==2 ? -50 : 0))
				//+ (scrollsInBag ==1 ? 100 : 0)
				//+ 100*scrollsInBag 
				+ 1000*(8 - (2*numOfScrollsInArea + scrollsInBag))
				+ (healpotsInBag==1 ? 20  : 0))	
				;
		var cleansed = (int) state.worldmodel.elements.values().stream()
				.filter(e -> Utils.isMoonShrine(e) && e.getBooleanProperty("cleansed")) 
				.count();
		
		int mazeNr = Utils.mazeId(frodo) ;
		float r2 = (float) 1000*(cleansed + mazeNr) ;
		
		//System.out.println(">>>> reward = " + r ) ;
		return r1 + r2 ;
	}

	/**
	 * Low-level move-command for MD. This is for low-level Q.
	 */
	@SuppressWarnings("incomplete-switch")
	static Action move(Command cmd) {
		var A = action("" + cmd)
				.do1((MyAgentState S) ->  {
					S.env().action(S.worldmodel.agentId, cmd) ;
					return S ;
				})
				.on_((MyAgentState S) -> {
					  var tile = Utils.toTile(S.worldmodel.position) ;
					  Tile tile2 = null ;
					  switch(cmd) {
					    case MOVEUP   : tile2 = new Tile(tile.x,tile.y+1) ; break ;
					    case MOVEDOWN : tile2 = new Tile(tile.x,tile.y-1) ; break ;
					    case MOVERIGHT: tile2 = new Tile(tile.x+1,tile.y) ; break ;
					    case MOVELEFT : tile2 = new Tile(tile.x-1,tile.y) ; break ;
					  }
					  return ! Utils.isWall(S,tile2) ;
					}) ; 
		return A ;
	}

	/**
	 * Low-level use-pot command for MD. This is for low-level Q.
	 */
	@SuppressWarnings("incomplete-switch")
	static Action usePotion(Command cmd) {
		var A = action("" + cmd)
				.do1((MyAgentState S) ->  {
					S.env().action(S.worldmodel.agentId, cmd) ;
					return S ;
				})
				.on_((MyAgentState S) -> {	
					  var frodo = S.worldmodel.elements.get("Frodo") ;
					  switch(cmd) {
					    case USEHEAL :
					    	return (Integer) frodo.properties.get("healpotsInBag") > 0 ;
					    case USERAGE :
					    	return (Integer) frodo.properties.get("ragepotsInBag") > 0 ;
					  }
					  return false ;
					}) ; 
		return A ;
	}
	
	/**
	 * Configure some basic params of the Q-alg. 
	 */
	static void basicConfigure(String agentId, 
			MiniDungeonConfig config, 
			boolean soundOn,
			boolean graphicsOn,
			int delayBetweenUpdates,
			BasicSearch alg) {
		
		BasicSearch.DEBUG = false ;

		alg.agentConstructor = dummy -> {
			try {
				return MDRelauncher.agentRestart(agentId, config, soundOn, graphicsOn) ;
			}
			catch(Exception e) {
				System.out.println(">>> FAIL to create a agent.") ;
				return null ;
			}
		} ;
		
		alg.closeEnv = dummy -> {
			// don't need to do anything special for closing:
			//System.out.println(">>> DISPOSING MD") ;
			return null ;
		} ;
		
		alg.maxReward = 1000000 ;
		
		// we'll fix the goal at winning the game, so cleaning the immortal shrine:
		alg.topGoalPredicate = state -> {
			//System.out.println(">>> WOM = " + state.worldmodel) ;
			var env_ = (MyAgentEnv) state.env();
			int lastMaze = env_.app.dungeon.config.numberOfMaze - 1 ;
			String immortalShrineId = "SI" + lastMaze ;
			var shrineImmortal = state.worldmodel.elements.get(immortalShrineId) ;
			return shrineImmortal != null && shrineImmortal.getBooleanProperty("cleansed") ;
		} ;
		
		alg.agentIsDead = state ->  ! ((MyAgentState) state).agentIsAlive() ;
		
		alg.delayBetweenAgentUpateCycles = delayBetweenUpdates ;
	}
	
	
	/**
	 * Construct a configured instance of action-level Q alg.
	 * @param config
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static AQalg mkActionLevelQ(MiniDungeonConfig config,
			boolean soundOn,
			boolean graphicsOn,
			int delayBetweenUpdates) {
		AQalg alg = new AQalg() ;
		// will use Frodo for Q:
		basicConfigure("Frodo",config,soundOn,graphicsOn,delayBetweenUpdates,alg) ;
		alg.availableActions.put("w", move(Command.MOVEUP)) ;
		alg.availableActions.put("a", move(Command.MOVELEFT)) ;
		alg.availableActions.put("s", move(Command.MOVEDOWN)) ;
		alg.availableActions.put("d", move(Command.MOVERIGHT)) ;
		alg.availableActions.put("e", usePotion(Command.USEHEAL)) ;
		alg.availableActions.put("r", usePotion(Command.USERAGE)) ;
		
		// attach a function that converts the agent state to a Q-state:
		int observeWindowSize = 9 ;
		alg.getQstate = (trace,currentState) -> {
					var env = (MyAgentEnv) alg.agent.env() ;
					return new MDQstate2(env.app.dungeon.config, observeWindowSize, (Iv4xrAgentState) currentState) ;
		} ;
				
		alg.stateValueFunction = state -> valueFunctionOfMDState2(alg,state) ;

		return alg ;	
	}
	
}
