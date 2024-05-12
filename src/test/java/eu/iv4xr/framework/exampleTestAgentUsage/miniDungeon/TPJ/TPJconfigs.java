package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

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

}
