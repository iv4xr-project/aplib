package nl.uu.cs.aplib.mainConcepts;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import nl.uu.cs.aplib.Logging;

/**
 * 
 * In aplib, agents (instances of {@link BasicAgent} or its subclasses) are used
 * to interact and control an environment. This class is a root class for
 * representing this environment. This class is just a template, and is meant to be
 * <b>extended/subclassed</b>. For a minimalistic example implementation, see
 * the class {@link nl.uu.cs.aplib.environments.ConsoleEnvironment}.
 * 
 * <p> Let's call the environment that we try to interact with
 * through this class <i>the actual environment</i>. To interact an agent
 * (or anything else) invoke the method {@link #sendCommand(String, String, String, Object)},
 * specifying the name of the command and an object to be sent along as a
 * parameter, if we have one. The method returns an object that the actual
 * environment sends back as the reply to the given command.
 * 
 * <p>Being just a template, this class does not know
 * what commands are available, nor does it know how to send it to the actual
 * environment. This depends on the concrete actual environment that you use.
 * Therefore, it is your responsibilty to implement sendCommand in your
 * concrete subclass of this class Environment. More precisely you need
 * to implement the method {@link #sendCommand_(EnvOperation)} (rather than
 * the previously said sendCommand).
 * 
 * <p>Additionally you need to provide implementations of:
 * 
 *    <ol>
 *    
 *    <li> {@link #observe(String)} to ask the real environment to send back
 *    information about its state. It is recommended that you implement this
 *    method by calling {@link Environment#sendCommand(String, String, String, Object)},
 *    passing to it the right parameters that correspond to an 'observe' command.
 *    
 *    <li> {@link #resetWorker()} to reset the state of your actual environment.
 *    </ol>
 * 
 * <p>
 * An implementation of Environment can in principle provide more methods to
 * access the real environment. It is recommended that you implement them by
 * translating them to calls to
 * {@link Environment#sendCommand(String, String, String, Object)}. If you
 * implement your own, by-passing {@code sendCommand}, make sure that you call
 * {@link #instrument(EnvOperation)}, or else calls to your custom methods will
 * not be seen by the instrumenters (see below).
 * 
 * <p>
 * The class also has a <b>debug=instrumentation</b> facility. You can register
 * instances of the class {@link EnvironmentInstrumenter} to an Environment.
 * Whenever an agent invokes a method ({@code refresh()} and
 * {@code sendCommand(...)}) of the Environment, and if the Environment's
 * debug-mode is turned on, it will trigger a call to the {@code update()}
 * method of each registered instrumenter, giving it an opportunity to peek into
 * the Environment state, e.g. for the purpose of checking some correctness
 * property.
 * 
 * @author wish
 *
 */
public class Environment {

    /**
     * Providing a lock to the entire Environment. This is used by
     * {@link BasicAgent} to make sure that agents get exclusive access to an
     * Environment when they want to do something with it.
     */
    ReentrantLock lock = new ReentrantLock();

    /**
     * Create an instance of this environment.
     */
    public Environment() {
    }


    /**
     * This will will call [@link #resetWorker()} and additionally reset this
     * Environment's active instrumenters.
     */
    public final void resetAndInstrument() {
        logger.info("Environment reset is called.");
        lastOperation = null;
        resetWorker();
        for (EnvironmentInstrumenter I : instrumenters) {
            I.reset();
        }
    }

    /**
     * This will reset the actual environment. By reset we mean to put it back in
     * some initial state. In reality this may involve re-deploying the environment.
     * It is your responsibility to make sure that after calling this method the
     * environment is indeed available and is in a legal initial state.
     * 
     * <p>
     * This implementation does not do anything; you should override this when
     * implementing your own specific Environment.
     */
    public void resetWorker() {
    }

    /**
     * Send the specified command to the environment. This method also anticipates
     * that the environment is populated by multiple reactive entities, so the
     * command includes an ID if addressing a specific entity in the environment is
     * needed.
     * 
     * @param invokerId            A unique ID identifying the invoker of this
     *                             method, e.g. in the format agentid.actionid.
     * @param targetId             The unique ID of the object in the real
     *                             environment to which the command is directed.
     * @param command              The name of the command.
     * @param arg                  The arguments to be passed along with the
     *                             command.
     * @param expectedTypeOfResult If non-null, specifies the expected type of the
     *                             result of this command.
     * @return an object returned by the real environment as the result of the
     *         command, if any. If the expectedTypeOfResult parameter is not null,
     *         then this returned object should be an instance of the class
     *         specified by the expectedTypeOfResult parameter.
     * 
     *         <p>
     *         The method may also throws a runtime exception.
     */
    public Object sendCommand(String invokerId, String targetId, String command, Object arg,
            Class expectedTypeOfResult) {
        var cmd = new EnvOperation(invokerId, targetId, command, arg, expectedTypeOfResult);
        var response = sendCommand_(cmd);
        cmd.result = response;
        instrument(cmd);
        return response;
    }

    /**
     * A simplified version of the other sendCommand where the expectedTypeOfResult
     * parameter is left unspecified (set to null). When using this method the agent
     * is assumed to somehow know what the runtime type of the returned object would
     * be.
     */
    public Object sendCommand(String invokerId, String targetId, String command, Object arg) {
        return sendCommand(invokerId, targetId, command, arg, null);
    }
    
