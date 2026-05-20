package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BusinessConfig
import com.example.data.Transaction
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val financialMetrics by viewModel.financialMetrics.collectAsStateWithLifecycle()
    val businessConfig by viewModel.businessConfig.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = businessConfig.businessName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = HighDensityTextDark
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Business Ledger • Realtime Books",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = HighDensitySubtext
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditConfigDialog = true },
                        modifier = Modifier.testTag("edit_business_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Business,
                            contentDescription = "Edit Business Profile",
                            tint = HighDensityM3Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HighDensityBg,
                    titleContentColor = HighDensityTextDark,
                    actionIconContentColor = HighDensityM3Primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = HighDensityM3Primary,
                contentColor = PureWhite,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add cash transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // METRIC CARDS HEADER
            FinancialOverviewGrid(
                metrics = financialMetrics,
                businessName = businessConfig.businessName
            )

            // BUDGET & TARGET tracker card
            BudgetGoalTrackerCard(
                expense = financialMetrics.totalExpense,
                config = businessConfig
            )

            // VISUAL ANALYTICS (CUSTOM CANVAS CHART)
            if (allTransactions.isNotEmpty()) {
                CashFlowAnalyticsCard(
                    allTransactions = allTransactions,
                    metrics = financialMetrics
                )
            }

            // CONTROLS HEADER (SEARCH & FILTERS)
            LedgerControls(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                selectedType = typeFilter,
                onTypeSelect = { viewModel.setTypeFilter(it) },
                selectedCategory = categoryFilter,
                onCategorySelect = { viewModel.setCategoryFilter(it) }
            )

            // TRANSACTION LIST
            Text(
                text = "Transaction Records",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextDark
                )
            )

            if (uiState.isEmpty()) {
                EmptyStateView(
                    isFilterActive = searchQuery.isNotEmpty() || typeFilter != null || categoryFilter != null,
                    onLoadDemo = { viewModel.loadDemoDataset() },
                    onClearFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.setTypeFilter(null)
                        viewModel.setCategoryFilter(null)
                    }
                )
            } else {
                Text(
                    text = "Showing ${uiState.size} items",
                    style = MaterialTheme.typography.bodySmall.copy(color = HighDensitySubtext)
                )

                // High density styled Recent Activity card container grouping real transaction cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = RecentActivityBg),
                    border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.forEach { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                onDelete = { viewModel.deleteTransaction(tx) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Quick Clear Ledger option for flexibility
                OutlinedButton(
                    onClick = { viewModel.clearAllTransactions() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseBar),
                    border = BorderStroke(1.dp, ExpenseBar.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Ledger Entries")
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB scroll clearances
        }
    }

    // FORM DIALOGS
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, type, category, notes ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    if (showEditConfigDialog) {
        EditBusinessConfigDialog(
            config = businessConfig,
            onDismiss = { showEditConfigDialog = false },
            onSave = { name, budget ->
                viewModel.updateBusinessConfig(name, budget)
                showEditConfigDialog = false
            }
        )
    }
}

// 1. FINANCIAL METRICS TILES
@Composable
fun FinancialOverviewGrid(
    metrics: FinancialMetrics,
    businessName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Broad Large Hero Card for Net Profit (using HeroBalance colors)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HeroBalanceBg),
            border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Ledger Balance",
                                tint = HeroBalanceText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NET OPERATING PROFIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HeroBalanceText,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        
                        // Beautiful surplus/deficit badge consistent with the design layout
                        Box(
                            modifier = Modifier
                                .background(
                                    color = PureWhite.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (metrics.netProfit >= 0) "SURPLUS" else "DEFICIT",
                                color = HeroBalanceText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatCurrency(metrics.netProfit),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = HeroBalanceText,
                            fontSize = 36.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Balance after recording all income and bills",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HeroBalanceText.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        // Row of Cash Flow Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cash In Card (Revenue styling)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RevenueBg),
                border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Cash In",
                            tint = RevenueBar,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REVENUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RevenueText
                            )
                        )
                    }
                    Text(
                        text = formatCurrency(metrics.totalIncome),
                        color = RevenueText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    // Styled progress bar matching the HTML design theme
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PureWhite.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.75f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RevenueBar)
                        )
                    }
                }
            }

            // Cash Out Card (Expense styling)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ExpenseBg),
                border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Cash Out",
                            tint = ExpenseBar,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXPENSES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseText
                            )
                        )
                    }
                    Text(
                        text = formatCurrency(metrics.totalExpense),
                        color = ExpenseText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    // Dynamic progress indicator based on Expense ratio to Revenue
                    val ratioProgress = if (metrics.totalIncome > 0) {
                        (metrics.totalExpense / metrics.totalIncome).coerceIn(0.0, 1.0).toFloat()
                    } else if (metrics.totalExpense > 0) {
                        1f
                    } else {
                        0f
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PureWhite.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratioProgress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ExpenseBar)
                        )
                    }
                }
            }
        }
    }
}

