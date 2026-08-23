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

@Entity(tableName = "calculations")
data class Calculation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val areaSqM: Double,
    val tileWidthCm: Double,
    val tileHeightCm: Double,
    val groutWidthMm: Double,
    val tilePricePerSqM: Double,
    val glueConsKgPerSqM: Double,
    val glueBagWeightKg: Double,
    val gluePricePerBag: Double,
    val groutPricePerKg: Double,
    
    // Calculated Outputs
    val calculatedTileCount: Int,
    val calculatedTileSqMNeededWithMargin: Double,
    val calculatedGlueBagsNeeded: Int,
    val calculatedGroutKgNeeded: Double,
    val calculatedTotalMaterialCost: Double
)
