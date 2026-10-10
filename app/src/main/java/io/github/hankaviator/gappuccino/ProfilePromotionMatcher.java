package io.github.hankaviator.gappuccino;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Exact offer headings: never match an arbitrary mention of AI or Play Points. */
final class ProfilePromotionMatcher {
    private static final Set<String> AI = Set.of("get a google ai plan", "获取 google ai 方案", "获取 google ai 计划", "取得 google ai 方案", "訂閱 google ai 方案");
    private static final Set<String> POINTS = Set.of("want to join play points", "want to join the play points program", "want to join play points plan", "想加入 play points 计划", "想加入 play points 計劃", "想加入 play points 方案");
    private static final Set<String> ACCOUNT = Set.of("manage your google account", "管理您的 google 账号", "管理您的 google 帳戶", "管理你的 google 帳戶");
    static boolean ai(CharSequence value) { return AI.contains(normalize(value)); }
    static boolean points(CharSequence value) { return POINTS.contains(normalize(value)); }
    static boolean account(CharSequence value) { return ACCOUNT.contains(normalize(value)); }
    private static String normalize(CharSequence value) {
        if (value == null || value.length() > 160) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ').trim().replaceAll("\\s+", " ").replaceFirst("[?？]$", "");
    }
    private ProfilePromotionMatcher() {}
}
