package cz.majkey.pocasicesko.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.majkey.pocasicesko.R
import cz.majkey.pocasicesko.data.ConfidenceLevel
import cz.majkey.pocasicesko.data.ConfidenceReason
import cz.majkey.pocasicesko.data.ForecastConfidence
import cz.majkey.pocasicesko.units.WeatherUnitFormatter

@Composable
internal fun ForecastConfidenceBadge(confidence: ForecastConfidence, units: WeatherUnitFormatter) {
    var show by rememberSaveable(confidence.validTime) { mutableStateOf(false) }
    val level = stringResource(confidence.level.labelResource())
    val description = stringResource(R.string.confidence_label, level)
    AssistChip(onClick = { show = true }, label = { Text(description, fontSize = 12.sp) },
        modifier = Modifier.heightIn(min = 48.dp))
    if (show) AlertDialog(onDismissRequest = { show = false },
        title = { Text(description) },
        text = {
            Column(Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.65f)
                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(when (confidence.reason) {
                    ConfidenceReason.AGREEMENT -> R.string.confidence_agreement
                    ConfidenceReason.DISAGREEMENT -> R.string.confidence_disagreement
                    ConfidenceReason.LONG_RANGE -> R.string.confidence_long_range
                    ConfidenceReason.FETCH_AGE -> R.string.confidence_old_fetch
                    ConfidenceReason.MISSING_MODELS -> R.string.confidence_missing
                    ConfidenceReason.PARTIAL_DAY -> R.string.confidence_partial_day
                    ConfidenceReason.HISTORICAL -> R.string.confidence_historical
                }))
                confidence.expectedHours?.let { expected ->
                    Text(stringResource(R.string.confidence_day_coverage, confidence.coveredHours ?: 0, expected))
                }
                confidence.evidence?.let { evidence ->
                    if (confidence.expectedHours != null) Text(stringResource(R.string.confidence_day_method))
                    Text(stringResource(R.string.confidence_models, confidence.modelCount))
                    Text(stringResource(R.string.confidence_temperature_spread, units.temperatureDifference(evidence.temperatureRangeCelsius)))
                    Text(stringResource(R.string.confidence_wind_spread, units.windSpeed(evidence.windRangeKmh)))
                    Text(stringResource(R.string.confidence_cloud_spread, evidence.cloudRangePercent.toInt()))
                }
                confidence.precipitationEvidence?.let { rain ->
                    Text("${stringResource(R.string.precipitation)} · ${units.precipitation(rain.minimumPrecipitationMm)} – ${units.precipitation(rain.maximumPrecipitationMm)}")
                }
                Text(stringResource(R.string.confidence_method), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, confirmButton = { TextButton(onClick = { show = false }) { Text(stringResource(android.R.string.ok)) } })
}

internal fun ConfidenceLevel.labelResource(): Int = when (this) {
    ConfidenceLevel.HIGH -> R.string.confidence_high
    ConfidenceLevel.MODERATE -> R.string.confidence_moderate
    ConfidenceLevel.LOW -> R.string.confidence_low
    ConfidenceLevel.LIMITED -> R.string.confidence_limited
}