    /**
     * Ask the real environment to return an observation (the state of the real 
     * environment) from the perspective of the invokerId. E.g. this id may
     * identifies a simulated player in a game. The returned observation would
     * then be the game state as seen by the player rather than the state of
     * the entire game.
     * 
     * <p>You have to provide a concrete implementation of this method. Do
     * implement it by calling the method {@link #sendCommand(String, String, String, Object)},
     * passing to it the right parameters that is needed to make the environment
     * to send back an observation.
     */
    public Object observe(String invokerId) {
    	throw new UnsupportedOperationException();
    }

    /**
     * Override this method to implement an actual Environment.
     * 
     * @param cmd representing the command to send to the real environment.
     * @return an object that the real environment sends back as the result of the
     *         command, if any. If the cmd specifies what the expected type of the
     *         returned object, then this method should guarantee that the returned
     *         object is indeed an instance of the specified type.
     */
    protected Object sendCommand_(EnvOperation cmd) {
        throw new UnsupportedOperationException();
    }

    /**
     * An instance of this class is used to record a command that an agent sent to
     * the real environment through an instance of {@link Environment}. This is so
     * that the Environment can store what was the last command sent for the purpose
     * of instrumentation.
     */
    static public class EnvOperation {

        /**
         * A unique id identifying whoever invokes this operation. E.g.
         * agentid.actionid.
         */
        public String invokerId;

        /**
         * A unique id identifying the entity in the real environment to which this
         * operation is targeted.
         */
        public String targetId;

        /**
         * The name of the command that this operation represents.
         */
        public String command;

        /**
         * The argment of the operation, if any.
         */
        public Object arg;

        /**
         * If not null, thus specifies the expected type of result of this operation.
         */
        public Class expectedTypeOfResult;

        /**
         * Used to store the result of the operation, if any. If the field
         * expectedTypeOfResult is not null, then the runtime type of result is expected
         * to match (equal of a subclass of) the type specified by expectedTypeOfResult.
         */
        public Object result = null;

        public EnvOperation(String invokerId, String targetId, String command, Object arg, Class expectedTypeOfResult) {
            this.invokerId = invokerId;
            this.targetId = targetId;
            this.command = command;
            this.arg = arg;
            this.expectedTypeOfResult = expectedTypeOfResult;
        }
    }

    /**
     * When true, this will enable some tracking to help debugging.
     */
    boolean debugmode = false;

    protected Logger logger = Logging.getAPLIBlogger();

    /**
     * To turn on debug-instrumentation facility. Return this environemnt to allow
     * the method to be used in the Fluent Interface style.
     */
    public Environment turnOnDebugInstrumentation() {
        debugmode = true;
        return this;
    }

    public Environment turnOffDebugInstrumentation() {
        debugmode = false;
        return this;
    }

    /**
     * Return the last "operation" that this environment does. This is either a
     * "refresh" (e.g. invoked by an agent), or a command this environment sends to
     * the real environment.
     * 
     * <p>
     * Note that the tracking of last-operation is only enabled when the debug-mode
     * of this environment is turned on.
     */
    public EnvOperation getLastOperation() {
        return lastOperation;
    }

    /**
     * Will record the id of the party that trigger this instrumentation step and
     * then call the update() method of every instrumenter registered to this
     * environement.
     * 
     * @param id An unique id of whoever trigger this instrumentation step.
     */
    protected void instrument(EnvOperation operation) {
        if (debugmode) {
            lastOperation = operation;
            for (EnvironmentInstrumenter I : instrumenters)
                I.update(this);
        }
    }

    EnvOperation lastOperation = null;

    List<EnvironmentInstrumenter> instrumenters = new LinkedList<EnvironmentInstrumenter>();

    /**
     * Register the given instrumenter to this environment. This will cause the
     * update() method of the instrumenter to be invoked whenether some agent invoke
     * some method of this environment.
     */
    public Environment registerInstrumenter(EnvironmentInstrumenter I) {
        if (I == null)
            throw new IllegalArgumentException();
        instrumenters.add(I);
        return this;
    }

    public Environment removeInstrumenter(EnvironmentInstrumenter I) {
        instrumenters.remove(I);
        return this;
    }

    /**
     * An interface for instrumenters that you can attach to an environment. You
     * attach an instrumenter to an environment through the method
     * {@code registerInstrumenter(instrumenter)} of the environment. Once
     * registered, and if the debug-mode of the environment is turned on, every time
     * an agent invoke some method of the environment, it will cause the environment
     * to notify the instrumenter by invoking the instrumenter's {@code invoke()}
     * method.
     * 
     * <p>
     * This root class has no behavior. You will have to write your own subcclass to
     * have an instrumenter that actually does something.
     */
    public static interface EnvironmentInstrumenter {

        /**
         * Will be invoked by the Environment when its sendCommand() and refresh()
         * methods are invoked.
         */
        public void update(Environment env);

        /**
         * Will be invoked by the Environment when its reset() is invoked. The reset()
         * of this instrumenter should set this instrumenter state back to its initial
         * state.
         */
        public void reset();
    }

}
