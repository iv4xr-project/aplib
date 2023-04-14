package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import java.util.LinkedList;
import java.util.List;

import eu.iv4xr.framework.mainConcepts.WorldEntity;

public class MyAgentStateExtended extends MyAgentState{

	public static WorldEntity  selectedItem = null;
	
	public static List<String> triedItems = new LinkedList<>() ;
	
} 
