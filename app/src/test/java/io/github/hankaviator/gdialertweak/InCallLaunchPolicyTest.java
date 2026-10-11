package io.github.hankaviator.gdialertweak;

import org.junit.Test;
import static org.junit.Assert.*;

public class InCallLaunchPolicyTest {
    @Test public void unrelatedLaunchesRemainVisible() {
        assertFalse(new InCallLaunchPolicy().suppress(100));
    }
    @Test public void answerTransitionExpires() {
        InCallLaunchPolicy policy = new InCallLaunchPolicy();
        policy.answered(100);
        assertTrue(policy.suppress(101));
        assertFalse(policy.suppress(8100));
        assertFalse(policy.suppress(8200));
    }
    @Test public void notificationTapImmediatelyOverridesAnswer() {
        InCallLaunchPolicy policy = new InCallLaunchPolicy();
        policy.answered(100);
        policy.explicitOpen();
        assertFalse(policy.suppress(101));
        assertFalse(policy.suppress(200));
    }
    @Test public void nextAnswerStartsANewTransition() {
        InCallLaunchPolicy policy = new InCallLaunchPolicy();
        policy.answered(100);
        policy.explicitOpen();
        policy.answered(500);
        assertTrue(policy.suppress(501));
        assertFalse(policy.suppress(8500));
    }
}
