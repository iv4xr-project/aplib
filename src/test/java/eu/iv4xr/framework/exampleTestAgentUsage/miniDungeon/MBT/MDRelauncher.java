package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;

/**
 * Provide a method to efficiently re-launch MD, and create an instance
 * of a test-agent bound to it.
 */
public class MDRelauncher {

	/** 
	 * Singleton to hold an instance of MD
	 */
	static DungeonApp miniDungeonInstance = null ;

	/**
	 * Restart the instance of MiniDungeon in {@link miniDungeonInstance} (this can possibly
	 * create a new instance of MiniDungeon, if the config is different than the current one).
	 * Then create and return a new test-agent, bound to the instance.
	 */
	public static TestAgent agentRestart(String agentId, 
			MiniDungeonConfig config,
			boolean withSound,
			boolean withGraphics 
			)  {
		
		if (miniDungeonInstance==null ||! miniDungeonInstance.dungeon.config.toString().equals(config.toString())) {
			// there is no MD-instance yet, or if the config is different than the config of the
			// running MD-instance, then we create a fresh MD instance:
			DungeonApp app = null ;
			try {
				app = new DungeonApp(config);
			}
			catch(Exception e) {
				System.out.println(">>>> failed to launch MD!\n") ;
				miniDungeonInstance = null ;
				return null ;
			}
			// setting sound on/off, graphics on/off etc:
			app.soundOn = withSound ;
			app.headless = ! withGraphics ;
			if(withGraphics) 
				DungeonApp.deploy(app);	
			System.out.println(">>> LAUNCHING a new instance of MD") ;
			miniDungeonInstance = app ;
		}
		else {
			// if the config is the same, we just reset the state of the running MD:
			miniDungeonInstance.keyPressedWorker('z');
			System.out.println(">>> RESETING MD ---- ") ;
		}
		System.out.println(">>> creating fresh test-agent ") ;
		var agent = new TestAgent(agentId, "tester"); 	
		agent.attachState(new MyAgentState())
			 .attachEnvironment(new MyAgentEnv(miniDungeonInstance)) ;
		
		// give initial state update to set it up
		agent.state().updateState(agent.getId()) ;
		return agent ;
	}

}
