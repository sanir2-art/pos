package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.CustomerEntity
import com.example.utils.PersianUtils
import com.example.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersAndChequesScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val cheques by viewModel.cheques.collectAsState()
    val config by viewModel.storeConfig.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: مشتریان و نسیه, 1: چک‌های صیادی
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var settlingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddChequeDialog by remember { mutableStateOf(false) }

    val totalDebts = customers.filter { it.balance > 0 }.sumOf { it.balance }
    val totalPendingCheques = cheques.filter { it.status == "PENDING" }.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tab switch
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("حساب دفتری و نسیه", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("دفتر چک‌های صیادی", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
            }
        }

        if (selectedTab == 0) {
            // Debtors & Customers
            CustomersDebtorsView(
                customers = customers,
                totalDebts = totalDebts,
                storeName = config?.storeName ?: "فروشگاه",
                onAddCustomer = { showAddCustomerDialog = true },
                onSettle = { settlingCustomer = it }
            )
        } else {
            // Sayad Cheques
            ChequesView(
                cheques = cheques,
                totalPending = totalPendingCheques,
                onUpdateStatus = { id, st -> viewModel.updateChequeStatus(id, st) }
            )
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, limit, addr ->
                viewModel.addCustomer(name, phone, limit, addr)
                showAddCustomerDialog = false
            }
        )
    }

    // Settle Debt Dialog
    if (settlingCustomer != null) {
        SettleDebtDialog(
            customer = settlingCustomer!!,
            onDismiss = { settlingCustomer = null },
            onConfirm = { amt ->
                viewModel.settleCustomerDebt(settlingCustomer!!.id, amt)
                settlingCustomer = null
            }
        )
    }
}

@Composable
fun CustomersDebtorsView(
    customers: List<CustomerEntity>,
    totalDebts: Double,
    storeName: String,
    onAddCustomer: () -> Unit,
    onSettle: (CustomerEntity) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Debts summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مجموع مطالبات و نسیه دفتری:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF92400E))
                    Text(PersianUtils.formatCurrency(totalDebts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                }
                Button(
                    onClick = onAddCustomer,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    modifier = Modifier.testTag("add_customer_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشتری جدید", fontSize = 11.sp)
                }
            }
        }

        // List of Customers
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customers, key = { it.id }) { customer ->
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
                            Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("تلفن: ${PersianUtils.toPersianDigits(customer.phone)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                            if (customer.balance > 0) {
                                Text(
                                    text = "بدهی: ${PersianUtils.formatCurrency(customer.balance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text("تسویه / بدون بدهی", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (customer.balance > 0) {
                                // SMS Reminder
                                IconButton(
                                    onClick = {
                                        val smsBody = "مشتری گرامی ${customer.name}، مانده بدهی حساب دفتری شما در $storeName مبلغ ${PersianUtils.formatCurrency(customer.balance)} می‌باشد. خواهشمند است جهت تسویه اقدام فرمایید."
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, smsBody)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(intent, "ارسال پیامک یادآوری بدهی"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "پیامک یادآوری", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }

                                Button(
                                    onClick = { onSettle(customer) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("تسویه", fontSize = 11.sp)
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
fun ChequesView(
    cheques: List<ChequeEntity>,
    totalPending: Double,
    onUpdateStatus: (Long, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("چک‌های صیادی در انتظار وصول:", style = MaterialTheme.typography.labelSmall)
                    Text(PersianUtils.formatCurrency(totalPending), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text("${PersianUtils.toPersianDigits(cheques.count { it.status == "PENDING" }.toString())} فقره", modifier = Modifier.padding(4.dp))
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cheques, key = { it.id }) { cheque ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${cheque.bankName} - ${cheque.accountOwner}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Badge(
                                containerColor = when (cheque.status) {
                                    "PASSED" -> Color(0xFFD1FAE5)
                                    "BOUNCED" -> Color(0xFFFFE4E6)
                                    else -> Color(0xFFFEF3C7)
                                }
                            ) {
                                Text(
                                    text = when (cheque.status) {
                                        "PASSED" -> "وصول شده"
                                        "BOUNCED" -> "برگشتی"
                                        else -> "در انتظار وصول"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (cheque.status) {
                                        "PASSED" -> Color(0xFF065F46)
                                        "BOUNCED" -> Color(0xFF9F1239)
                                        else -> Color(0xFF92400E)
                                    }
                                )
                            }
                        }

                        Text("شناسه ۱۶ رقمی صیاد: ${PersianUtils.toPersianDigits(cheque.sayadNumber)}", style = MaterialTheme.typography.bodySmall)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سررسید: ${PersianUtils.toPersianDigits(cheque.dueDateJalali)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(PersianUtils.formatCurrency(cheque.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (cheque.status == "PENDING") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(cheque.id, "PASSED") },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("اعلام وصول چک", fontSize = 11.sp, color = Color(0xFF059669))
                                }
                                OutlinedButton(
                                    onClick = { onUpdateStatus(cheque.id, "BOUNCED") },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text("برگشت خورد", fontSize = 11.sp, color = Color(0xFFDC2626))
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
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var creditLimitStr by remember { mutableStateOf("10000000") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعریف مشتری جدید و حساب دفتری", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره موبایل *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = creditLimitStr,
                    onValueChange = { creditLimitStr = it },
                    label = { Text("سقف اعتبار نسیه (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("آدرس مشتری") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = creditLimitStr.toDoubleOrNull() ?: 10_000_000.0
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, limit, address)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("ثبت مشتری")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun SettleDebtDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var settleAmountStr by remember { mutableStateOf(customer.balance.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسویه بدهی حساب دفتری", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مشتری: ${customer.name}")
                Text("کل مانده بدهی فعلی: ${PersianUtils.formatCurrency(customer.balance)}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))

                OutlinedTextField(
                    value = settleAmountStr,
                    onValueChange = { settleAmountStr = it },
                    label = { Text("مبلغ پرداختی مشتری (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = settleAmountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt)
                    }
                }
            ) {
                Text("ثبت دریافت و کسر از بدهی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
