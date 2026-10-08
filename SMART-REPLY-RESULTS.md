# ASI Smart Reply results

October 8, 2026. Installed Gappuccino **0.1.5 / version code 6** on Xiaomi 14,
Android 15. ASI **V.41.playstore.oemfull.843410720**, Gboard
**18.4.1.985164140-release-arm64-v8a**.

## Outcome

WhatsApp Smart Reply generation was observed through ASI and a reply chip was
visible in Gboard. The user subsequently confirmed WhatsApp works. **Official
Telegram does not work in the tested configuration.** An allowlist alone did
not produce a parsed conversation or reply request. No messages were sent, no
reply suggestions were tapped, and no conversation text was entered by the agent.
Only Telegram's chat-search field was used to reopen the user-selected thread.

This is not validation of every supported app or every language. The visible
WhatsApp candidate in the inspected conversation was an emoji; text replies to
a fresh incoming English question were not separately verified.

## Implementation

Gappuccino previously contained only the ASI multilingual classify/select-text
fallback. It now also ships and manages a resource-only Magisk companion under
`/data/adb/modules/gappuccino_asi`, installed from its own ASI page. There is no
second Xposed APK, and its seven Google-app scopes are unchanged.

The companion supplies these framework resource defaults:

| Resource | Value |
|---|---|
| config_defaultContentCaptureService | com.google.android.as/com.google.android.apps.miphone.aiai.app.AiAiContentCaptureService |
| config_defaultAugmentedAutofillService | com.google.android.as/com.google.android.apps.miphone.aiai.app.AiAiAugmentedAutofillService |

Its boot service restores `content_capture/service_explicitly_enabled=true`,
`content_capture/enable_contentcapture=true`, and user 0's secure
`content_capture_enabled=1`. These flags had reverted in the live initial state.
The service checks once a minute and repairs only those values; it does not
disable global DeviceConfig synchronization.

The old `/data/adb/modules/asi_smart_reply` is disabled and retained. The existing
text-classifier resource module is preserved. Original Google Xposed modules
remain disabled, and Gappuccino's enabled state and seven scopes were checked
again in a final LSPosed database snapshot.

The new V41 locale hook repairs ASI's unsupported display-language eligibility
check using its existing English eligibility path. It retains the autofill
preference and supported-model checks, and leaves conversation-language checks
intact. It does not install Chinese reply models. Unknown ASI version names skip
these obfuscated mappings.

Optional tracing records capture/request counts, parsed-message counts, snapshot
state and response presence. It does not log message text or proposed replies.
The experimental allowlist switch applies only to `org.telegram.messenger`;
Telegram Monet is excluded at the user's request.

## Live-state verification

Before setup, both provider resource defaults resolved to empty, despite the
older provider module being present. The older overlay had not taken effect.
After installing Gappuccino's setup and rebooting:

- Both resource defaults resolved to the intended ASI components without
  temporary service overrides (`numberTemps=0`).
- Content Capture and augmented autofill both connected to ASI.
- The crash buffer was empty; compatibility change **296558535** remained
  disabled only for `com.google.android.as`.
- Regular password autofill remained
  `com.x8bit.bitwarden/com.x8bit.bitwarden.Autofill.AutofillService`.
- Smart suggestion mode remained **1 / SYSTEM**.
- The required Content Capture flags were still enabled at the final check,
  more than two hours after setup.

ASI initially reported its augmented handler disabled for `zh-cn` with models
for `[en,fr,de,es,ja]`. With the repair installed, its conversation pipeline
included the normal supported packages. During English Gboard testing it reported
the handler enabled for `en-gb`, model version **20000**.

## Pipeline observations

| Test | Capture / parsing | Generation / rendering |
|---|---|---|
| Instagram, user-selected thread | Capture events reached ASI; two messages parsed into a READY snapshot. | No augmented fill request. Its tested composer had no active autofill service client. No reply chip verified. |
| WhatsApp, user-selected thread | Capture and conversation processing occurred. | ASI received fill requests for `com.whatsapp/id:entry`. Its TC candidate provider returned `1/1`; response codes included `RESPOND_OK`. Non-null FillResponse callbacks and a visible Gboard chip were observed. User confirmed operation. |
| Official Telegram, normal setup | Neither applicable ASI capture nor reply allowlist included Telegram. | No reply request or suggestion. |
| Official Telegram, experimental allowlists | Verified Telegram added alongside existing packages to both allowlists. Capture session and events reached ASI. No parsed conversation snapshot was observed. | Handler request history remained empty while the user-selected empty composer was focused. No reply chip appeared. Experiment restored off. |

The first provider-disable comparison retained a chip, so it was affected by
caching. A later fresh-keyboard comparison failed to keep a usable keyboard in
both conditions. Those comparisons are **inconclusive** and are not counted as
proof of rendering attribution. The positive evidence is ASI's actual candidate
and response path, the observed chip, and the user's confirmation.

## Official Telegram compatibility

The installed Telegram APK's `EditTextBoldCursor.getAutofillType()` bytecode was
checked: `12000f00` (`const/4 v0, 0; return v0`), so it returns
**AUTOFILL_TYPE_NONE**. Its upstream source also marks the view unimportant for
autofill and returns that type. See [Telegram's EditTextBoldCursor source](https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java).

ASI additionally checks app/field target slots and expects a parsed conversation
snapshot before its TC reply provider can run. Its successful WhatsApp path does
not imply support for another app's custom view hierarchy. This experiment did
not supply Telegram parsing rules or target-slot mappings.

Supporting Telegram would require an app-specific adapter to expose the correct
composer and incoming/outgoing conversation semantics, plus matching ASI reply
targets. That adapter has **not** been implemented. No unverified Telegram support
is presented as working, and no hooks were installed directly in Telegram.

## Final configuration and artifacts

Smart Reply providers and display-language fallback are on. The Telegram capture
experiment is off. Metadata diagnostics remain on for follow-up investigation;
they can be switched off on the ASI page, followed by restarting ASI. Autofill
framework debug logging was restored off. Bitwarden, SYSTEM suggestion mode, and
the working ASI provider defaults are preserved.

Final assemble, lint and all 11 existing parser tests pass. Those tests cover the
previous Photos parsers, not Smart Reply. Device checks above provide the ASI
evidence. Detailed dumps and private screenshots stay in the ignored
`app/build/smart-reply/` directory; no conversation screenshots are checked in.
The installable APK is `app/build/outputs/apk/debug/app-debug.apk`.
