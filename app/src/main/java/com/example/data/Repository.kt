package com.example.data

import kotlinx.coroutines.flow.Flow

class TileRepository(
    private val orderDao: OrderDao,
    private val calculationDao: CalculationDao,
    private val workspaceDao: WorkspaceDao
) {
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allCalculations: Flow<List<Calculation>> = calculationDao.getAllCalculations()

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun deleteOrderById(id: Int) = orderDao.deleteOrderById(id)

    suspend fun insertCalculation(calculation: Calculation) = calculationDao.insertCalculation(calculation)
    suspend fun deleteCalculationById(id: Int) = calculationDao.deleteCalculationById(id)

    suspend fun loadWorkspace(): Workspace? = workspaceDao.get()
    suspend fun saveWorkspace(cadJson: String, calcJson: String) =
        workspaceDao.save(Workspace(cadJson = cadJson, calcJson = calcJson))
}
