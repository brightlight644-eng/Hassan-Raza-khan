package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // Business Config
    @Query("SELECT * FROM business_config WHERE id = 'default_config' LIMIT 1")
    fun getBusinessConfigFlow(): Flow<BusinessConfig?>

    @Query("SELECT * FROM business_config WHERE id = 'default_config' LIMIT 1")
    suspend fun getBusinessConfigDirect(): BusinessConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBusinessConfig(config: BusinessConfig)
}
