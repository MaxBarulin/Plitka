package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Calculation
import com.example.data.Order
import com.example.data.Snapshots
import com.example.data.TileRepository
import com.example.ui.cad.CadEditorState
import com.example.ui.calc.CalculatorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TileRepository

    val allOrders: StateFlow<List<Order>>
    val allCalculations: StateFlow<List<Calculation>>

    // Состояние экранов держим здесь: вкладки пересоздают композицию, и введённый
    // чертёж вместе с ценами иначе сбрасывался бы при каждом переключении.
    // Калькулятор читает cadState напрямую — данные плана попадают в смету сами.
    val cadState = CadEditorState()
    val calcState = CalculatorState()

    /** true, когда прошлое состояние уже прочитано с диска (или выяснилось, что его нет). */
    private val _workspaceLoaded = MutableStateFlow(false)
    val workspaceLoaded: StateFlow<Boolean> = _workspaceLoaded.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TileRepository(database.orderDao(), database.calculationDao(), database.workspaceDao())
        
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

        // Восстанавливаем прошлый сеанс до того, как человек начнёт что-то вводить.
        viewModelScope.launch {
            val saved = runCatching { repository.loadWorkspace() }.getOrNull()
            if (saved != null) {
                Snapshots.applyCadJson(cadState, saved.cadJson)
                Snapshots.applyCalcJson(calcState, saved.calcJson)
            }
            _workspaceLoaded.value = true
        }
    }

    /**
     * Сохраняет рабочее состояние на диск. Вызывается при сворачивании приложения:
     * человек мог просто убрать телефон, и введённое должно дождаться его возвращения.
     */
    fun persistWorkspace() {
        // Снимок делаем синхронно, в главном потоке: состояние Compose нельзя читать
        // из фонового потока, пока пользователь его правит.
        val cadJson = Snapshots.cadToJson(cadState)
        val calcJson = Snapshots.calcToJson(calcState)
        viewModelScope.launch {
            runCatching { repository.saveWorkspace(cadJson, calcJson) }
        }
    }

    /** Открыть сохранённый расчёт: восстанавливает и план, и все поля калькулятора. */
    fun loadCalculation(calculation: Calculation): Boolean {
        val cadOk = Snapshots.applyCadJson(cadState, calculation.cadJson.ifBlank { null })
        val calcOk = Snapshots.applyCalcJson(calcState, calculation.calcJson.ifBlank { null })
        return cadOk || calcOk
    }

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
    /**
     * Сохраняет смету как есть. Никакого повторного расчёта: в записи оказываются
     * ровно те цифры, которые калькулятор показал на экране.
     */
    fun saveCalculation(calculation: Calculation) {
        viewModelScope.launch {
            repository.insertCalculation(calculation)
        }
    }

    fun deleteCalculation(calcId: Int) {
        viewModelScope.launch {
            repository.deleteCalculationById(calcId)
        }
    }
}
