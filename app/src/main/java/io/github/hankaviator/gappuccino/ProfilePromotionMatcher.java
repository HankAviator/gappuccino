package io.github.hankaviator.gappuccino;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Exact offer headings: never match an arbitrary mention of AI or Play Points. */
final class ProfilePromotionMatcher {
    static final int AI = 1, POINTS = 2, ACCOUNT = 3;
    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("\\s+");
    private static final Set<String> AI_LABELS = Set.of("get a google ai plan", "获取 google ai 方案", "获取 google ai 计划", "取得 google ai 方案", "訂閱 google ai 方案");
    private static final Set<String> POINTS_LABELS = Set.of("want to join play points", "want to join the play points program", "want to join play points plan", "想加入 play points 计划", "想加入 play points 計劃", "想加入 play points 方案");
    private static final Set<String> ACCOUNT_LABELS = Set.of("manage your google account", "管理您的 google 账号", "管理您的 google 帳戶", "管理你的 google 帳戶");
    static boolean ai(CharSequence value) { return kind(value) == AI; }
    static boolean points(CharSequence value) { return kind(value) == POINTS; }
    static boolean account(CharSequence value) { return kind(value) == ACCOUNT; }
    static int kind(CharSequence value) {
        if (value == null || value.length() > 160 || !candidateInitial(value)) return 0;
        String label = normalize(value);
        if (AI_LABELS.contains(label)) return AI;
        if (POINTS_LABELS.contains(label)) return POINTS;
        if (ACCOUNT_LABELS.contains(label)) return ACCOUNT;
        return 0;
    }
    private static boolean candidateInitial(CharSequence value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || c == '\u00a0') continue;
            return "gGmMwWＧｇＭｍＷｗ获取訂想管".indexOf(c) >= 0;
        }
        return false;
    }
    private static String normalize(CharSequence value) {
        if (value == null || value.length() > 160) return "";
        String label = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ').trim();
        label = WHITESPACE.matcher(label).replaceAll(" ");
        return label.endsWith("?") ? label.substring(0, label.length() - 1) : label;
    }
    private ProfilePromotionMatcher() {}
}
