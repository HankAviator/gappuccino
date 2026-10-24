package io.github.hankaviator.gappuccino;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Shared Google account-card identity, with exact-heading fallback for older menus. */
final class ProfilePromotionMatcher {
    static final int AI = 1, POINTS = 2, ACCOUNT = 3, AI_CANDIDATE = 4;
    private static final java.util.regex.Pattern WHITESPACE = java.util.regex.Pattern.compile("[\\s\\p{Z}]+");
    private static final java.util.regex.Pattern FORMAT = java.util.regex.Pattern.compile("\\p{Cf}");
    private static final Set<String> AI_STATUS_LABELS = Set.of("", "pro", "ultra", "plus", "starter");
    private static final java.util.regex.Pattern AI_BRAND = java.util.regex.Pattern.compile("google\\s*ai(?=$|[^a-z])");
    private static final Set<String> AI_LABELS = Set.of("get a google ai plan", "获取 google ai 方案", "获取 google ai 计划", "取得 google ai 方案", "訂閱 google ai 方案");
    private static final Set<String> POINTS_LABELS = Set.of("want to join play points", "want to join the play points program", "want to join play points plan", "想加入 play points 计划", "想加入 play points 計劃", "想加入 play points 方案");
    private static final Set<String> ACCOUNT_LABELS = Set.of("manage your google account", "管理您的 google 账号", "管理您的 google 帳戶", "管理你的 google 帳戶");
    private static final Set<String> COMPACT_ACCOUNT_LABELS = Set.of("manageyourgoogleaccount", "管理您的google账号", "管理您的google帳戶", "管理你的google帳戶");
    static boolean ai(CharSequence value) { return kind(value) == AI; }
    static boolean points(CharSequence value) { return kind(value) == POINTS; }
    static boolean account(CharSequence value) { return kind(value) == ACCOUNT; }
    static int kind(CharSequence value, String resourceName) {
        int exact = kind(value);
        if (exact != 0) return exact;
        if (!aiOfferHeading(value)) return 0;
        if ("og_bento_card_title".equals(resourceName)) return AI;
        // Play Store strips resource names. This marker is not eligible for
        // removal until the hook proves it shares the account-management stack.
        return "0_resource_name_obfuscated".equals(resourceName) ? AI_CANDIDATE : 0;
    }
    static boolean aiOfferHeading(CharSequence value) {
        if (value == null || value.length() > 160) return false;
        // Cheap prefilter before Unicode normalization for unrelated UI text.
        boolean googleInitial = false;
        for (int i = 0; i < value.length(); i++) {
            if ("gGＧｇ".indexOf(value.charAt(i)) >= 0) { googleInitial = true; break; }
        }
        if (!googleInitial) return false;
        String label = normalize(value);
        // Keep word boundaries before stripping spaces: offer wording after the
        // product name may be in any language, not just English "plan".
        if (!AI_BRAND.matcher(label).find()) return false;
        String compact = label.replace(" ", "");
        int brand = compact.indexOf("googleai");
        if (brand < 0) return false;
        int end = brand + "googleai".length();
        // A bare product/tier label is account status, not an invitation.
        if (brand == 0 && AI_STATUS_LABELS.contains(compact.substring(end))) return false;
        return true;
    }
    static int kind(CharSequence value) {
        if (value == null || value.length() > 160 || !candidateInitial(value)) return 0;
        String label = normalize(value);
        if (AI_LABELS.contains(label)) return AI;
        if (POINTS_LABELS.contains(label)) return POINTS;
        if (ACCOUNT_LABELS.contains(label) || COMPACT_ACCOUNT_LABELS.contains(label.replace(" ", ""))) return ACCOUNT;
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
        String label = FORMAT.matcher(Normalizer.normalize(value, Normalizer.Form.NFKC)).replaceAll("").toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ').trim();
        label = WHITESPACE.matcher(label).replaceAll(" ");
        return label.endsWith("?") ? label.substring(0, label.length() - 1) : label;
    }
    private ProfilePromotionMatcher() {}
}
