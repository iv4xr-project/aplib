package eu.iv4xr.framework.goalsAndTactics;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

public class Test_ActionLevelQ {
	
	boolean withGraphics = true ;
	boolean supressLogging = false ;
	
	static class MDQstate2 {
		byte[] state ;
		
		MDQstate2(MiniDungeonConfig mdconfig, Iv4xrAgentState agentstate) {
			int N = mdconfig.worldSize ;
			int N__ = N-2 ;
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
			// N__* N__ tiles
			int numOfProperties = 7 ;
			int arraySize = numOfProperties + N__ * N__ ;
			state = new byte[arraySize] ;
			Arrays.fill(state, (byte) 0) ;
			state[0] = (byte) agent_x ;
			state[1] = (byte) agent_y ;
			state[2] = (byte) agent_mazeId ;
			state[3] = (byte) hp ;
			state[4] = (byte) numOfScrollsInBag ;
			state[5] = (byte) numOfHealPotsInBag ;
			state[6] = (byte) numOfRagePotsInBag ;
			for (var e : wom.elements.values()) {
				int e_mazeId = (Integer) e.properties.get("maze") ;
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
				if (code >0) {
					int e_x = (int) e.position.x ;
					int e_y = (int) e.position.z ;
					int index = numOfProperties + (e_x - 1) + N__ * (e_y - 1) ;
					state[index] = (byte) code ;
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

}
