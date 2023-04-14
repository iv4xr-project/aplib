package nl.uu.cs.aplib.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static nl.uu.cs.aplib.AplibEDSL.*;

import nl.uu.cs.aplib.agents.AutonomousBasicAgent;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.environments.ConsoleEnvironment;
import nl.uu.cs.aplib.environments.NullEnvironment;
import nl.uu.cs.aplib.mainConcepts.*;
import nl.uu.cs.aplib.multiAgentSupport.ComNode;
import nl.uu.cs.aplib.multiAgentSupport.Message;
import nl.uu.cs.aplib.multiAgentSupport.Message.MsgCastType;

public class Test_InterAgentCommunication {

    static class MyState extends State {
        int counter = 0;

        @Override
        public MyState setEnvironment(Environment env) {
            super.setEnvironment(env);
            return this;
        }

    }

    @Test
    public void test_sending_and_receiving() {
        // test for simple sending and recieving, in a system of three agents. We will
        // set them as subservient.

        var comNode = new ComNode();

        // agent-1 will be sending 3x messages, 1x singlecase, 1x broadcase, and 1x
        // rolecast:
        var state1 = new MyState().setEnvironment(new NullEnvironment());
        var agent1 = new AutonomousBasicAgent("D1", "teacher").attachState(state1).registerTo(comNode);
        var a0 = action("a0").do1((MyState S) -> {
            S.messenger.send("D1", 0, MsgCastType.SINGLECAST, "P1", "SC");
            return 0;
        }).lift();

        var a1 = action("a1").do1((MyState S) -> {
            S.messenger.send("D1", 0, MsgCastType.BROADCAST, "P1", "BC");
            return 0;
        }).lift();

        var a2 = action("a2").do1((MyState S) -> {
            S.messenger.send("D1", 0, MsgCastType.ROLECAST, "student", "RC");
            return 0;
        }).lift();

        var g1 = goal("g1").toSolve((Integer x) -> false).withTactic(SEQ(a0, a1, a2)).lift();
        agent1.setGoal(g1);

        // agent 2 will receive 1x, of category BC:
        var state2 = new MyState().setEnvironment(new NullEnvironment());
        var agent2 = new AutonomousBasicAgent("P1", "student").attachState(state2).registerTo(comNode);

        var b1 = action("b1").do1((MyState S) -> S.messenger().retrieve(M -> M.getMsgName().equals("BC")))
                .on_((MyState S) -> S.messenger().has(M -> M.getMsgName().equals("BC"))).lift();

        var g2 = goal("g2").toSolve((Message M) -> M.getMsgName().equals("BC")).withTactic(b1).lift();
        agent2.setGoal(g2);

        // agent 3 does nothing, though it will still register to the comNode, hence
        // receiving
        // messages:
        var state3 = new MyState().setEnvironment(new NullEnvironment());
        var agent3 = new AutonomousBasicAgent("P2", "unknown").attachState(state3).registerTo(comNode);

        // ------------------ round 1, agent-1 sends to agent-2, but agent-2 cant
        // consume the msg
        agent1.update();
        agent2.update();
        assertTrue(state2.messenger.size() == 1);
        assertTrue(state3.messenger.size() == 0);
        assertTrue(state2.messenger.has(M -> M.getMsgName().equals("SC")));
        assertFalse(state3.messenger.has(M -> M.getMsgName().equals("SC")));

        assertTrue(g2.getStatus().inProgress());

        // ------------------ round 2, agent-1 broadcasts, then agent-2 consumes the bc
        // msg and solves its goal
        agent1.update();
        assertTrue(state2.messenger.size() == 2);
        assertTrue(state3.messenger.size() == 1);
        assertTrue(state2.messenger.has(M -> M.getMsgName().equals("BC")));
        assertTrue(state3.messenger.has(M -> M.getMsgName().equals("BC")));

        agent2.update();
        assertTrue(state2.messenger.size() == 1);
        assertTrue(g2.getStatus().success());

        // ------------------ round 3 agent-1 role-case, but agent 2 has no more goal,
        // so agent-2 wont consume the msgs
        agent1.update();
        agent2.update();
        assertTrue(state2.messenger.size() == 2);
        assertTrue(state3.messenger.size() == 1);
        assertTrue(state2.messenger.has(M -> M.getMsgName().equals("RC")));
        assertFalse(state3.messenger.has(M -> M.getMsgName().equals("RC")));
    }

    @Test
    public void test_rejectedSend() {
        // test that when an agent A tries to send a single-cast msg to an agent that is
        // not registered to A's comNode, this should be rejected
        var comNode = new ComNode();

        // agent-1 will be sending 3x messages, 1x singlecase, 1x broadcase, and 1x
        // rolecast:
        var state1 = new MyState().setEnvironment(new NullEnvironment());
        var agent1 = new AutonomousBasicAgent("D1", "teacher").attachState(state1).registerTo(comNode);

        // create agent-2, NOT registered to the comNode
        var state2 = new MyState().setEnvironment(new NullEnvironment());
        var agent2 = new AutonomousBasicAgent("P1", "student").attachState(state2);

        var ack = agent1.messenger().send("D1", 0, MsgCastType.SINGLECAST, "P1", "blabla");
        assertFalse(ack.success());
    }
}
