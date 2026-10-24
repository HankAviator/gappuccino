package io.github.hankaviator.gappuccino;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProfilePromotionMatcherTest {
    @Test public void sharedAccountCardMatchesLocalizedOffersWithoutTranslationList() {
        for (String heading : new String[]{"订阅 Google AI 方案", "订阅Google AI方案", "取得Google AI方案",
                "Obtenir un forfait Google AI", "Suscríbete a Google AI", "Google AI プランに登録",
                "Google AI 요금제 구독", "Получить подписку Google AI", "اشترك في Google AI", "Google AI abonnieren"})
            assertEquals(heading, ProfilePromotionMatcher.AI,
                    ProfilePromotionMatcher.kind(heading, "og_bento_card_title"));
    }
    @Test public void brandFormattingDoesNotDependOnAsciiSpacingOrDirection() {
        for (String heading : new String[]{"订阅\u200fGoogle\u00a0AI\u200e方案", "订阅Ｇｏｏｇｌｅ　ＡＩ方案", "订阅 Google\u202fAI 方案"})
            assertEquals(ProfilePromotionMatcher.AI, ProfilePromotionMatcher.kind(heading, "og_bento_card_title"));
    }
    @Test public void structuralMatchingDoesNotHideOrdinaryTextOrMembership() {
        assertEquals(0, ProfilePromotionMatcher.kind("订阅 Google AI 方案", "document_title"));
        assertEquals(0, ProfilePromotionMatcher.kind("订阅 Google AI 方案", null));
        for (String value : new String[]{"Google AI", "Google AI Pro", "Google AI Ultra", "Google Air", "Google Airlines", "管理您的 Google 账号", "付款和订阅"})
            assertNotEquals(ProfilePromotionMatcher.AI, ProfilePromotionMatcher.kind(value, "og_bento_card_title"));
    }
    @Test public void obfuscatedResourceRequiresAccountStructureValidation() {
        assertEquals(ProfilePromotionMatcher.AI_CANDIDATE,
                ProfilePromotionMatcher.kind("订阅 Google AI 方案", "0_resource_name_obfuscated"));
        assertEquals(0, ProfilePromotionMatcher.kind("Google AI Pro", "0_resource_name_obfuscated"));
        assertEquals(0, ProfilePromotionMatcher.kind("付款和订阅", "0_resource_name_obfuscated"));
    }
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
        assertTrue(ProfilePromotionMatcher.account("管理您的Google账号"));
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