// 2. BUDGET TRACKING COMPONENT
@Composable
fun BudgetGoalTrackerCard(
    expense: Double,
    config: BusinessConfig,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (config.monthlyBudgetGoal > 0) {
        (expense / config.monthlyBudgetGoal).coerceIn(0.0, 1.1)
    } else {
        0.0
    }
    val percentage = (progressFraction * 100).toInt()
    val isOverBudget = expense > config.monthlyBudgetGoal

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RecentActivityBg),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverBudget) SolidRed.copy(alpha = 0.8f) else MutedBorder.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONTHLY SPENDING BUDGET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensitySubtext
                        )
                    )
                    Text(
                        text = "Goal Cap: ${formatCurrency(config.monthlyBudgetGoal)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HighDensityTextDark.copy(alpha = 0.8f)
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isOverBudget) SolidRed.copy(alpha = 0.15f) else HighDensityM3Primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percentage%",
                        color = if (isOverBudget) SolidRed else HighDensityM3Primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Beautiful High Density track indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(PureWhite)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.toFloat().coerceAtMost(1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            brush = if (isOverBudget) {
                                Brush.horizontalGradient(listOf(SolidRed, AlertOrange))
                            } else {
                                Brush.horizontalGradient(listOf(HighDensityM3Primary, AccentBlue))
                            }
                        )
                )
            }

            AnimatedVisibility(visible = isOverBudget) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Over Budget Warning",
                        tint = AlertOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Warning: Expenses exceed budget limit by ${formatCurrency(expense - config.monthlyBudgetGoal)}!",
                        color = AlertOrange,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// 3. FINANCIAL ANALYTICS (CUSTOM CANVAS BAR CHART)
