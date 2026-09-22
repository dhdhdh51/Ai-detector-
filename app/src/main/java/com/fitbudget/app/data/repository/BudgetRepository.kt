package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.ExpenseDao
import com.fitbudget.app.data.database.entity.ExpenseEntity
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val expenseDao: ExpenseDao,
    private val dayRepository: DayRepository
) {

    fun observeExpenses(epochDay: Long): Flow<List<ExpenseEntity>> = expenseDao.observeForDay(epochDay)

    fun observeExtraSpend(epochDay: Long): Flow<Double> = expenseDao.observeTotalForDay(epochDay)

    suspend fun extraSpend(epochDay: Long): Double = expenseDao.totalForDay(epochDay)

    suspend fun addExpense(
        label: String,
        amount: Double,
        epochDay: Long = DateTimeUtils.todayEpochDay()
    ): Long {
        dayRepository.ensureDay(epochDay)
        return expenseDao.insert(
            ExpenseEntity(
                epochDay = epochDay,
                label = label.trim().ifEmpty { "Extra food spend" },
                amount = amount.coerceAtLeast(0.0),
                createdAtMillis = DateTimeUtils.nowMillis()
            )
        )
    }

    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)

    suspend fun getAll(): List<ExpenseEntity> = expenseDao.getAll()

    suspend fun clear() = expenseDao.clear()
}
