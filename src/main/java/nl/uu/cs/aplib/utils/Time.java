package nl.uu.cs.aplib.utils;

/**
 * A class to track elapsed computing time.
 */
public class Time {

    protected long lastsample;

    public Time() {
    }

    /**
     * Return the current time. This is obtained using System.currentTimeMillis,
     * which is supposed to return time in ms since the UNIX epoch.
     */
    public long currentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Remember the time when this method is called. The time is sampled using
     * System.currentTimeMillis, which is supposed to return time in ms since the
     * UNIX epoch.
     */
    public void sample() {
        lastsample = currentTime();
    }

    /**
     * Return the time that has elapsed since the last call to sample(). Assuming
     * time was tracked in ms, this will also return the elapsed time in ms.
     */
    public long elapsedTimeSinceLastSample() {
        return currentTime() - lastsample;
    }

}