@Composable
fun CashFlowAnalyticsCard(
    allTransactions: List<Transaction>,
    metrics: FinancialMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RecentActivityBg),
        border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CASH INFLOW vs OUTFLOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensitySubtext
                        )
                    )
                    Text(
                        text = "Current Ledger Visual Analytics",
                        style = MaterialTheme.typography.bodySmall.copy(color = HighDensitySubtext)
                    )
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RevenueBar))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revenue", style = MaterialTheme.typography.bodySmall.copy(color = HighDensityTextDark, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseBar))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expenses", style = MaterialTheme.typography.bodySmall.copy(color = HighDensityTextDark, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Canvas drawing
            val maxAmount = maxOf(metrics.totalIncome, metrics.totalExpense, 100.0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw Horizontal Dotted Grids
                    val gridLines = 4
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    for (i in 0..gridLines) {
                        val y = (canvasHeight / gridLines) * i
                        drawLine(
                            color = MutedBorder.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect
                        )
                    }

                    // Compute paired sizes
                    val barWidth = 40.dp.toPx()
                    val barSpacing = 48.dp.toPx()
                    val middleX = canvasWidth / 2

                    // Income bar dimensions
                    val incomeFraction = (metrics.totalIncome / maxAmount).toFloat()
                    val incomeBarHeight = canvasHeight * incomeFraction
                    val incomeLeft = middleX - barWidth - (barSpacing / 2)
                    val incomeTop = canvasHeight - incomeBarHeight

                    // Expense bar dimensions
                    val expenseFraction = (metrics.totalExpense / maxAmount).toFloat()
                    val expenseBarHeight = canvasHeight * expenseFraction
                    val expenseLeft = middleX + (barSpacing / 2)
                    val expenseTop = canvasHeight - expenseBarHeight

                    // DRAW REVENUE BAR (RevenueBar Rounded Pill)
                    if (metrics.totalIncome > 0) {
                        drawRoundRect(
                            color = RevenueBar,
                            topLeft = Offset(incomeLeft, incomeTop),
                            size = Size(barWidth, incomeBarHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    } else {
                        // Empty outline
                        drawRoundRect(
                            color = MutedBorder.copy(alpha = 0.5f),
                            topLeft = Offset(incomeLeft, canvasHeight - 10f),
                            size = Size(barWidth, 10f),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // DRAW EXPENSE BAR (ExpenseBar Rounded Pill)
                    if (metrics.totalExpense > 0) {
                        drawRoundRect(
                            color = ExpenseBar,
                            topLeft = Offset(expenseLeft, expenseTop),
                            size = Size(barWidth, expenseBarHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    } else {
                        // Empty outline
                        drawRoundRect(
                            color = MutedBorder.copy(alpha = 0.5f),
                            topLeft = Offset(expenseLeft, canvasHeight - 10f),
                            size = Size(barWidth, 10f),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Labels under matching columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "Total Inflows",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = HighDensitySubtext,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(end = 24.dp)
                )
                Text(
                    text = "Total Outflows",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = HighDensitySubtext,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }
    }
}

// 4. LEDGER CONTROLS (SEARCH & INTUITIVE CHIP FILTERING)
@Composable
fun LedgerControls(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedType: String?,
    onTypeSelect: (String?) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Filter by title, category, or note...", color = HighDensitySubtext) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = HighDensityM3Primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = HighDensitySubtext)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = HighDensityTextDark,
                unfocusedTextColor = HighDensityTextDark,
                focusedBorderColor = HighDensityM3Primary,
                unfocusedBorderColor = MutedBorder.copy(alpha = 0.6f),
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        // Segmented filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ALL types Chip
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelect(null) },
                label = { Text("All Ledger") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HighDensityM3Primary,
                    selectedLabelColor = PureWhite,
                    containerColor = PureWhite,
                    labelColor = HighDensitySubtext
                ),
                border = BorderStroke(1.dp, if (selectedType == null) HighDensityM3Primary else MutedBorder.copy(alpha = 0.5f))
            )

            // INCOME Chip
            FilterChip(
                selected = selectedType == Transaction.TYPE_INCOME,
                onClick = { onTypeSelect(Transaction.TYPE_INCOME) },
                label = { Text("Inflow ↗") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolidGreen,
                    selectedLabelColor = PureWhite,
                    containerColor = PureWhite,
                    labelColor = SolidGreen
                ),
                border = BorderStroke(1.dp, if (selectedType == Transaction.TYPE_INCOME) SolidGreen else MutedBorder.copy(alpha = 0.5f))
            )

            // EXPENSE Chip
            FilterChip(
                selected = selectedType == Transaction.TYPE_EXPENSE,
                onClick = { onTypeSelect(Transaction.TYPE_EXPENSE) },
                label = { Text("Bills Out ↘") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SolidRed,
                    selectedLabelColor = PureWhite,
                    containerColor = PureWhite,
                    labelColor = SolidRed
                ),
                border = BorderStroke(1.dp, if (selectedType == Transaction.TYPE_EXPENSE) SolidRed else MutedBorder.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.weight(1f))

            // Choose Category Filter Trigger
            Box {
                OutlinedButton(
                    onClick = { expandedCategoryDropdown = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedCategory != null) HighDensityM3Primary.copy(alpha = 0.12f) else PureWhite,
                        contentColor = if (selectedCategory != null) HighDensityM3Primary else HighDensitySubtext
                    ),
                    border = BorderStroke(1.dp, if (selectedCategory != null) HighDensityM3Primary else MutedBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Category Filter",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCategory ?: "Category",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val allAvailableCategories = if (selectedType == Transaction.TYPE_INCOME) {
                    Transaction.INCOME_CATEGORIES
                } else if (selectedType == Transaction.TYPE_EXPENSE) {
                    Transaction.EXPENSE_CATEGORIES
                } else {
                    Transaction.INCOME_CATEGORIES + Transaction.EXPENSE_CATEGORIES
                }

                DropdownMenu(
                    expanded = expandedCategoryDropdown,
                    onDismissRequest = { expandedCategoryDropdown = false },
                    modifier = Modifier.background(PureWhite)
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear Category Filter", color = AlertOrange) },
                        onClick = {
                            onCategorySelect(null)
                            expandedCategoryDropdown = false
                        }
                    )
                    allAvailableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, color = HighDensityTextDark) },
                            onClick = {
                                onCategorySelect(category)
                                expandedCategoryDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// 5. TRANSACTION ITEM CARD ROW
@Composable
fun TransactionItemRow(
    transaction: Transaction,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == Transaction.TYPE_INCOME
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateText = sdf.format(Date(transaction.timestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Visual Indicator Left Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = if (isIncome) SolidGreen.copy(alpha = 0.12f) else SolidRed.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) SolidGreen else SolidRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Core Info Card middle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    color = HighDensityTextDark,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.category,
                        color = HighDensityM3Primary,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "•",
                        color = HighDensitySubtext,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = dateText,
                        color = HighDensitySubtext,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!transaction.notes.isNullOrBlank()) {
                    Text(
                        text = transaction.notes,
                        color = HighDensitySubtext.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cost & Delete side
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${formatCurrency(transaction.amount)}",
                    color = if (isIncome) SolidGreen else SolidRed,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = HighDensitySubtext.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// 6. EMPTY STATE HELPER VIEW
@Composable
fun EmptyStateView(
    isFilterActive: Boolean,
    onLoadDemo: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RecentActivityBg),
        border = BorderStroke(1.dp, MutedBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MutedBorder.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFilterActive) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = HighDensityM3Primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isFilterActive) "No Search Results" else "Ledger is Empty",
                    color = HighDensityTextDark,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isFilterActive) {
                        "Try adjusting filters or searching other business titles."
                    } else {
                        "Get started by writing down your first receipt cashflow, or click below to load a demonstration ledger."
                    },
                    color = HighDensitySubtext,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (isFilterActive) {
                Button(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityM3Primary, contentColor = PureWhite)
                ) {
                    Text("Clear Applied Filters")
                }
            } else {
                Button(
                    onClick = onLoadDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = SolidGreen, contentColor = PureWhite)
                ) {
                    Text("Load Demonstration Dataset")
                }
            }
        }
    }
}

// 7. INPUT FORMS & DIALOG COMPONENTS

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, notes: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(Transaction.TYPE_INCOME) }
    var category by remember { mutableStateOf(Transaction.INCOME_CATEGORIES.first()) }
    var notes by remember { mutableStateOf("") }
    var inputErrorMsg by remember { mutableStateOf<String?>(null) }

    // Synchronize selected default category when Type changes
    LaunchedEffect(type) {
        category = if (type == Transaction.TYPE_INCOME) {
            Transaction.INCOME_CATEGORIES.first()
        } else {
            Transaction.EXPENSE_CATEGORIES.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RecentActivityBg),
            border = BorderStroke(1.dp, MutedBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Title and Close Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = HighDensityTextDark
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextDark)
                    }
                }

                // SEGMENTED CASHFLOW TYPE SWITCH (INCOME / EXPENSE)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureWhite)
                        .border(1.dp, MutedBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Income button
                    Button(
                        onClick = { type = Transaction.TYPE_INCOME },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == Transaction.TYPE_INCOME) SolidGreen else Color.Transparent,
                            contentColor = if (type == Transaction.TYPE_INCOME) PureWhite else HighDensitySubtext
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Revenue In (+)", fontWeight = FontWeight.Bold)
                    }

                    // Expense button
                    Button(
                        onClick = { type = Transaction.TYPE_EXPENSE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == Transaction.TYPE_EXPENSE) SolidRed else Color.Transparent,
                            contentColor = if (type == Transaction.TYPE_EXPENSE) PureWhite else HighDensitySubtext
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Expense Out (-)", fontWeight = FontWeight.Bold)
                    }
                }

                // TRANSACTION TITLE input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Client/Vendor/Item Title", color = HighDensitySubtext) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighDensityTextDark,
                        unfocusedTextColor = HighDensityTextDark,
                        focusedBorderColor = HighDensityM3Primary,
                        unfocusedBorderColor = MutedBorder,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // AMOUNT input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Cash Amount ($)", color = HighDensitySubtext) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighDensityTextDark,
                        unfocusedTextColor = HighDensityTextDark,
                        focusedBorderColor = HighDensityM3Primary,
                        unfocusedBorderColor = MutedBorder,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // CATEGORIES CHIPS CONTAINER FOR THE ACTIVE TYPE
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextDark
                        )
                    )

                    val activeCategories = if (type == Transaction.TYPE_INCOME) {
                        Transaction.INCOME_CATEGORIES
                    } else {
                        Transaction.EXPENSE_CATEGORIES
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCategories.forEach { cat ->
                            val isSelected = category == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (type == Transaction.TYPE_INCOME) SolidGreen else SolidRed,
                                    selectedLabelColor = PureWhite,
                                    containerColor = PureWhite,
                                    labelColor = HighDensitySubtext
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MutedBorder.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // OPTIONAL NOTES
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", color = HighDensitySubtext) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighDensityTextDark,
                        unfocusedTextColor = HighDensityTextDark,
                        focusedBorderColor = HighDensityM3Primary,
                        unfocusedBorderColor = MutedBorder,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (inputErrorMsg != null) {
                    Text(
                        text = inputErrorMsg!!,
                        color = SolidRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // ACTIONS Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MutedBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val doubleAmount = amount.toDoubleOrNull()
                            if (title.isBlank()) {
                                inputErrorMsg = "Transaction title cannot be empty."
                            } else if (doubleAmount == null || doubleAmount <= 0) {
                                inputErrorMsg = "Please enter a valid positive amount."
                            } else {
                                onSave(title, doubleAmount, type, category, notes.ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == Transaction.TYPE_INCOME) SolidGreen else SolidRed,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_transaction_button")
                    ) {
                        Text("Record Cash")
                    }
                }
            }
        }
    }
}

