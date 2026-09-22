package com.fitbudget.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.fitbudget.app.util.DateTimeUtils
import com.fitbudget.app.util.Formatters
import java.time.DayOfWeek

/** Text field for numeric input with inline validation message. */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    decimal: Boolean = true,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                // Accept only digits, one separator and nothing else - no crash on paste.
                val filtered = raw.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }
                    .replace(',', '.')
                onValueChange(filtered)
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = errorMessage != null,
            suffix = suffix?.let { { Text(it) } },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = imeAction
            ),
            supportingText = {
                val message = errorMessage ?: supportingText
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (errorMessage != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun PlainTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        isError = errorMessage != null,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        supportingText = {
            val message = errorMessage ?: supportingText
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** +/- control for meal servings. */
@Composable
fun QuantityStepper(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    step: Double = 0.5,
    range: ClosedFloatingPointRange<Double> = 0.5..20.0
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(
            onClick = { onQuantityChange((quantity - step).coerceIn(range.start, range.endInclusive)) },
            enabled = quantity - step >= range.start,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
        }
        Text(
            Formatters.quantity(quantity),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        FilledIconButton(
            onClick = { onQuantityChange((quantity + step).coerceIn(range.start, range.endInclusive)) },
            enabled = quantity + step <= range.endInclusive,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}

/** Row that opens a Material 3 time picker. */
@Composable
fun TimeField(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }

    FitCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (enabled) ({ showPicker = true }) else null,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                DateTimeUtils.formatTime(hour, minute),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showPicker) {
        FitTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showPicker = false },
            onConfirm = { h, m ->
                showPicker = false
                onTimeSelected(h, m)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    title: String = "Pick a time"
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Weekday selector for reminder repeat rules. */
@Composable
fun DayOfWeekSelector(
    daysMask: Int,
    onMaskChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                val selected = DateTimeUtils.maskContains(daysMask, day)
                FilterChip(
                    selected = selected,
                    enabled = enabled,
                    onClick = {
                        val bit = 1 shl (day.value - 1)
                        val updated = if (selected) daysMask and bit.inv() else daysMask or bit
                        onMaskChange(updated and DateTimeUtils.ALL_DAYS_MASK)
                    },
                    label = {
                        Text(day.name.take(1), style = MaterialTheme.typography.labelMedium)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            DateTimeUtils.describeMask(daysMask),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Simple labelled checkbox row used by checklists and settings. */
@Composable
fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
