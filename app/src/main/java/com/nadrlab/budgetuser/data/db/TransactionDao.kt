package com.nadrlab.budgetuser.data.db

import androidx.room.*
import com.nadrlab.budgetuser.data.model.Transaction
import com.nadrlab.budgetuser.data.model.TransactionType
import com.nadrlab.budgetuser.data.model.UserSummaryData
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsOnce(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE storeId = :storeId ORDER BY date DESC")
    fun getTransactionsByStore(storeId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE storeId = :storeId AND type = 'PURCHASE'")
    fun getTotalPurchases(storeId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE storeId = :storeId AND type = 'PAYMENT'")
    fun getTotalPayments(storeId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'PURCHASE' AND date BETWEEN :startDate AND :endDate")
    fun getTotalPurchasesByDateRange(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'PAYMENT' AND date BETWEEN :startDate AND :endDate")
    fun getTotalPaymentsByDateRange(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'PURCHASE'")
    fun getAllTimePurchases(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'PAYMENT'")
    fun getAllTimePayments(): Flow<Double>

    @Query("""
        SELECT senderTag,
               COALESCE(SUM(CASE WHEN type = 'PURCHASE' THEN amount ELSE 0 END), 0) as totalPurchases,
               COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) as totalPayments
        FROM transactions
        WHERE senderTag != ''
        GROUP BY senderTag
        ORDER BY senderTag ASC
    """)
    fun getUserSummaries(): Flow<List<UserSummaryData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
