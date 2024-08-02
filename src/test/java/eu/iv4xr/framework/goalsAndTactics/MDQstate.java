package eu.iv4xr.framework.goalsAndTactics;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import eu.iv4xr.framework.mainConcepts.Iv4xrAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;

/**
 * A Q-state representation of MD-state. The representation is kept at
 * a pretty high level.
 */
public class MDQstate {
	
	public List<String> scrolls = new LinkedList<>() ;
	public List<String> openShrines = new LinkedList<>() ;
	public int numberOfScrollsInbag = 0 ;
	public boolean alive ;
	
	MDQstate() { }
	
	MDQstate(Iv4xrAgentState state) {
		var frodo = state.worldmodel.elements.get("Frodo") ;
		alive = ((Integer) frodo.properties.get("hp")) > 0 ;
		numberOfScrollsInbag = (Integer) frodo.properties.get("scrollsInBag") ;
		scrolls = state.worldmodel.elements.values().stream()
				.filter(e -> Utils.isScroll(e))
				.map(e -> e.id) 
				.collect(Collectors.toList()) ;
		scrolls.sort((s1,s2) -> s1.compareTo(s2)) ;
		openShrines = state.worldmodel.elements.values().stream()
				.filter(e -> Utils.isShrine(e))
				.filter(e -> (Boolean) e.properties.get("cleansed"))
				.map(e -> e.id) 
				.collect(Collectors.toList()) ;
		openShrines.sort((s1,s2) -> s1.compareTo(s2)) ;
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof MDQstate)) return false ;
		MDQstate o_ = (MDQstate) o ;
		return this.scrolls.equals(o_.scrolls)
				&& this.openShrines.equals(o_.openShrines)
				&& this.numberOfScrollsInbag == o_.numberOfScrollsInbag 
				&& this.alive == o_.alive ;
	}
	
	@Override
    public int hashCode() {
        return scrolls.hashCode() 
        		+ 3*openShrines.hashCode() 
        		+ 31*numberOfScrollsInbag 
        		+ (alive?1:0) ;
    }

}
