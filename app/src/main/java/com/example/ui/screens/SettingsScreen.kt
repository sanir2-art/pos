package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StoreConfigEntity
import com.example.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.storeConfig.collectAsState()
    val current = config ?: StoreConfigEntity()

    var wooUrl by remember(current.wooUrl) { mutableStateOf(current.wooUrl) }
    var consumerKey by remember(current.consumerKey) { mutableStateOf(current.consumerKey) }
    var consumerSecret by remember(current.consumerSecret) { mutableStateOf(current.consumerSecret) }
    var showSecret by remember { mutableStateOf(false) }

    var storeName by remember(current.storeName) { mutableStateOf(current.storeName) }
    var storePhone by remember(current.storePhone) { mutableStateOf(current.storePhone) }
    var storeAddress by remember(current.storeAddress) { mutableStateOf(current.storeAddress) }
    var footerNote by remember(current.invoiceFooterNote) { mutableStateOf(current.invoiceFooterNote) }
    var taxRateStr by remember(current.taxRatePercent) { mutableStateOf(current.taxRatePercent.toInt().toString()) }

    var testStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showSaveToast by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // WooCommerce REST API Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("تنظیمات اتصال به سایت و ووکامرس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "برای همگام‌سازی محصولات سایت و ارسال خودکار سفارش‌های حضوری به پیشخوان وردپرس، کلیدهای REST API ووکامرس را وارد نمایید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = wooUrl,
                        onValueChange = { wooUrl = it },
                        label = { Text("آدرس وبسایت فروشگاه (URL)") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("woo_url_input")
                    )

                    OutlinedTextField(
                        value = consumerKey,
                        onValueChange = { consumerKey = it },
                        label = { Text("کلید مصرف‌کننده (Consumer Key)") },
                        placeholder = { Text("ck_xxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("woo_ck_input")
                    )

                    OutlinedTextField(
                        value = consumerSecret,
                        onValueChange = { consumerSecret = it },
                        label = { Text("رمز مصرف‌کننده (Consumer Secret)") },
                        placeholder = { Text("cs_xxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                        singleLine = true,
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(
                                    imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "نمایش رمز"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("woo_cs_input")
                    )

                    // Test Connection Button
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                testStatusMessage = null
                                viewModel.testWooConnection(wooUrl, consumerKey, consumerSecret) { success, msg ->
                                    isTestingConnection = false
                                    testStatusMessage = Pair(success, msg)
                                }
                            },
                            enabled = !isTestingConnection && wooUrl.isNotBlank() && consumerKey.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_woo_connection_btn")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تست ارتباط با سایت", fontSize = 12.sp)
                            }
                        }
                    }

                    if (testStatusMessage != null) {
                        val (success, msg) = testStatusMessage!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (success) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)
                            )
                        ) {
                            Text(
                                text = msg,
                                color = if (success) Color(0xFF065F46) else Color(0xFF9F1239),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Help banner for where to find keys
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("راهنمای ساخت کلید در ووکامرس:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "۱. وارد پیشخوان وردپرس سایت خود شوید.\n" +
                                        "۲. به مسیر: ووکامرس > پیکربندی > پیشرفته > REST API بروید.\n" +
                                        "۳. روی «افزودن کلید» کلیک کرده و دسترسی را روی «خواندن/نوشتن» بگذارید.\n" +
                                        "۴. کلید و رمز نمایش داده شده را در فیلدهای بالا کپی نمایید.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        // Store Profile & Thermal Receipt Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("مشخصات فروشگاه و سربرگ فاکتور", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("نام فروشگاه (در بالای فاکتور چاپ می‌شود)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storePhone,
                        onValueChange = { storePhone = it },
                        label = { Text("تلفن تماس فروشگاه") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = storeAddress,
                        onValueChange = { storeAddress = it },
                        label = { Text("آدرس فروشگاه") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = footerNote,
                        onValueChange = { footerNote = it },
                        label = { Text("متن پاورقی فاکتور و شرایط تعویض") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = taxRateStr,
                        onValueChange = { taxRateStr = it },
                        label = { Text("درصد مالیات بر ارزش افزوده (پیش‌فرض ۱۰٪)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val tax = taxRateStr.toDoubleOrNull() ?: 10.0
                    viewModel.saveWooSettings(
                        url = wooUrl,
                        key = consumerKey,
                        secret = consumerSecret,
                        storeName = storeName,
                        storePhone = storePhone,
                        storeAddress = storeAddress,
                        footerNote = footerNote,
                        taxPercent = tax
                    )
                    showSaveToast = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ذخیره کلیه تنظیمات", fontWeight = FontWeight.Bold)
            }

            if (showSaveToast) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))
                ) {
                    Text(
                        "تنظیمات با موفقیت در دیتابیس ذخیره شد.",
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 200 Features Summary info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("قابلیت‌های فعال در این پایانه فروشگاهی:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "• همگام‌سازی دوطرفه با REST API ووکامرس\n" +
                                "• کسر خودکار موجودی انبار سایت هنگام فروش حضوری\n" +
                                "• محاسبه اتوماتیک بهای تمام شده کالای فروش رفته (COGS)\n" +
                                "• گزارش روزانه سود و زیان (P&L) و سود خالص واقعی\n" +
                                "• صدور گزارش Z و مغایرت‌گیری کسری و مازاد صندوقدار\n" +
                                "• پشتیبانی از پرداخت نقد، کارت‌خوان، چک صیادی و نسیه دفتری\n" +
                                "• ثبت و رهگیری چک‌های صیادی ۱۶ رقمی با تاریخ سررسید شمسی\n" +
                                "• دفتر معین مشتریان و ارسال پیامک یادآوری بدهی\n" +
                                "• صدور فاکتور حرارتی استاندارد ۸۰ میلی‌متری با بارکد\n" +
                                "• کارکرد ۱۰۰٪ آفلاین در زمان قطعی اینترنت و صف همگام‌سازی خودکار",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
