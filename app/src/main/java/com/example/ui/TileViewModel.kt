package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Calculation
import com.example.data.Order
import com.example.data.TileRepository
import com.example.ui.cad.CadEditorState
import com.example.ui.calc.CalculatorState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

class TileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TileRepository

    val allOrders: StateFlow<List<Order>>
    val allCalculations: StateFlow<List<Calculation>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TileRepository(database.orderDao(), database.calculationDao())
        
        allOrders = repository.allOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        allCalculations = repository.allCalculations.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Состояние экранов держим здесь: вкладки пересоздают композицию, и введённый
    // чертёж вместе с ценами иначе сбрасывался бы при каждом переключении.
    // Калькулятор читает cadState напрямую — данные плана попадают в смету сами.
    val cadState = CadEditorState()
    val calcState = CalculatorState()

    // --- Order Operations ---
    fun addOrder(
        title: String,
        clientName: String,
        clientPhone: String,
        dateMillis: Long,
        address: String,
        notes: String,
        totalCost: Double,
        status: String
    ) {
        viewModelScope.launch {
            repository.insertOrder(
                Order(
                    title = title,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    dateMillis = dateMillis,
                    address = address,
                    notes = notes,
                    totalCost = totalCost,
                    status = status
                )
            )
        }
    }

    fun updateOrder(order: Order) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            repository.deleteOrderById(orderId)
        }
    }

    // --- Calculation Operations ---
    fun saveCalculation(
        name: String,
        areaSqM: Double,
        tileWidthCm: Double,
        tileHeightCm: Double,
        groutWidthMm: Double,
        tilePricePerSqM: Double,
        glueConsKgPerSqM: Double,
        glueBagWeightKg: Double,
        gluePricePerBag: Double,
        groutPricePerKg: Double
    ) {
        viewModelScope.launch {
            val results = runCalculation(
                areaSqM = areaSqM,
                tileWidthCm = tileWidthCm,
                tileHeightCm = tileHeightCm,
                groutWidthMm = groutWidthMm,
                tilePricePerSqM = tilePricePerSqM,
                glueConsKgPerSqM = glueConsKgPerSqM,
                glueBagWeightKg = glueBagWeightKg,
                gluePricePerBag = gluePricePerBag,
                groutPricePerKg = groutPricePerKg
            )

            val calculation = Calculation(
                name = name,
                areaSqM = areaSqM,
                tileWidthCm = tileWidthCm,
                tileHeightCm = tileHeightCm,
                groutWidthMm = groutWidthMm,
                tilePricePerSqM = tilePricePerSqM,
                glueConsKgPerSqM = glueConsKgPerSqM,
                glueBagWeightKg = glueBagWeightKg,
                gluePricePerBag = gluePricePerBag,
                groutPricePerKg = groutPricePerKg,
                calculatedTileCount = results.tileCount,
                calculatedTileSqMNeededWithMargin = results.tileSqMNeededWithMargin,
                calculatedGlueBagsNeeded = results.glueBagsNeeded,
                calculatedGroutKgNeeded = results.groutKgNeeded,
                calculatedTotalMaterialCost = results.totalCost
            )
            repository.insertCalculation(calculation)
        }
    }

    fun deleteCalculation(calcId: Int) {
        viewModelScope.launch {
            repository.deleteCalculationById(calcId)
        }
    }

    // Calculation Result Holder
    data class CalcResults(
        val tileCount: Int,
        val tileSqMNeededWithMargin: Double,
        val glueBagsNeeded: Int,
        val groutKgNeeded: Double,
        val tileCost: Double,
        val glueCost: Double,
        val groutCost: Double,
        val totalCost: Double
    )

    fun runCalculation(
        areaSqM: Double,
        tileWidthCm: Double,
        tileHeightCm: Double,
        groutWidthMm: Double,
        tilePricePerSqM: Double,
        glueConsKgPerSqM: Double,
        glueBagWeightKg: Double,
        gluePricePerBag: Double,
        groutPricePerKg: Double
    ): CalcResults {
        if (areaSqM <= 0 || tileWidthCm <= 0 || tileHeightCm <= 0) {
            return CalcResults(0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        // Single tile area
        val tileWidthM = tileWidthCm / 100.0
        val tileHeightM = tileHeightCm / 100.0
        val tileAreaSqM = tileWidthM * tileHeightM

        // Effective tile size including grout
        val groutCm = groutWidthMm / 10.0
        val effectiveWidthM = (tileWidthCm + groutCm) / 100.0
        val effectiveHeightM = (tileHeightCm + groutCm) / 100.0
        val effectiveTileAreaSqM = effectiveWidthM * effectiveHeightM

        // Total area needed including 10% margin
        val tileSqMNeededWithMargin = areaSqM * 1.10

        // Tile count (based on effective area to account for grout gaps)
        val tileCount = ceil(tileSqMNeededWithMargin / effectiveTileAreaSqM).toInt()

        // Tile cost
        val tileCost = tileSqMNeededWithMargin * tilePricePerSqM

        // Glue bags calculation
        val totalGlueKg = areaSqM * glueConsKgPerSqM
        val glueBagsNeeded = ceil(totalGlueKg / glueBagWeightKg).toInt()
        val glueCost = glueBagsNeeded * gluePricePerBag

        // Grout kg calculation
        // Grout consumption rate: C = thickness(mm) * grout_width(mm) * (W_mm + H_mm) / (W_mm * H_mm) * 1.6
        val tileThicknessMm = 8.0 // assume standard 8mm tile thickness
        val tileWidthMm = tileWidthCm * 10.0
        val tileHeightMm = tileHeightCm * 10.0
        
        val groutConsRate = tileThicknessMm * groutWidthMm * ((tileWidthMm + tileHeightMm) / (tileWidthMm * tileHeightMm)) * 1.6
        val groutKgNeeded = Math.round((areaSqM * groutConsRate) * 10.0) / 10.0 // round to 1 decimal place
        val groutCost = groutKgNeeded * groutPricePerKg

        val totalCost = tileCost + glueCost + groutCost

        return CalcResults(
            tileCount = tileCount,
            tileSqMNeededWithMargin = tileSqMNeededWithMargin,
            glueBagsNeeded = glueBagsNeeded,
            groutKgNeeded = groutKgNeeded,
            tileCost = tileCost,
            glueCost = glueCost,
            groutCost = groutCost,
            totalCost = totalCost
        )
    }
}
