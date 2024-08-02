package eu.iv4xr.framework.goalsAndTactics;

import java.util.Arrays;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

/**
 * A Q-state representation of MD-state. The representation is at a 
 * low level (it keeps more details). We use byte-array to keep its size small.
 */
public class MDQstate2 {
	
	byte[] state ;
		
	/**
	 * Construct a Q-state. 
	 * 
	 * <p> "windowSize" defines a rectangle area around the agent,
	 * where observation will be obtained. That is, only things within this rectangle,
	 * and moreover visible to the agent (within its visibility range) will be included
	 * in the constructed Q-state.
	 */
	MDQstate2(MiniDungeonConfig mdconfig, int windowSize, Iv4xrAgentState agentstate) {
		
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
