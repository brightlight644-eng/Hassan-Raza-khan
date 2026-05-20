package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val timestamp: Long,
    val notes: String? = null
) {
    companion object {
        const val TYPE_INCOME = "INCOME"
        const val TYPE_EXPENSE = "EXPENSE"

        val INCOME_CATEGORIES = listOf(
            "Sales & Revenue",
            "Consulting",
            "Investments",
            "Grants & Loans",
            "Other Income"
        )

        val EXPENSE_CATEGORIES = listOf(
            "Rent & Lease",
            "Utilities & Internet",
            "Salaries & Contracts",
            "Marketing & Ads",
            "Software & Subs",
            "Supplies & Goods",
            "Travel & Entertainment",
            "Taxes & Licenses",
            "Other Expense"
        )
    }
}
