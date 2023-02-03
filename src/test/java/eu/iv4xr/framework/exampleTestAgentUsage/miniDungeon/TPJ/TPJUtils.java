package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import java.util.logging.Level;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;

public class TPJUtils {
	
	/**
	 * Give the goal G to the agent and run it on MiniDungeon.
	 */
	static MyAgentState runAgent(TestAgent agent, 
			MiniDungeonConfig config,
			GoalStructure G, 
			int budget,
			int sleep,
			boolean stopAfterAgentDie,
			boolean withGraphics,
			boolean supressLogging,
			boolean verbosePrint) throws Exception {
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}

		MyAgentEnv env = new MyAgentEnv(app);
		MyAgentState state = new MyAgentState();
		
		agent. attachState(state) . attachEnvironment(env)  .setGoal(G) ;
		
		agent.addInv(new MD_invs().allInvs) ;
		agent.resetLTLs() ;
		
		Thread.sleep(1000);
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		int aterdieCount = 2 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			if (verbosePrint) System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			if (sleep>0) Thread.sleep(sleep); 
			if (k>=budget) break ;
			if (!state.agentIsAlive() && stopAfterAgentDie) {
				aterdieCount-- ;
			}
			if (aterdieCount<=0) break ;
			k++ ;
		}	
		System.out.println(">>> end of run.") ;
		//System.out.println(">>> Game status:" + state.gameStatus()) ;
		return state ;
	}

}
