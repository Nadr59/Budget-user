package com.nadrlab.budgetuser.data

import com.nadrlab.budgetuser.data.db.StoreDao
import com.nadrlab.budgetuser.data.db.TransactionDao
import com.nadrlab.budgetuser.data.model.Store
import com.nadrlab.budgetuser.data.model.Transaction
import com.nadrlab.budgetuser.data.model.UserSummaryData
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val storeDao: StoreDao,
    private val transactionDao: TransactionDao
) {
    fun getAllStores(): Flow<List<Store>> = storeDao.getAllStores()
    suspend fun getStoreById(id: Long): Store? = storeDao.getStoreById(id)
    suspend fun insertStore(store: Store): Long = storeDao.insertStore(store)
    suspend fun updateStore(store: Store) = storeDao.updateStore(store)
    suspend fun deleteStore(store: Store) = storeDao.deleteStore(store)

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    fun getTransactionsByStore(storeId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByStore(storeId)
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByDateRange(start, end)
    suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    fun getTotalPurchases(storeId: Long): Flow<Double> = transactionDao.getTotalPurchases(storeId)
    fun getTotalPayments(storeId: Long): Flow<Double> = transactionDao.getTotalPayments(storeId)
    fun getAllTimePurchases(): Flow<Double> = transactionDao.getAllTimePurchases()
    fun getAllTimePayments(): Flow<Double> = transactionDao.getAllTimePayments()
    fun getTotalPurchasesByDateRange(start: Long, end: Long): Flow<Double> = transactionDao.getTotalPurchasesByDateRange(start, end)
    fun getTotalPaymentsByDateRange(start: Long, end: Long): Flow<Double> = transactionDao.getTotalPaymentsByDateRange(start, end)

    fun getUserSummaries(): Flow<List<UserSummaryData>> = transactionDao.getUserSummaries()
}
