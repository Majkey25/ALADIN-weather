# Background notification investigation

Selia Weather 0.4.1, version code 33. Checked on the shared Huawei Android 10 test phone on 1 October 2026. Production data and settings were not changed.

## Causes and fixes

1. The reboot/update receiver returned before restoring rain and warning checks whenever the separate morning briefing was off. A native regression test failed against 0.4.0. Moving the existing alert scheduler before that guard fixed the test. Disabled alert categories and notification permissions remain respected.
2. A real background refresh failed when a model request timed out and the fallback forecast contained a null `daily.precipitation_probability_max[20]`. The parser rejected the entire response. Daily rain probability is now nullable, preserving the rest of the forecast. The UI shows unavailable, not zero, and the briefing omits unsupported no-umbrella advice. The parser regression failed before this fix and passes afterward.
3. Huawei automatic launch management prevented the cold service from starting. The system logged `Service starting has been prevented by iaware or trustsbase`, followed by `WeatherRefreshJob unavailable`. The app cannot override this policy. Notification settings now include Background delivery guidance and a link to this app's Android settings.

## Physical background check

Only the debug package was used. Huawei launch management was temporarily changed from automatic to manual, keeping its existing Auto-launch, Secondary launch and Run in background switches enabled. No device-wide battery policy was changed.

- The launcher was in front.
- `am kill` removed the debug process, PID 14832, at 11:17:12 CEST. This was not Force stop.
- A forced run of pending JobScheduler job 7003 started PID 14953 for `WeatherRefreshJob` at 11:17:25.
- The new process fetched actual weather data successfully and posted briefing notification 7001 at approximately 11:17:30, while the launcher remained in front.
- Probe preferences and Huawei automatic launch management were restored. Saved UI trees confirm all original launch switches match the restored values.

This proves cold-process execution, data loading and notification posting when background launch is allowed. It does not prove natural timing of periodic alert job 7004, execution during Doze, real reboot delivery, or delivery under the restored Huawei automatic policy. Instrumentation itself force-stops its target on exit; cold-process tests must clear that stopped state by opening the app normally before closing and killing its process.

## Verification

- 409 Android unit tests passed, zero failures/errors/skips.
- 17 focused Huawei checks passed, covering recovery, settings navigation, the background-help action, and forecast layouts.
- Debug lint and APK/instrumentation builds passed.
- A release build daemon disappeared during R8. Retrying with a single worker and a single-use daemon passed. Final signed release artifacts are checked separately during publication.
- Independent review found no blocking regression. OS background restrictions remain an explicit delivery limit.

References: [Android background limits](https://developer.android.com/topic/performance/background-optimization), [Huawei background protection](https://consumer.huawei.com/en/support/content/en-us15850065/).
