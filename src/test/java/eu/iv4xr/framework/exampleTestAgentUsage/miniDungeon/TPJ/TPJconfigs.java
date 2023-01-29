package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

public class TPJconfigs {
	
	public static MiniDungeonConfig MDconfig0() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 8;
		config.viewDistance = 4;
		config.numberOfMonsters = 1 ;
		config.randomSeed = 79371;
		return config ;
	}
	
	public static MiniDungeonConfig MDconfig1() {
		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 4;
		config.numberOfMonsters = 6 ;
		config.randomSeed = 79371;
		return config ;
	}

}
