package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon;

import static nl.uu.cs.aplib.utils.ConsoleUtils.hitAKeyToContinue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;

/**
 * 
 * Test closing/disposal of MD.
 *
 */
public class Test_ClosingMD {
	
	@Test
	/**
	 * Testing the disposal of MD. We do it like this:
	 * 
	 * <pre>
	 *    Window win = SwingUtilities.getWindowAncestor(app);
	 *	  win.dispose();
	 * </pre>
	 * 
	 * We can't really check that it is 'disposed'.
	 * Disposing a container would release the resources it holds. If there is
	 * no reference left to the container, it will be eventually be removed by
	 * the GC.
	 * 
	 * <p>Below we only test if once disposed, MD is not visible anymore.
	 */
	public void test_closingMiniDungeon() throws Exception {
		
		var interactive = false ;
		
		DungeonApp app = new DungeonApp(new MiniDungeonConfig());
		app.soundOn = false;
		DungeonApp.deploy(app);		
		
		assertTrue(app.isShowing()) ;

		hitAKeyToContinue(interactive) ;
		
		Window win = SwingUtilities.getWindowAncestor(app);
		win.dispose();
		
		hitAKeyToContinue(interactive) ;
		assertTrue(!app.isShowing()) ;
		
		app = new DungeonApp(new MiniDungeonConfig());
		app.soundOn = false;
		DungeonApp.deploy(app);		
		assertTrue(app.isShowing()) ;

		hitAKeyToContinue(interactive) ;
		
		win = SwingUtilities.getWindowAncestor(app);
		win.dispose();
		
		hitAKeyToContinue(interactive) ;
		assertTrue(!app.isShowing()) ;
	}

}
