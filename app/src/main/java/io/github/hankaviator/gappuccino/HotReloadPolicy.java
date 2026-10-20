package io.github.hankaviator.gappuccino;

/** Only the generic filter has an audited retirement path. App-specific recipes restart. */
final class HotReloadPolicy {
    static boolean supports(String pkg, String process) {
        return pkg != null && pkg.equals(process)
                && TweakCatalog.PROFILE_PACKAGES.contains(pkg) && TweakCatalog.find(pkg) == null;
    }
    private HotReloadPolicy() {}
}
