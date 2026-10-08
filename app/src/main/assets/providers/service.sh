#!/system/bin/sh
MODDIR=${0%/*}
attempt=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$attempt" -lt 180 ]; do
  sleep 1
  attempt=$((attempt + 1))
done
# DeviceConfig can overwrite these values after boot. Reapply only the three
# required Content Capture settings, without globally disabling config sync.
while [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ]; do
  [ "$(cmd device_config get content_capture service_explicitly_enabled)" = true ] || cmd device_config put content_capture service_explicitly_enabled true
  [ "$(cmd device_config get content_capture enable_contentcapture)" = true ] || cmd device_config put content_capture enable_contentcapture true
  [ "$(settings --user 0 get secure content_capture_enabled)" = 1 ] || settings --user 0 put secure content_capture_enabled 1
  sleep 60
done
