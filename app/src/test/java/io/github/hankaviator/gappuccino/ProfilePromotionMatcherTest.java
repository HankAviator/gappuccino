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
    @Test public void fastPrefilterPreservesUnicodeHeadings() {
        assertTrue(ProfilePromotionMatcher.ai("\u00a0Ｇｅｔ ａ Ｇｏｏｇｌｅ ＡＩ ｐｌａｎ"));
        assertTrue(ProfilePromotionMatcher.ai("取得 Google AI 方案"));
        assertTrue(ProfilePromotionMatcher.points("想加入 Play Points 计划？"));
    }
    @Test public void emptyAndLongUiTextHaveNoMarker() {
        assertEquals(0, ProfilePromotionMatcher.kind(""));
        assertEquals(0, ProfilePromotionMatcher.kind(" \u00a0"));
        assertEquals(0, ProfilePromotionMatcher.kind("Get a Google AI plan".repeat(20)));
    }
}
