#!/system/bin/sh
set -eu
SOURCE=${0%/*}
TARGET=/data/adb/modules/gappuccino_asi
[ "$(getprop ro.build.version.sdk)" = 35 ] || { echo 'This provider setup requires Android 15.'; exit 1; }
pm path com.google.android.as >/dev/null || exit 1
case "${1:-}" in
  enable)
    mkdir -p "$TARGET/system/product/overlay"
    cp "$SOURCE/module.prop" "$TARGET/module.prop"
    cp "$SOURCE/service.sh" "$TARGET/service.sh"
    cp "$SOURCE/GappuccinoProviders.apk" "$TARGET/system/product/overlay/GappuccinoProviders.apk"
    chmod 0755 "$TARGET" "$TARGET/service.sh"
    chmod 0644 "$TARGET/module.prop" "$TARGET/system/product/overlay/GappuccinoProviders.apk"
    # Retain the old module for rollback, but prevent competing provider overlays.
    [ ! -d /data/adb/modules/asi_smart_reply ] || touch /data/adb/modules/asi_smart_reply/disable
    rm -f "$TARGET/disable" "$TARGET/remove"
    cmd device_config put content_capture service_explicitly_enabled true
    cmd device_config put content_capture enable_contentcapture true
    settings --user 0 put secure content_capture_enabled 1
    # SYSTEM mode; inline presentation is negotiated with the keyboard separately.
    cmd device_config put autofill smart_suggestion_supported_modes 1
    # Preserve the narrowly targeted workaround for this ASI build's boot FGS.
    am compat disable --no-kill 296558535 com.google.android.as
    ;;
  disable)
    [ ! -d "$TARGET" ] || touch "$TARGET/disable"
    settings --user 0 put secure content_capture_enabled 0
    cmd device_config put content_capture service_explicitly_enabled false
    cmd device_config put content_capture enable_contentcapture false
    ;;
  *) exit 2 ;;
esac
echo 'Provider configuration saved. Reboot to apply provider defaults.'