@Composable
fun EditBusinessConfigDialog(
    config: BusinessConfig,
    onDismiss: () -> Unit,
    onSave: (name: String, budget: Double) -> Unit
) {
    var name by remember { mutableStateOf(config.businessName) }
    var budget by remember { mutableStateOf(config.monthlyBudgetGoal.toString()) }
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RecentActivityBg),
            border = BorderStroke(1.dp, MutedBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Business Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = HighDensityTextDark
                    )
                )

                // Business Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Business / Company Name", color = HighDensitySubtext) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighDensityTextDark,
                        unfocusedTextColor = HighDensityTextDark,
                        focusedBorderColor = HighDensityM3Primary,
                        unfocusedBorderColor = MutedBorder,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Monthly budget cap
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Monthly Expense Limit ($)", color = HighDensitySubtext) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HighDensityTextDark,
                        unfocusedTextColor = HighDensityTextDark,
                        focusedBorderColor = HighDensityM3Primary,
                        unfocusedBorderColor = MutedBorder,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationErrorMsg != null) {
                    Text(
                        text = validationErrorMsg!!,
                        color = SolidRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Cancel / Save row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MutedBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextDark),
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val isNameValid = name.isNotBlank()
                            val doubleBudget = budget.toDoubleOrNull()
                            if (!isNameValid) {
                                validationErrorMsg = "Business Name cannot be blank."
                            } else if (doubleBudget == null || doubleBudget < 0) {
                                validationErrorMsg = "Please enter a valid monthly limit."
                            } else {
                                onSave(name, doubleBudget)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityM3Primary, contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_profile_button")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}

// 8. FORMAT HELPER FUNCTIONS

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}
