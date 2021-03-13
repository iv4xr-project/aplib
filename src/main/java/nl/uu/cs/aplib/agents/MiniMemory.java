package nl.uu.cs.aplib.agents;

import java.util.LinkedList;
import java.util.List;

/**
 * A class to provide light weight state between tactics and between goals.
 */
public class MiniMemory {
    // to mark state e.g. S0,S1,S2 etc
    String state = "";

    // to store objects to be passed between functions that share this memory
    public List memorized = new LinkedList();

    public MiniMemory(String initstate) {
        state = initstate;
    }

    public boolean stateIs(String s) {
        return state.equals(s);
    }

    public void moveState(String s) {
        state = s;
    }

    public void memorize(Object o) {
        memorized.add(o);
    }

    public void clear() {
        memorized.clear();
    }
}
