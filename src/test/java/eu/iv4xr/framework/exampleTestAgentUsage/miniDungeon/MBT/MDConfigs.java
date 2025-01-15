package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import java.util.Scanner;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

public class MDConfigs {
	
	static MiniDungeonConfig miniMD() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "mini" ;
		config.randomSeed = 79371;		
		config.viewDistance = 40;
		config.numberOfMaze = 1 ;
		config.numberOfMonsters = 2 ;
		config.numberOfScrolls = 2 ;
		config.worldSize = 15 ;
		//config.numberOfHealPots = 4 ;
		return config ;
	}
	
	static MiniDungeonConfig oneMazeStandardMD() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "M1" ;
		config.randomSeed = 79371;		
		config.viewDistance = 40;
		config.numberOfMaze = 1 ;
		config.numberOfHealPots = 4 ;
		return config ;
	}
	
	static MiniDungeonConfig ML1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "ML1" ;
		config.randomSeed = 79371;		
		config.viewDistance = 80;
		config.numberOfMaze = 1 ;
		config.worldSize = 40 ;
		config.numberOfHealPots = 4 ;
		return config ;
	}
	
	static MiniDungeonConfig ML2() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "ML2" ;
		config.randomSeed = 79371;		
		config.viewDistance = 80;
		config.worldSize = 40 ;
		//config.numberOfMaze = 1 ;
		config.numberOfHealPots = 4 ;
		return config ;
	}
	
	static MiniDungeonConfig ML5() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "ML5" ;
		config.randomSeed = 79371;		
		config.viewDistance = 80;
		config.numberOfMaze = 5 ;
		config.worldSize = 40 ;
		config.numberOfHealPots = 4 ;
		return config ;
	}
	
	static MiniDungeonConfig ML10() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "ML10" ;
		config.randomSeed = 79371;		
		config.viewDistance = 80;
		config.numberOfMaze = 10 ;
		config.worldSize = 40 ;
		config.numberOfHealPots = 4 ;
		return config ;
	}
	
	
	
	static public void main(String[] args) {
		MDRelauncher.agentRestart("Frodo",miniMD(),true,true) ;
	}

	

	

}
