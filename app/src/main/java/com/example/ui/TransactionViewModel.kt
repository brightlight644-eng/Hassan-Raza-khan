package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BusinessConfig
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<String?>(null) // null = ALL, "INCOME", "EXPENSE"
    val selectedCategoryFilter = MutableStateFlow<String?>(null) // null = ALL

    // Business Config Flow
    val businessConfig: StateFlow<BusinessConfig> = repository.businessConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BusinessConfig()
        )

    // Raw Transactions Flow
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter
    ) { txList, query, type, cat ->
        txList.filter { tx ->
            val matchesQuery = tx.title.contains(query, ignoreCase = true) || 
                             (tx.notes?.contains(query, ignoreCase = true) ?: false) ||
                             tx.category.contains(query, ignoreCase = true)
            val matchesType = type == null || tx.type == type
            val matchesCategory = cat == null || tx.category == cat
            matchesQuery && matchesType && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Financial Metrics derived reactively from unfiltered allTransactions
    val financialMetrics = repository.allTransactions.map { txList ->
        val income = txList.filter { it.type == Transaction.TYPE_INCOME }.sumOf { it.amount }
        val expense = txList.filter { it.type == Transaction.TYPE_EXPENSE }.sumOf { it.amount }
        val profit = income - expense
        FinancialMetrics(
            totalIncome = income,
            totalExpense = expense,
            netProfit = profit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialMetrics(0.0, 0.0, 0.0)
    )

    // Quick filter helpers
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setTypeFilter(type: String?) {
        selectedTypeFilter.value = type
        selectedCategoryFilter.value = null // Reset category filter when switching types
    }

    fun setCategoryFilter(category: String?) {
        selectedCategoryFilter.value = category
    }

    // DB Operations
    fun addTransaction(title: String, amount: Double, type: String, category: String, timestamp: Long, notes: String?) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp,
                    notes = notes
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateBusinessConfig(name: String, budget: Double) {
        viewModelScope.launch {
            val current = repository.getBusinessConfigDirect()
            repository.saveBusinessConfig(
                current.copy(businessName = name, monthlyBudgetGoal = budget)
            )
        }
    }

    // Populate a mock demo dataset for testing/first launch
    fun loadDemoDataset() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hourInMillis = 3600000L
            val dayInMillis = 24 * hourInMillis

            val demoData = listOf(
                Transaction(title = "Client Project Invoice Payment", amount = 3200.0, type = Transaction.TYPE_INCOME, category = "Sales & Revenue", timestamp = now - 1 * hourInMillis, notes = "Direct deposit from Acme Corp"),
                Transaction(title = "Co-Working Office Rent", amount = 850.0, type = Transaction.TYPE_EXPENSE, category = "Rent & Lease", timestamp = now - 1 * dayInMillis, notes = "Monthly desk leasing"),
                Transaction(title = "Hourly Consulting Consultation", amount = 450.0, type = Transaction.TYPE_INCOME, category = "Consulting", timestamp = now - 3 * dayInMillis, notes = "Consulting for Startup Inc"),
                Transaction(title = "AWS Hosting Services", amount = 142.50, type = Transaction.TYPE_EXPENSE, category = "Software & Subs", timestamp = now - 4 * dayInMillis, notes = "Production server scaling"),
                Transaction(title = "SaaS Email Marketing Fee", amount = 49.00, type = Transaction.TYPE_EXPENSE, category = "Software & Subs", timestamp = now - 5 * dayInMillis),
                Transaction(title = "Social Media Ads Setup", amount = 300.0, type = Transaction.TYPE_EXPENSE, category = "Marketing & Ads", timestamp = now - 7 * dayInMillis, notes = "Facebook and Google search ads"),
                Transaction(title = "Enterprise Client Retainer", amount = 1500.0, type = Transaction.TYPE_INCOME, category = "Sales & Revenue", timestamp = now - 8 * dayInMillis, notes = "Invoice #2041"),
                Transaction(title = "Workspace High-Speed Fiber", amount = 110.0, type = Transaction.TYPE_EXPENSE, category = "Utilities & Internet", timestamp = now - 10 * dayInMillis, notes = "Fiber optic commercial plan")
            )

            // Overwrite business config with demonstration values
            updateBusinessConfig("Creative Agency Ltd", 2000.0)

            // Insert each transaction
            for (tx in demoData) {
                repository.insertTransaction(tx)
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            // Drop transactions
            val txs = repository.allTransactions.first()
            for (t in txs) {
                repository.deleteTransaction(t)
            }
        }
    }
}

data class FinancialMetrics(
    val totalIncome: Double,
    val totalExpense: Double,
    val netProfit: Double
)

class TransactionViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
