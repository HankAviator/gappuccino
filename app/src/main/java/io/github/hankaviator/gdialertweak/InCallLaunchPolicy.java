package io.github.hankaviator.gdialertweak;

/** Only the short automatic launch transition after a notification answer is suppressed. */
final class InCallLaunchPolicy {
    private long until;
    synchronized void answered(long now) { until = now + 8_000L; }
    synchronized void explicitOpen() { until = 0; }
    synchronized boolean suppress(long now) {
        if (until == 0 || now >= until) { until = 0; return false; }
        return true;
    }
}
