package nl.uu.cs.aplib.mainConcepts;

import java.util.*;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvironmentInstrumenter;

import static org.junit.jupiter.api.Assertions.*;

public class Test_Environment {

    class MyEnv extends Environment {
        int x = 0;
        int y = 0;

        @Override
        public void refreshWorker() {
            x = 0;
            y = 0;
        }

        @Override
        protected Object sendCommand_(EnvOperation opr) {
            switch (opr.command) {
            case "incrx":
                x++;
                break;
            case "incry":
                y++;
            }
            return this;
        }

        MyEnv clone_() {
            MyEnv o = new MyEnv();
            o.x = x;
            o.y = y;
            return o;
        }
    }

    class MyInstrumenter implements EnvironmentInstrumenter {

        List<MyEnv> history = new LinkedList<>();

        @Override
        public void update(Environment env) {
            history.add(((MyEnv) env).clone_());
        }

        @Override
        public void reset() {
            history.clear();
        }

        MyEnv last() {
            return history.get(history.size() - 1);
        }

    }

    @Test
    public void test_debugmode() {
        var env = new MyEnv();
        assertFalse(env.debugmode);
        env.turnOnDebugInstrumentation();
        assertTrue(env.debugmode);
        env.turnOffDebugInstrumentation();
        assertFalse(env.debugmode);
    }

    @Test
    public void test_register_remove_instrumenter() {

        var env = new MyEnv();
        var instrumenter = new MyInstrumenter();

        env.registerInstrumenter(instrumenter);
        assertTrue(env.instrumenters.contains(instrumenter));

        env.removeInstrumenter(instrumenter);
        assertFalse(env.instrumenters.contains(instrumenter));
    }

    @Test
    public void test_instrumentation() {
        var env = new MyEnv();
        var instrumenter = new MyInstrumenter();
        env.registerInstrumenter(instrumenter);
        env.turnOnDebugInstrumentation();

        assertTrue(env.getLastOperation() == null);
        assertFalse(env.lastOperationWasRefresh());

        env.refresh();
        assertTrue(env.getLastOperation().command.equals("refresh"));
        assertTrue(env.lastOperationWasRefresh());
        assertTrue(instrumenter.history.size() == 1);
        assertTrue(instrumenter.last().x == 0 && instrumenter.last().y == 0);
        env.refresh();
        assertTrue(env.getLastOperation().command.equals("refresh"));
        assertTrue(env.lastOperationWasRefresh());
        assertTrue(instrumenter.history.size() == 2);
        assertTrue(instrumenter.last().x == 0 && instrumenter.last().y == 0);

        env.sendCommand("originId", "targetId", "incrx", null);
        assertTrue(env.getLastOperation().command.equals("incrx"));
        assertFalse(env.lastOperationWasRefresh());
        assertTrue(instrumenter.history.size() == 3);
        assertTrue(instrumenter.last().x == 1 && instrumenter.last().y == 0);

        env.sendCommand("originId", "targetId", "incry", null);
        assertTrue(env.getLastOperation().command.equals("incry"));
        assertFalse(env.lastOperationWasRefresh());
        assertTrue(instrumenter.history.size() == 4);
        assertTrue(instrumenter.last().x == 1 && instrumenter.last().y == 1);

        env.resetAndInstrument();
        assertTrue(env.getLastOperation() == null);
        assertFalse(env.lastOperationWasRefresh());
        assertTrue(instrumenter.history.size() == 0);

    }

}
