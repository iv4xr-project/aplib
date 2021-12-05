package eu.iv4xr.framework.mainConcepts;

import java.util.List;

/**
 * A generic interface for representing the emotional state of an agent. Since
 * this is only an interface, you need to provide a concrete implementation of
 * it. To use it, attach it to an emotional-test-agent using
 * {@link EmotiveTestAgent#attachEmptionState(IEmotionState)}. Then, whenever
 * the agent is updated (through a call to its {@link EmotiveTestAgent#update()}
 * method), it will also update the emotion-state attached to it by calling the
 * method {@link #updateEmotion(EmotiveTestAgent)}.
 * 
 * @author Wish
 */
public interface IEmotionState {

	/**
	 * This will return all the current/latest emotions stored in the emotion state.
hy	 */
	public List<IEmotion> getCurrentEmotion();

	/**
	 * This should implement how the emotion state is updated. The method is called
	 * by the agent whenever the method {@link EmotiveTestAgent#update()} is called,
	 * passing to it a reference to the agent to which this emotion state is attached to.
	 * This allows this method to inspect the agent state to calculate what emotion
	 * that would induce.
	 * 
	 * @param agent The {@link EmotiveTestAgent} to which this emotion-state is attached to.
	 */
	public void updateEmotion(EmotiveTestAgent agent);

}
