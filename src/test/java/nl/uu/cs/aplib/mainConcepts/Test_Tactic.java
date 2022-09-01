package nl.uu.cs.aplib.mainConcepts;

import static nl.uu.cs.aplib.AplibEDSL.*;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.mainConcepts.SimpleState;

import static org.junit.jupiter.api.Assertions.*;

public class Test_Tactic {

    static class IntState extends SimpleState {
        int i;

        IntState(int i) {
            this.i = i;
        }
    }

    static IntState Int(int x) {
        return new IntState(x);
    }

    @Test
    public void test_getFirstEnabledActions() {
        var a0 = action("a0").on_(s -> ((IntState) s).i == 0).lift() ;
        var a1 = action("a1").on_(s -> ((IntState) s).i == 1).lift() ;
        var a2 = action("a2").on_(s -> ((IntState) s).i == 1).lift() ;
        var a3 = action("a3").on_(s -> ((IntState) s).i == 3).lift() ;
        var a4 = action("a4").on_(s -> ((IntState) s).i == 4).lift() ;

        assertFalse(FIRSTof(a0, a1, a2).getFirstEnabledActions(Int(1)).contains(a0));
        assertTrue(FIRSTof(a0, a1, a2).getFirstEnabledActions(Int(1)).contains(a1));
        assertFalse(FIRSTof(a0, a1, a2).getFirstEnabledActions(Int(1)).contains(a2));
        assertTrue(FIRSTof(a0, a1, a2).getFirstEnabledActions(Int(99)).isEmpty());

        assertFalse(ANYof(a0, a1, a2).getFirstEnabledActions(Int(1)).contains(a0));
        assertTrue(ANYof(a0, a1, a2).getFirstEnabledActions(Int(1)).contains(a1));
        assertTrue(ANYof(a0, a1, a2).getFirstEnabledActions(Int(1)).contains(a2));
        assertTrue(ANYof(a0, a1, a2).getFirstEnabledActions(Int(99)).isEmpty());

        assertTrue(SEQ(a0, a1, a2).getFirstEnabledActions(Int(0)).contains(a0));
        assertFalse(SEQ(a0, a1, a2).getFirstEnabledActions(Int(0)).contains(a1));
        assertFalse(SEQ(a0, a1, a2).getFirstEnabledActions(Int(0)).contains(a2));
        assertTrue(SEQ(a0, a1, a2).getFirstEnabledActions(Int(1)).isEmpty());
        assertTrue(SEQ(a0, a1, a2).getFirstEnabledActions(Int(99)).isEmpty());
    }

    @Test
    public void test_calcNextTactic() {
        var a0 = action("a0").lift();
        var a1 = action("a1").lift();
        var a2 = action("a2").lift();
        var a3 = action("a3").lift();
        var a4 = action("a4").lift();
        var a5 = action("a5").lift();

        var s1 = FIRSTof(a0, a1);
        var s2 = ANYof(s1, a2);

        a0.action.completed = false;
        assertTrue(a0.calcNextTactic() == a0);

        a0.action.completed = true;
        assertTrue(a0.calcNextTactic() == null);
        a1.action.completed = true;
        assertTrue(a1.calcNextTactic() == null);
        a2.action.completed = true;
        assertTrue(a2.calcNextTactic() == null);

        var s3 = ANYof(a3, a4);
        var s4 = SEQ(s2, s3, a5);
        assertTrue(a2.calcNextTactic() == s3);
        assertTrue(s2.calcNextTactic() == s3);

        a3.action.completed = true;
        assertTrue(a3.calcNextTactic() == a5);
        assertTrue(s3.calcNextTactic() == a5);

        var s5 = SEQ(a0, a1);
        var s6 = SEQ(s5, a2);
        a1.action.completed = true;
        assertTrue(a1.calcNextTactic() == a2);
        a2.action.completed = true;
        assertTrue(a2.calcNextTactic() == null);

    }

}
