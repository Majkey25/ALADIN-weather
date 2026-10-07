# Appearance and confidence verification, 0.5.0

Version code 37. Checked on Huawei YAL-L21, Android 10, on 7 October 2026. Only the separate debug application and isolated test preferences were used. Production app data, device theme, launch restrictions and permissions were unchanged.

## Appearance

The forecast uses soft weather gradients, outlined icons and a thinner hourly chart. System, Light and Dark modes keep independent theme selections. The Appearance menu includes twelve presets, custom opaque HEX colours, font styles, text size and corner radius. App-style widgets resolve matching text colours; manual widget overrides remain intact. Radar colours change without reloading its frames or layers.

Custom palettes require readable primary and secondary text across the gradient and surface. Validation checks 33 sRGB samples with a contrast margin, not just endpoints. Invalid settings fall back to a readable palette. Manual white text on a white background cannot be applied.

## Confidence

Hourly comparison metadata describes the complete live model cohort that contributes to the displayed temperature, wind, cloud, condition and precipitation fields. Partial cohorts and static calibration run mismatches remain unavailable. Wind disagreement includes direction. Rain comparison uses the same interval-ending source record as the displayed starting-hour totals.

High, Medium, Low and Limited are descriptive estimates. Disagreement, longer lead times and older downloads reduce the rating. Differing rain amounts and displayed values outside the compared range cannot inherit high confidence. Daily ratings require coverage of at least three quarters of the remaining hours and use the least certain compared hour. Partial coverage cannot become High.

This does not change forecast values or activate learned weights. Download age is not provider run age. Model agreement is not a validated probability of accuracy, and the explanatory dialog states that limitation.

## Checks

- 432 unit checks passed, with no failures or errors.
- 42 radar and location-picker JavaScript checks passed.
- 22 final Huawei checks passed in 24.4 seconds: appearance persistence and separate variants, blocked unreadable custom colours, confidence explanation, compact and enlarged forecast layouts, navigation, native widget resizing and real WebView observed/future frames.
- Live Prague refresh showed nine complete model inputs. Tapping Now opened its expanded hour, and the confidence dialog displayed real temperature, wind, cloud and rainfall ranges. Light and dark screens were inspected; the debug appearance mode was restored to System before releasing the phone.
- Debug and release lint and APK/AAB builds passed. Release signing uses the existing upload certificate; final artifacts require the committed-tree build.
- Earlier native failures were outdated test assumptions: the background-consent fixture was absent, lazy rows were not composed, and coordinate taps missed a scrolled category. The test now grants consent only in its isolated fixture and uses native semantic clicks. Assertions were retained; production consent behavior was not changed.
- Claude Code `claude-opus-5-5` with high effort was unavailable because of its weekly limit. A fresh read-only Codex review found three issues: incomplete model cohorts, rain-amount disagreement and interior gradient contrast. Regression checks cover the fixes. The focused recheck found no actionable issue.

## Limits

One Android 10 device does not validate every launcher or device. Android 12+ Material You uses the platform colour schemes but was not physically checked on this phone. These tests do not establish improved meteorological accuracy or calibrate confidence against observations.

References: [T3code Appearance](https://github.com/pingdotgg/t3code/blob/main/docs/user/appearance.md), [Gradient Weather](https://play.google.com/store/apps/details?id=com.subtlesignals.gradientweather), [Material 3 dynamic colours](https://developer.android.com/develop/ui/compose/designsystems/material3), [ECMWF forecast uncertainty](https://www.ecmwf.int/en/research/modelling-and-prediction/quantifying-forecast-uncertainty).
