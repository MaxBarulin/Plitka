package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val clientName: String,
    val clientPhone: String,
    val dateMillis: Long,
    val address: String,
    val notes: String,
    val totalCost: Double,
    val status: String // PLANNED, IN_PROGRESS, DONE
)

/**
 * Снимок сметы на момент сохранения.
 *
 * Здесь лежат ровно те числа, которые человек видел на экране калькулятора.
 * Раньше запись пересчитывалась заново по упрощённой формуле, и сохранённое
 * не совпадало с показанным — теперь ничего не пересчитывается.
 */
@Entity(tableName = "calculations")
data class Calculation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),

    // --- исходные данные объекта
    val areaM2: Double,
    val perimeterM: Double,
    val tileWidthMm: Double,
    val tileHeightMm: Double,
    val tileThicknessMm: Double,
    val groutMm: Double,
    val wastePercent: Double,

    // --- плитка
    val tilePieces: Int,
    val tilePacks: Int,
    val tileAreaM2: Double,
    val tilePricePerM2: Double,
    val tileCost: Double,

    // --- клей
    val glueKgPerM2: Double,
    val glueKg: Double,
    val glueBags: Int,
    val glueBagKg: Double,
    val gluePricePerBag: Double,
    val glueCost: Double,

    // --- затирка
    val groutKgPerM2: Double,
    val groutKg: Double,
    val groutPacks: Int,
    val groutPricePerKg: Double,
    val groutCost: Double,

    // --- прочее: грунт, гидроизоляция, стяжка, крестики/СВП, погонаж
    val extrasCost: Double,

    // --- итоги
    val materialsCost: Double,
    val laborPricePerM2: Double,
    val laborCost: Double,
    val totalCost: Double,
    val totalWeightKg: Double,
    val workDays: Double,

    // --- полный снимок введённого, чтобы расчёт можно было открыть и продолжить
    val cadJson: String = "",
    val calcJson: String = ""
)

/**
 * Рабочий стол: последнее состояние экранов. Одна строка на приложение.
 * Пишется при сворачивании приложения, читается при запуске — введённое не теряется,
 * даже если систем убьёт процесс.
 */
@Entity(tableName = "workspace")
data class Workspace(
    @PrimaryKey val id: Int = 1,
    val cadJson: String,
    val calcJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
