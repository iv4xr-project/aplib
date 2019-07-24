package nl.uu.cs.aplib.Utils;

/**
 * A class to track elapsed computing time.
 */
public class Time {
	
	long lastsample ;
	
	public Time() { }
	
	/**
	 * Remember the time when this method is called. The time is sampled using System.currentTimeMillis,
	 * which is supposed to return time in ms since the UNIX epoch.
	 */
	public void sample() {
		lastsample = System.currentTimeMillis() ;
	}

	/**
	 * Return the time that has elapsed since the last call to sample(). Assuming time was tracked in
	 * ms, this will also return the elapsed time in ms.
	 */
	public long elapsedTimeSinceLastSample() {
		return System.currentTimeMillis() - lastsample ;
	}

}
