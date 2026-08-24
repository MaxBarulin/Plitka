package com.example.ui.cad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Разбор числа с поддержкой запятой как разделителя. */
fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

fun fmtNum(v: Double, decimals: Int): String =
    if (decimals <= 0) "%.0f".format(v) else "%.${decimals}f".format(v).trimEnd('0').trimEnd('.', ',')

/**
 * Числовое поле с кнопками «−» и «+», шагающими на [step].
 * Значение можно как ввести руками, так и добрать стрелками.
 */
@Composable
fun NumberStepperField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    modifier: Modifier = Modifier,
    suffix: String = "",
    decimals: Int = 0,
    min: Double = -1_000_000.0,
    max: Double = 1_000_000.0,
    enabled: Boolean = true
) {
    var text by remember { mutableStateOf(fmtNum(value, decimals)) }
    var lastPushed by remember { mutableStateOf(value) }

    // Внешнее изменение значения (стрелки, решатель связей) — обновляем поле.
    if (value != lastPushed) {
        lastPushed = value
        text = fmtNum(value, decimals)
    }

    fun push(v: Double) {
        val c = v.coerceIn(min, max)
        lastPushed = c
        onValueChange(c)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { push(value - step); text = fmtNum((value - step).coerceIn(min, max), decimals) },
            enabled = enabled,
            modifier = Modifier.size(38.dp)
        ) { Icon(Icons.Default.Remove, contentDescription = "Минус $step", modifier = Modifier.size(18.dp)) }

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                parseNum(it)?.let { v -> push(v) }
            },
            label = { Text(label, fontSize = 11.sp) },
            suffix = if (suffix.isNotEmpty()) ({ Text(suffix, fontSize = 11.sp) }) else null,
            singleLine = true,
            enabled = enabled,
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        FilledTonalIconButton(
            onClick = { push(value + step); text = fmtNum((value + step).coerceIn(min, max), decimals) },
            enabled = enabled,
            modifier = Modifier.size(38.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Плюс $step", modifier = Modifier.size(18.dp)) }
    }
}

/** Выбор шага перемещения: пресеты + своё значение. */
@Composable
fun StepChooser(
    title: String,
    presets: List<Double>,
    step: Double,
    onStepChange: (Double) -> Unit,
    unit: String,
    decimals: Int = 0,
    modifier: Modifier = Modifier
) {
    var customText by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { p ->
                FilterChip(
                    selected = kotlin.math.abs(p - step) < 1e-9,
                    onClick = { onStepChange(p); customText = "" },
                    label = { Text("${fmtNum(p, decimals)} $unit", fontSize = 11.sp) }
                )
            }
            OutlinedTextField(
                value = customText,
                onValueChange = {
                    customText = it
                    parseNum(it)?.let { v -> if (v > 0) onStepChange(v) }
                },
                placeholder = { Text("свой", fontSize = 11.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(92.dp)
            )
        }
    }
}

/** Крестовина стрелок для сдвига точки с текущим шагом. */
@Composable
fun DPad(
    step: Double,
    unit: String,
    onMove: (dx: Double, dy: Double) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    enabled: Boolean = true
) {
    val btn = 42.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalIconButton(
            onClick = { onMove(0.0, -step) },
            enabled = enabled,
            modifier = Modifier.size(btn)
        ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Вверх") }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onMove(-step, 0.0) },
                enabled = enabled,
                modifier = Modifier.size(btn)
            ) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Влево") }

            Box(
                modifier = Modifier
                    .size(btn)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${fmtNum(step, decimals)}\n$unit",
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalIconButton(
                onClick = { onMove(step, 0.0) },
                enabled = enabled,
                modifier = Modifier.size(btn)
            ) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Вправо") }
        }

        FilledTonalIconButton(
            onClick = { onMove(0.0, step) },
            enabled = enabled,
            modifier = Modifier.size(btn)
        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз") }
    }
}

/** Компактная строка «подпись — значение». */
@Composable
fun InfoRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Небольшая иконка-кнопка для панелей инструментов. */
@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        colors = if (selected) IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) else IconButtonDefaults.filledTonalIconButtonColors()
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}
