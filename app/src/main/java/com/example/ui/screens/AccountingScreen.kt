package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CashRegisterSessionEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.OrderEntity
import com.example.ui.components.ReceiptDialog
import com.example.utils.PersianUtils
import com.example.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val config by viewModel.storeConfig.collectAsState()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: بستن صندوق (Z-Report), 1: سود و زیان (P&L), 2: هزینه‌ها, 3: فاکتورهای فروش
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showCloseSessionDialog by remember { mutableStateOf(false) }
    var showOpenSessionDialog by remember { mutableStateOf(false) }
    var selectedOrderForReceipt by remember { mutableStateOf<OrderEntity?>(null) }

    // Aggregate Calculations
    val totalGrossSales = orders.sumOf { it.subtotal }
    val totalDiscounts = orders.sumOf { it.discountAmount }
    val totalNetSales = orders.sumOf { it.netAmount }
    val totalCogs = orders.sumOf { it.cogsAmount }
    val totalGrossProfit = totalNetSales - totalCogs
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalGrossProfit - totalExpenses
    val totalVatCollected = orders.sumOf { it.taxAmount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub Navigation Tabs
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("صندوق و Z", "سود و زیان", "هزینه‌ها", "فاکتورها").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                ) {
                    Text(label, fontSize = 11.sp, fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        when (selectedSubTab) {
            // 0: Cash Register & Z-Report
            0 -> {
                CashRegisterSection(
                    session = currentSession,
                    orders = orders,
                    expenses = expenses,
                    onOpenSession = { showOpenSessionDialog = true },
                    onCloseSession = { showCloseSessionDialog = true }
                )
            }
            // 1: P&L Statement (سود و زیان واقعی)
            1 -> {
                ProfitAndLossSection(
                    grossSales = totalGrossSales,
                    discounts = totalDiscounts,
                    netSales = totalNetSales,
                    cogs = totalCogs,
                    grossProfit = totalGrossProfit,
                    expenses = totalExpenses,
                    netProfit = netProfit,
                    vat = totalVatCollected,
                    ordersCount = orders.size
                )
            }
            // 2: Expenses Management
            2 -> {
                ExpensesSection(
                    expenses = expenses,
                    onAddExpense = { showAddExpenseDialog = true },
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
            }
            // 3: Sales Orders Log
            3 -> {
                OrdersLogSection(
                    orders = orders,
                    onViewReceipt = { selectedOrderForReceipt = it }
                )
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, cat, amt, source, desc ->
                viewModel.addExpense(title, cat, amt, source, desc)
                showAddExpenseDialog = false
            }
        )
    }

    // Close Session Dialog (گزارش Z)
    if (showCloseSessionDialog && currentSession != null) {
        CloseSessionDialog(
            session = currentSession!!,
            orders = orders,
            expenses = expenses,
            onDismiss = { showCloseSessionDialog = false },
            onConfirm = { actualCash ->
                viewModel.closeCurrentSession(actualCash)
                showCloseSessionDialog = false
            }
        )
    }

    // Open Session Dialog
    if (showOpenSessionDialog) {
        OpenSessionDialog(
            onDismiss = { showOpenSessionDialog = false },
            onConfirm = { cash, cashier ->
                viewModel.openNewSession(cash, cashier)
                showOpenSessionDialog = false
            }
        )
    }

    // Receipt reprint
    if (selectedOrderForReceipt != null) {
        ReceiptDialog(
            order = selectedOrderForReceipt!!,
            config = config ?: com.example.data.local.entity.StoreConfigEntity(),
            onDismiss = { selectedOrderForReceipt = null }
        )
    }
}

@Composable
fun CashRegisterSection(
    session: CashRegisterSessionEntity?,
    orders: List<OrderEntity>,
    expenses: List<ExpenseEntity>,
    onOpenSession: () -> Unit,
    onCloseSession: () -> Unit
) {
    if (session == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text("در حال حاضر شیفت یا صندوقی باز نیست", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("برای شروع ثبت فروش روزانه و کنترل موجودی صندوق، شیفت جدید باز کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Button(onClick = onOpenSession, modifier = Modifier.testTag("open_session_btn")) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("باز کردن شیفت و صندوق جدید")
                }
            }
        }
        return
    }

    val sessionOrders = orders.filter { it.timestamp >= session.openedAt }
    val cashSales = sessionOrders.sumOf { it.cashPaid }
    val cardSales = sessionOrders.sumOf { it.cardPaid }
    val creditSales = sessionOrders.sumOf { it.creditPaid }
    val chequeSales = sessionOrders.sumOf { it.chequePaid }

    val sessionExpenses = expenses.filter { it.timestamp >= session.openedAt && it.paidFrom == "صندوق نقدی" }
    val cashExpenses = sessionExpenses.sumOf { it.amount }

    val expectedCashInTill = session.openingBalance + cashSales - cashExpenses

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Badge(containerColor = Color(0xFF059669)) {
                                Text("شیفت فعال", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(session.cashierName, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "شروع: ${PersianUtils.formatToPersianDateTime(session.openedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("موجودی اولیه صندوق (پول خرد):", style = MaterialTheme.typography.bodySmall)
                        Text(PersianUtils.formatCurrency(session.openingBalance), fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("موجودی محاسباتی فعلی داخل کشو:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(
                            PersianUtils.formatCurrency(expectedCashInTill),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ریز عملکرد شیفت جاری (گزارش X)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    AccountingRow(label = "فروش نقدی ورودی به صندوق:", value = PersianUtils.formatCurrency(cashSales), valueColor = Color(0xFF059669))
                    AccountingRow(label = "فروش پوز و کارت‌خوان بانکی:", value = PersianUtils.formatCurrency(cardSales))
                    AccountingRow(label = "فروش نسیه و حساب دفتری:", value = PersianUtils.formatCurrency(creditSales), valueColor = Color(0xFFD97706))
                    AccountingRow(label = "فروش با چک صیادی:", value = PersianUtils.formatCurrency(chequeSales))
                    AccountingRow(label = "هزینه‌های پرداخت شده از کشوی صندوق:", value = "- ${PersianUtils.formatCurrency(cashExpenses)}", valueColor = Color(0xFFDC2626))

                    HorizontalDivider()

                    AccountingRow(
                        label = "تعداد فاکتورهای صادر شده این شیفت:",
                        value = "${PersianUtils.toPersianDigits(sessionOrders.size.toString())} فاکتور",
                        isBold = true
                    )
                }
            }
        }

        item {
            Button(
                onClick = onCloseSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("close_shift_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("بستن صندوق و صدور گزارش Z (مغایرت‌گیری)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfitAndLossSection(
    grossSales: Double,
    discounts: Double,
    netSales: Double,
    cogs: Double,
    grossProfit: Double,
    expenses: Double,
    netProfit: Double,
    vat: Double,
    ordersCount: Int
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            // Net Profit Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (netProfit >= 0) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "سود خالص نهایی کسب‌وکار",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (netProfit >= 0) Color(0xFF065F46) else Color(0xFF9F1239),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = PersianUtils.formatCurrency(netProfit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (netProfit >= 0) Color(0xFF047857) else Color(0xFFBE123C)
                    )
                    Text(
                        text = "بر اساس ${PersianUtils.toPersianDigits(ordersCount.toString())} فاکتور فروش ثبت شده و هزینه‌های جاری",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("صورت سود و زیان دقیق (P&L)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    AccountingRow(label = "فروش ناخالص (Gross Sales):", value = PersianUtils.formatCurrency(grossSales))
                    AccountingRow(label = "تخفیفات اعطا شده به مشتریان:", value = "- ${PersianUtils.formatCurrency(discounts)}", valueColor = Color(0xFFDC2626))
                    AccountingRow(label = "فروش خالص (Net Sales):", value = PersianUtils.formatCurrency(netSales), isBold = true)

                    HorizontalDivider()

                    AccountingRow(label = "بهای تمام شده کالای فروش رفته (COGS):", value = "- ${PersianUtils.formatCurrency(cogs)}", valueColor = Color(0xFFB45309))
                    AccountingRow(label = "سود ناخالص بازرگانی:", value = PersianUtils.formatCurrency(grossProfit), isBold = true, valueColor = Color(0xFF059669))

                    HorizontalDivider()

                    AccountingRow(label = "مجموع هزینه‌های جاری و دفتری:", value = "- ${PersianUtils.formatCurrency(expenses)}", valueColor = Color(0xFFDC2626))
                    AccountingRow(label = "سود خالص عملیاتی نهایی:", value = PersianUtils.formatCurrency(netProfit), isBold = true, valueColor = if (netProfit >= 0) Color(0xFF047857) else Color(0xFFDC2626))

                    HorizontalDivider()
                    AccountingRow(label = "مالیات بر ارزش افزوده وصول شده (۱۰٪):", value = PersianUtils.formatCurrency(vat), valueColor = Color(0xFF2563EB))
                }
            }
        }
    }
}

@Composable
fun ExpensesSection(
    expenses: List<ExpenseEntity>,
    onAddExpense: () -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("هزینه‌های جاری مغازه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAddExpense,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_expense_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ثبت هزینه جدید", fontSize = 12.sp)
            }
        }

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز هزینه‌ای ثبت نشده است.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses, key = { it.id }) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                        Text(expense.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                    Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = "پرداخت از: ${expense.paidFrom} | ${PersianUtils.formatToPersianDateTime(expense.timestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    PersianUtils.formatCurrency(expense.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                                IconButton(onClick = { onDeleteExpense(expense) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersLogSection(
    orders: List<OrderEntity>,
    onViewReceipt: (OrderEntity) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("فاکتوری ثبت نشده است.", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders, key = { it.orderId }) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(PersianUtils.toPersianDigits(order.invoiceNumber), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Badge(
                                    containerColor = when (order.paymentMethod) {
                                        "CASH" -> Color(0xFFD1FAE5)
                                        "POS_CARD" -> Color(0xFFE0E7FF)
                                        "CREDIT" -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                ) {
                                    Text(
                                        text = when (order.paymentMethod) {
                                            "CASH" -> "نقد"
                                            "POS_CARD" -> "کارت‌خوان"
                                            "CREDIT" -> "نسیه"
                                            "CHEQUE" -> "چک"
                                            else -> "ترکیبی"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${order.customerName} | ${PersianUtils.formatToPersianDateTime(order.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "سود فاکتور: ${PersianUtils.formatCurrency(order.grossProfit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(PersianUtils.formatCurrency(order.netAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { onViewReceipt(order) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Receipt, contentDescription = "مشاهده فاکتور", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountingRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("قبوض") }
    var amountStr by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("صندوق نقدی") }

    val categories = listOf("اجاره", "قبوض", "حقوق پرسنل", "پیک و ارسال", "بسته‌بندی", "پذیرایی", "تعمیرات", "متفرقه")
    val sources = listOf("صندوق نقدی", "کارت بانکی", "تنخواه")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت هزینه جدید مغازه", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هزینه *") },
                    placeholder = { Text("مثال: قبض برق، خرید چای و قند...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ هزینه (تومان) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("دسته‌بندی هزینه:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                Text("محل پرداخت:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sources.forEach { s ->
                        FilterChip(
                            selected = source == s,
                            onClick = { source = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title, category, amt, source, "")
                    }
                },
                enabled = title.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("ثبت هزینه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun CloseSessionDialog(
    session: CashRegisterSessionEntity,
    orders: List<OrderEntity>,
    expenses: List<ExpenseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val sessionOrders = orders.filter { it.timestamp >= session.openedAt }
    val cashSales = sessionOrders.sumOf { it.cashPaid }
    val cashExpenses = expenses.filter { it.timestamp >= session.openedAt && it.paidFrom == "صندوق نقدی" }.sumOf { it.amount }
    val expectedCash = session.openingBalance + cashSales - cashExpenses

    var actualCashStr by remember { mutableStateOf(expectedCash.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بستن شیفت و صدور گزارش Z", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("موجودی محاسباتی مورد انتظار صندوق: ${PersianUtils.formatCurrency(expectedCash)}")

                OutlinedTextField(
                    value = actualCashStr,
                    onValueChange = { actualCashStr = it },
                    label = { Text("مبلغ فیزیکی شمرده شده در کشو (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                val counted = actualCashStr.toDoubleOrNull() ?: expectedCash
                val diff = counted - expectedCash
                if (diff != 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (diff < 0) Color(0xFFFFE4E6) else Color(0xFFD1FAE5))
                    ) {
                        Text(
                            text = if (diff < 0) "کسری صندوق: ${PersianUtils.formatCurrency(-diff)}"
                            else "مازاد صندوق: ${PersianUtils.formatCurrency(diff)}",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (diff < 0) Color(0xFF9F1239) else Color(0xFF065F46)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actual = actualCashStr.toDoubleOrNull() ?: expectedCash
                    onConfirm(actual)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("تایید نهایی و بستن شیفت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun OpenSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var openingCashStr by remember { mutableStateOf("500000") }
    var cashierName by remember { mutableStateOf("صندوقدار") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("باز کردن شیفت صندوق", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text("نام صندوقدار یا شیفت") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = openingCashStr,
                    onValueChange = { openingCashStr = it },
                    label = { Text("موجودی اولیه صندوق (پول خرد - تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = openingCashStr.toDoubleOrNull() ?: 0.0
                    onConfirm(cash, cashierName)
                }
            ) {
                Text("شروع شیفت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
