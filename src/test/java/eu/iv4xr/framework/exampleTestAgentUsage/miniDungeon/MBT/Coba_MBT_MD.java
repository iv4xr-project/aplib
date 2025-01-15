package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.mbt.MBTRunner;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;

public class Coba_MBT_MD {

	// just a simple test to try out
	// @Test
	public void coba0() throws Exception {

		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40;
		config.randomSeed = 79371;
		System.out.println(">>> Configuration:\n" + config);
		var agent = MDRelauncher.agentRestart("Frodo", config, false, true); // "Smeagol"

		// if (supressLogging) {
		// Logging.getAPLIBlogger().setLevel(Level.OFF);
		// }

		var mymodel = MBT_MD_Model.MD_model0(200, false);
		var runner = new MBTRunner<MyAgentState>(mymodel);
		// runner.rnd = new Random() ;

		// give initial state update, to setup the agent's initial state
		// agent.state().updateState(agent.getId()) ;

		var results = runner.generateTestSequence(agent, 50);

		System.out.println(runner.showCoverage());
		System.out.println(">>> failed actions:" + MBTRunner.getFailedActionsFromSeqResult(results));
		System.out.println(">>> postcond violations:" + MBTRunner.getViolatedPostCondsFromSeqResult(results));
	}

	// just a simple test to try out
	@Test
	public void coba1() throws Exception {

		MiniDungeonConfig config = new MiniDungeonConfig();
		config.numberOfHealPots = 4;
		config.viewDistance = 40;
		config.randomSeed = 79371;
		config.numberOfMaze = 2;
		System.out.println(">>> Configuration:\n" + config);

		var mymodel = MBT_MD_Model.MD_model1(200, true, true);
		var runner = new MBTRunner<MyAgentState>(mymodel);
		runner.inferTransitions = true;
		// runner.rnd = new Random() ;

		// runner.actionSelectionPolicy = ACTION_SELECTION.Q ;

		var results = runner.generate(dummy -> MDRelauncher.agentRestart("Frodo", config, false, true), 20, 60);

		System.out.println(runner.showCoverage());
		System.out.println(">>> failed actions:" + MBTRunner.getFailedActionsFromSuiteResults(results));
		System.out.println(">>> postcond violations:" + MBTRunner.getViolatedPostCondsFromSuiteResults(results));

		mymodel.saveDot("./tmp/myMDmodel.dot");
	}

}
