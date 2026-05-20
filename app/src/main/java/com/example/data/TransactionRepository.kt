package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()

    val businessConfig: Flow<BusinessConfig> = transactionDao.getBusinessConfigFlow().map { config ->
        config ?: BusinessConfig()
    }

    suspend fun getBusinessConfigDirect(): BusinessConfig {
        return transactionDao.getBusinessConfigDirect() ?: BusinessConfig()
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun saveBusinessConfig(config: BusinessConfig) {
        transactionDao.insertOrUpdateBusinessConfig(config)
    }
}
