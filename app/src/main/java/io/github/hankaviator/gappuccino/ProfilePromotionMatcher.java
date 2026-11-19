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
        int classification = classify(value);
        if (classification != AI_CANDIDATE) return classification;
        return kindForResource(resourceName);
    }

    static int kindForResource(String resourceName) {
        if ("og_bento_card_title".equals(resourceName)) return AI;
        // Obfuscated Play Store markers still require account-stack validation.
        return "0_resource_name_obfuscated".equals(resourceName) ? AI_CANDIDATE : 0;
    }

    static int kind(CharSequence value) {
        if (value == null || value.length() > 160 || !candidateInitial(value)) return 0;
        return exactKind(normalize(value));
    }

    static int classify(CharSequence value) {
        if (value == null || value.length() > 160) return 0;
        boolean exactCandidate = candidateInitial(value);
        if (!exactCandidate && !hasGoogleInitial(value)) return 0;
        String label = normalize(value);
        int exact = exactCandidate ? exactKind(label) : 0;
        if (exact != 0) return exact;
        return (!exactCandidate || hasGoogleInitial(value)) && isAiOffer(label) ? AI_CANDIDATE : 0;
    }

    static boolean aiOfferHeading(CharSequence value) {
        return value != null && value.length() <= 160 && hasGoogleInitial(value)
                && isAiOffer(normalize(value));
    }

    private static boolean hasGoogleInitial(CharSequence value) {
        for (int i = 0; i < value.length(); i++) {
            if ("gGＧｇ".indexOf(value.charAt(i)) >= 0) return true;
        }
        return false;
    }

    private static boolean isAiOffer(String label) {
        if (!AI_BRAND.matcher(label).find()) return false;
        String compact = label.replace(" ", "");
        int brand = compact.indexOf("googleai");
        if (brand < 0) return false;
        int end = brand + "googleai".length();
        return brand != 0 || !AI_STATUS_LABELS.contains(compact.substring(end));
    }

    private static int exactKind(String label) {
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
