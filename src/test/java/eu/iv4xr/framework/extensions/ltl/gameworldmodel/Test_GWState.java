package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Test_GWState {
	
	@Test
	public void test_equals_GWObject() {
		
		var b0 = LabRecruitsModel.mkClosedDoor("b0") ;
		var b0b = LabRecruitsModel.mkClosedDoor("b0") ;
		var b0c = LabRecruitsModel.mkOpenDoor("b0") ;
		var b1 = LabRecruitsModel.mkClosedDoor("b1") ;
		
		assertTrue(b0.equals(b0)) ;
		assertTrue(b0.equals(b0b)) ;
		assertFalse(b0.equals(b0c)) ;
		assertFalse(b0.equals(null)) ;
		assertFalse(b0.equals(b1)) ;
		
		b0b.destroyed = true ;
		assertFalse(b0.equals(b0b)) ;
			
	}
	
	@Test
	public void test_equals_GWState() {
		var b0 = LabRecruitsModel.mkClosedDoor("b0") ;
		var b0b = LabRecruitsModel.mkClosedDoor("b0") ;
		var b1 = LabRecruitsModel.mkClosedDoor("b1") ;
		var b2 = LabRecruitsModel.mkClosedDoor("b2") ;
		
		GWState st1 = new GWState() ;
		st1.currentAgentLocation = "x" ;
		st1.addObjects(b0,b1);
			
		GWState st2 = new GWState() ;
		st2.currentAgentLocation = "x" ;
		st2.addObjects(b0b,b1);
		
		GWState st3 = new GWState() ;
		st3.currentAgentLocation = "x" ;
		st3.addObjects(b0b,b1,b2);
		
		GWState st4 = new GWState() ;
		st4.currentAgentLocation = "x" ;
		st4.addObjects(b0b,b2);

		assertTrue(st1.equals(st2)) ;
		assertFalse(st1.equals(st3)) ;
		assertFalse(st1.equals(st4)) ;

		st2.currentAgentLocation = "y" ;
		assertFalse(st1.equals(st2)) ;
	}

}
