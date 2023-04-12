package eu.iv4xr.framework.exampleTestAgentUsage;

import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.*;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig ;

public class TestCoba {

	
	@Test
	public void test1() {
		try {
			var config = new MiniDungeonConfig() ;
			var app = new DungeonApp(config) ;
			app.headless = true ;
			app.soundOn = false ;
			//DungeonApp.deploy(app) ;
		}
		catch(Exception e) {
			Assertions.fail() ;
		}
	}
}
