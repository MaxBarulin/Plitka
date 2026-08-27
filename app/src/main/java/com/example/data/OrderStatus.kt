package com.example.data

/**
 * Статус заказа хранится в базе кодом, а показывается подписью.
 * Раньше экраны сравнивали код с подписью («Готово»), из-за чего завершённые
 * заказы не отсеивались на главной — держим единственный источник правды здесь.
 */
object OrderStatus {
    const val PLANNED = "PLANNED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val DONE = "DONE"

    val ALL = listOf(PLANNED, IN_PROGRESS, DONE)

    fun label(code: String): String = when (code) {
        PLANNED -> "План"
        IN_PROGRESS -> "В работе"
        DONE -> "Готово"
        else -> code
    }

    fun isActive(code: String): Boolean = code != DONE
}
