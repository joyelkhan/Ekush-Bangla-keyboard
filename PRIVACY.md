# Privacy Policy — Ekush (একুশ)

**Developer:** Abu Naser Khan · NaserX Labs  
**Application ID:** `com.ekush.keyboard`

Ekush is engineered from the ground up with an absolute offline-first privacy guarantee:

1. **Zero Network Permissions:** `AndroidManifest.xml` declares zero `INTERNET` or `ACCESS_NETWORK_STATE` permissions. The app cannot initiate outbound network connections.
2. **On-Device Processing Only:** Phonetic transliteration, dictionary lookups, Bangla inflection resolution, and conservative autocorrect all execute locally on your device.
3. **Local Personal Lexicon & Mode Memory:** Words you type and per-app typing mode preferences are stored exclusively in an on-device Room database (`ekush_local.db`) and DataStore Preferences (`ekush_settings`), and may be cleared anytime in the Lexicon screen.
4. **No Telemetry or Ads:** Ekush contains zero analytics SDKs, crash reporters, ad networks, or trackers.
5. **User-Controlled Exports:** Settings/lexicon backups and diagnostic summaries are exported only when you explicitly tap Export or Prepare Diagnostic Report via Android's Storage Access Framework or Clipboard.
6. **Android `allowBackup` Disabled:** Manifest backup attributes prevent automatic unencrypted backup surprises across third-party backup agents.
