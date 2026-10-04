# Notification timing, 0.4.2

Checked on Huawei YAL-L21, Android 10, on 4 October 2026. Only the debug package was installed or configured. The Play installation and its settings were unchanged.

## Findings

- The morning briefing was fixed at 07:00. Android 12+ used a window alarm without Alarms & reminders access, which could be deferred in sleep.
- A failed briefing refresh remained eligible all day. Opening the app could allow that old work to run and post morning advice hours late.
- The alarm could post cached advice and then post it again after the refresh. There was no persistent daily delivery marker.
- Rain used the same six-hour horizon as other advice and could trigger from one weak wet-model signal with no rain in the main forecast.

## Changes

Choose a daily time through the native time picker in Notifications → Morning briefing. A preparation alarm requests forecast refresh beforehand; the delivery alarm can post a valid cached forecast without waiting for a network job. A missed delivery expires after 30 minutes. A persistent daily marker prevents duplicate delivery.

Rain has its own lead time, default one hour, and probability threshold. Forecast rain, precipitation conditions, sufficient probability or model agreement can trigger advice. A single weak model signal alone does not trigger it. A cached forecast schedules a native rain check alongside the existing periodic refresh.

Android 12+ has a user-directed Alarms & reminders action. Without that access, alarms use the system's inexact idle-compatible path. Revoking access during scheduling falls back safely. User-disabled channels remain respected.

## Physical check

At approximately 10:12 CEST, the real app menu was used to change the briefing from 07:00 to 10:15. `dumpsys alarm` showed an RTC wakeup at 10:15 with a zero scheduling window. The launcher was shown, the app process was killed with `am kill`, and a subsequent `pidof` was empty. This was not Force stop.

By 10:15:45, a new process, PID 15932, had posted notification 7001. Its daily delivery marker was 2026-10-04, and the launcher remained in front. No app opening or forced job run was used to cause delivery. The menu time was restored to 07:00 afterward. Huawei launch policy was not changed during this test.

The old missed-briefing regression failed on the phone before implementation. Rain horizon and weak-signal regressions failed in unit tests before implementation. Nineteen focused native checks passed afterward, covering expiry, category controls, rain lead/probability changes, navigation and layouts. Unit tests cover chosen time, daylight saving, delivery boundaries and future rain scheduling.

## Limits

This confirms natural alarm delivery from a cold process on this Android 10 phone with a valid cached forecast. It does not validate overnight Doze, all manufacturer policies, Android 12+ special-access UI or weather prediction accuracy. Alarms cannot override Force stop; the app must be reopened afterward. A forecast must be available for the selected place.

Reference: [Android alarm scheduling](https://developer.android.com/develop/background-work/services/alarms/schedule).
