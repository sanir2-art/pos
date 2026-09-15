package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.StoreConfigEntity
import com.example.ui.theme.ReceiptPaper
import com.example.utils.PersianUtils

@Composable
fun ReceiptDialog(
    order: OrderEntity,
    config: StoreConfigEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val receiptText = buildString {
        appendLine("============== فاکتور فروش حضوری ==============")
        appendLine(config.storeName)
        appendLine("تلفن: ${config.storePhone}")
        appendLine("آدرس: ${config.storeAddress}")
        appendLine("------------------------------------------------")
        appendLine("شماره فاکتور: ${order.invoiceNumber}")
        appendLine("تاریخ: ${PersianUtils.formatToPersianDateTime(order.timestamp)}")
        appendLine("مشتری: ${order.customerName}")
        appendLine("------------------------------------------------")
        order.itemsJson.split(";").forEach { item ->
            if (item.isNotBlank()) appendLine(item)
        }
        appendLine("------------------------------------------------")
        appendLine("جمع اقلام: ${PersianUtils.formatCurrency(order.subtotal, config.currency)}")
        if (order.discountAmount > 0) {
            appendLine("تخفیف: ${PersianUtils.formatCurrency(order.discountAmount, config.currency)}")
        }
        if (order.taxAmount > 0) {
            appendLine("مالیات بر ارزش افزوده: ${PersianUtils.formatCurrency(order.taxAmount, config.currency)}")
        }
        appendLine("مبلغ نهایی قابل پرداخت: ${PersianUtils.formatCurrency(order.netAmount, config.currency)}")
        appendLine("روش پرداخت: ${when(order.paymentMethod) {
            "CASH" -> "نقد"
            "POS_CARD" -> "کارت‌خوان پوز (کد پیگیری: ${order.cardRrn ?: "ثبت شد"})"
            "CREDIT" -> "حساب دفتری / نسیه"
            "CHEQUE" -> "چک صیادی"
            "SPLIT" -> "ترکیبی"
            else -> order.paymentMethod
        }}")
        appendLine("------------------------------------------------")
        appendLine(config.invoiceFooterNote)
        appendLine("================================================")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("receipt_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "رسید چاپی فاکتور فروش",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_receipt_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Paper Receipt
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ReceiptPaper)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = config.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "صندوق مکانیزه فروش حضوری متصل به ووکامرس",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تلفن: ${PersianUtils.toPersianDigits(config.storePhone)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice metadata
                        ReceiptRow(label = "شماره فاکتور:", value = PersianUtils.toPersianDigits(order.invoiceNumber))
                        ReceiptRow(label = "تاریخ و زمان:", value = PersianUtils.formatToPersianDateTime(order.timestamp))
                        ReceiptRow(label = "مشتری گرامی:", value = order.customerName)
                        ReceiptRow(label = "صندوقدار:", value = config.defaultCashier)

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items list
                        Text(
                            text = "اقلام خریداری شده",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val items = order.itemsJson.split(";").filter { it.isNotBlank() }
                        for (item in items) {
                            Text(
                                text = "• ${PersianUtils.toPersianDigits(item)} تومان",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Financial totals
                        ReceiptRow(label = "جمع کل اقلام:", value = PersianUtils.formatCurrency(order.subtotal, config.currency))
                        if (order.discountAmount > 0) {
                            ReceiptRow(
                                label = "تخفیف ویژه:",
                                value = "- ${PersianUtils.formatCurrency(order.discountAmount, config.currency)}",
                                valueColor = Color(0xFFDC2626)
                            )
                        }
                        if (order.taxAmount > 0) {
                            ReceiptRow(
                                label = "مالیات بر ارزش افزوده (۱۰٪):",
                                value = PersianUtils.formatCurrency(order.taxAmount, config.currency)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            ReceiptRow(
                                label = "مبلغ نهایی پرداخت:",
                                value = PersianUtils.formatCurrency(order.netAmount, config.currency),
                                isBold = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payment info
                        val paymentLabel = when (order.paymentMethod) {
                            "CASH" -> "نقدی (تسویه شد)"
                            "POS_CARD" -> "کارت‌خوان POS"
                            "CREDIT" -> "نسیه / حساب دفتری"
                            "CHEQUE" -> "چک صیادی"
                            "SPLIT" -> "پرداخت ترکیبی"
                            else -> order.paymentMethod
                        }
                        ReceiptRow(label = "روش پرداخت:", value = paymentLabel)
                        if (!order.cardRrn.isNullOrBlank()) {
                            ReceiptRow(label = "شماره پیگیری پوز:", value = PersianUtils.toPersianDigits(order.cardRrn))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulated Barcode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFCBD5E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "||| | |||| || | ||| |||| | ||| || |||",
                                letterSpacing = 4.sp,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        }
                        Text(
                            text = PersianUtils.toPersianDigits(order.invoiceNumber),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = config.invoiceFooterNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Share and Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "ارسال فاکتور")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اشتراک پیامک")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تایید و چاپ")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color(0xFF1E293B)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            color = Color(0xFF475569),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
fun DashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        color = Color(0xFFCBD5E1),
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
