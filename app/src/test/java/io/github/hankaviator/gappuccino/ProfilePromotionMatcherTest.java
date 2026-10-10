package io.github.hankaviator.gappuccino;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProfilePromotionMatcherTest {
    @Test public void observedEnglishAiOffer() { assertTrue(ProfilePromotionMatcher.ai("Get a Google AI plan")); }
    @Test public void observedChinesePointsOffer() { assertTrue(ProfilePromotionMatcher.points("想加入 Play Points 计划？")); }
    @Test public void englishPointsPunctuationAndWhitespace() { assertTrue(ProfilePromotionMatcher.points(" Want to join\u00a0Play Points? ")); }
    @Test public void preserveMembershipAndSubscriptions() {
        for (String value : new String[]{"Play Points", "Google AI Pro", "Manage subscriptions", "Your Play Points balance", "Join Google One", "Manage your Google Account"}) {
            assertFalse(ProfilePromotionMatcher.ai(value)); assertFalse(ProfilePromotionMatcher.points(value));
        }
    }
    @Test public void preserveMessageOrDocumentWithOfferMention() {
        assertFalse(ProfilePromotionMatcher.ai("Yesterday I saw Get a Google AI plan"));
        assertFalse(ProfilePromotionMatcher.points("Want to join Play Points? Here is my review."));
    }
    @Test public void accountAnchorsAndNulls() {
        assertTrue(ProfilePromotionMatcher.account("管理您的 Google 账号"));
        assertTrue(ProfilePromotionMatcher.account("Manage your Google Account"));
        assertFalse(ProfilePromotionMatcher.ai(null)); assertFalse(ProfilePromotionMatcher.points(null));
    }
}
