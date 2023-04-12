package eu.iv4xr.framework.mainConcepts;

/**
 * An interface for generically represent emotion and its intensity.
 * 
 * @author Wish
 */
public interface IEmotion {
	
	/**
	 * Return the type of the emotion. E.g. "fear".
	 */
	public String getEmotionType() ;
	
	/**
	 * The id of the person who has the emotion.
	 */
	public String getAgentId() ;
	
	/**
	 * The id of the target of the emotion, which can be a person (if someone is
	 * angry towards another person), or some goal (e.g. "to win a game", and the
	 * emotion could be "joy" towards this goal e.g. when its prospect because quite
	 * certain)
	 */
	public String getTargetId() ;
	
	/**
	 * Return the intensity of this emotion.
	 */
	public float getIntensity() ;
	
	/**
	 * Return the time when this emotion is sampled; if this information is available.
	 * The value of {@link #getIntensity()} is the intensity sampled at that time.
	 * If no timing information is available, the methos returns null.
	 */
	public Long getTime() ;

	/**
	 * Return the (last) time when this emotion was activated/triggered. After the activation,
	 * the emotion might decay, until it is reactivated again. The method {@link #getTime}
	 * returns the time when this emotion is being sampled, which would be at the time
	 * it was activated, or later.
	 * If no timing information is available, the methos returns null.
	 */
	public Long getActivationTime() ;

}
