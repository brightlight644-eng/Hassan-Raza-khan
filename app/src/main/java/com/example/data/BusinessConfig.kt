package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_config")
data class BusinessConfig(
    @PrimaryKey val id: String = "default_config",
    val businessName: String = "My Business",
    val monthlyBudgetGoal: Double = 5000.0
)
