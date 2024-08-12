package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

public class TPJconfigs {
	
	public static MiniDungeonConfig MDconfig0() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDconfig0" ;
		config.numberOfHealPots = 8;
		config.viewDistance = 4;
		config.numberOfMonsters = 1 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig MDconfig1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDconfig1" ;
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig MDconfig2() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDconfig2" ;
		config.numberOfHealPots = 4;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 8 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig Mini1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "Mini1" ;
		config.numberOfHealPots = 2 ;
		config.worldSize = 16 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40 ;
		config.numberOfMaze = 1 ;
		config.numberOfScrolls = 1 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 3 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig Mini2() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "Mini2" ;
		config.numberOfHealPots = 2 ;
		config.worldSize = 16 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40 ;
		config.numberOfMaze = 2 ;
		config.numberOfScrolls = 1 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 3 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig Small1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "Small1" ;
		config.numberOfHealPots = 2 ;
		config.worldSize = 16 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40 ;
		config.numberOfMaze = 1 ;
		config.numberOfScrolls = 3 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 4 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig Small2() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "Small2" ;
		config.numberOfHealPots = 2 ;
		config.worldSize = 16 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40 ;
		config.numberOfMaze = 2 ;
		config.numberOfScrolls = 3 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 4 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig SamiraLevel2() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "SamiraLevel2- 79371" ;
		config.numberOfHealPots = 4 ;
		config.worldSize = 20 ;		
		config.viewDistance = 4 ;
		//config.viewDistance = 40 ;
		//config.numberOfMaze = 2 ;
		config.numberOfMaze = 4 ;
		config.numberOfScrolls = 3 ;
		config.enableSmeagol = false ;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	// configs to test PvP 
	public static MiniDungeonConfig PVPConfig1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "PVPConfig1" ;
		config.numberOfMaze = 1 ;
		config.numberOfHealPots = 2 ;
		config.numberOfRagePots = 2 ;
		config.viewDistance = 4;
		config.numberOfMonsters = 2 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig PVPConfig2() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "PVPConfig2" ;
		config.numberOfMaze = 1 ;
		config.numberOfHealPots = 30 ;
		config.numberOfRagePots = 2 ;
		config.viewDistance = 4;
		config.numberOfMonsters = 0 ;
		config.randomSeed = 71;
		return config ;
	}
	
	// config to test combat with monsters and use of rage and aggravated state
	public static MiniDungeonConfig MonstersCombatConfig1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MonstersCombatConfig1" ;
		config.numberOfMaze = 1 ;
		config.numberOfHealPots = 4 ;
		config.numberOfRagePots = 8 ;
		config.viewDistance = 4;
		config.numberOfMonsters = 12 ;
		config.randomSeed = 79371;
		return config ;
	}
	
    // configs for immortal-shrine test
	public static MiniDungeonConfig M2Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "M2Config" ;
		config.numberOfMaze = 2 ;
		config.numberOfHealPots = 8;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig M3Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "M3Config" ;
		config.numberOfMaze = 3 ;
		config.numberOfHealPots = 8;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig M4Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "M4Config" ;
		config.numberOfMaze = 4 ;
		config.numberOfHealPots = 8;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig M6Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "M6Config" ;
		config.numberOfMaze = 6 ;
		config.numberOfHealPots = 8;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig M8Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "M8Config" ;
		config.numberOfMaze = 4 ;
		config.numberOfHealPots = 8;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	// config for survival experiment:
	
	public static MiniDungeonConfig BigAreaConfig() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "BigAreaConfig" ;
		config.numberOfMaze = 1 ;
		config.worldSize = 40 ;
		config.numberOfHealPots = 8;
		config.numberOfRagePots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	// configs for evaluating reinforcement learning
	public static MiniDungeonConfig MDQ1Config() {
		// just one maze, not so big, with no monster 
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDQ1Config" ;
		config.numberOfMaze = 1 ;
		config.worldSize = 14 ;
		config.numberOfHealPots = 8;
		config.numberOfScrolls = 2 ;
		config.numberOfMonsters = 0 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40;
		config.randomSeed = 791; // two scrolls, with the far one being holy
		//config.randomSeed = 781 ; 
		//config.randomSeed = 784; 
		//config.randomSeed = 119 ;
		config.enableSmeagol = false ;
		return config ;
	}
	
	// like MDQ1Config, with monsters
	public static MiniDungeonConfig MDQ2Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDQ2Config" ;
		config.numberOfMaze = 1 ;
		config.worldSize = 14 ;
		config.numberOfHealPots = 8;
		config.numberOfScrolls = 2 ;
		config.numberOfMonsters = 4 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40;
		config.randomSeed = 791; // two scrolls, with the far one being holy
		config.enableSmeagol = false ;
		return config ;
	}
	
	// like MDQ1Config, but a bit larger
	public static MiniDungeonConfig MDQ3Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDQ3Config" ;
		config.numberOfMaze = 1 ;
		config.worldSize = 20 ;
		config.numberOfHealPots = 8;
		config.numberOfScrolls = 2 ;
		config.numberOfMonsters = 0 ;
		config.numberOfCorridors = 3 ;
		config.viewDistance = 40;
		config.randomSeed = 787; // two scrolls, with the far one being holy
		config.enableSmeagol = false ;
		return config ;
	}
	
	// like MDQ1Config, but two mazes (and no monsters)
	public static MiniDungeonConfig MDQ4Config() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.configname = "MDQ4Config" ;
		config.numberOfMaze = 2 ;
		config.worldSize = 16 ;
		config.numberOfHealPots = 8;
		config.numberOfScrolls = 2 ;
		config.numberOfMonsters = 0 ;
		config.numberOfCorridors = 4 ;
		config.viewDistance = 40;
		config.randomSeed = 783; // two scrolls, with the far one being holy
		config.enableSmeagol = false ;
		return config ;
	}
	
	// just for trying out different configs:
	public static void main(String[] args) throws Exception {		
		MiniDungeonConfig config = M2Config() ;
		System.out.println(">>> Configuration:\n" + config) ;
		var app = new DungeonApp(config) ;
		//app.dungeon.showConsoleIO = false ;
		DungeonApp.deploy(app);
	}

}
