package com.example.data

import kotlinx.coroutines.flow.Flow

class TileRepository(
    private val orderDao: OrderDao,
    private val calculationDao: CalculationDao
) {
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allCalculations: Flow<List<Calculation>> = calculationDao.getAllCalculations()

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun deleteOrderById(id: Int) = orderDao.deleteOrderById(id)

    suspend fun insertCalculation(calculation: Calculation) = calculationDao.insertCalculation(calculation)
    suspend fun deleteCalculationById(id: Int) = calculationDao.deleteCalculationById(id)
}
