package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utils.PersianUtils

@Composable
fun QuickCalculatorDialog(
    onDismiss: () -> Unit,
    onUseResult: ((Double) -> Unit)? = null
) {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableDoubleStateOf(0.0) }
    var currentOp by remember { mutableStateOf<String?>(null) }
    var shouldResetOnNextDigit by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (shouldResetOnNextDigit || display == "0") {
            display = d
            shouldResetOnNextDigit = false
        } else {
            if (display.length < 12) {
                display += d
            }
        }
    }

    fun onOperator(op: String) {
        operand1 = display.toDoubleOrNull() ?: 0.0
        currentOp = op
        shouldResetOnNextDigit = true
    }

    fun onCalculate() {
        val operand2 = display.toDoubleOrNull() ?: 0.0
        val result = when (currentOp) {
            "+" -> operand1 + operand2
            "-" -> operand1 - operand2
            "×" -> operand1 * operand2
            "÷" -> if (operand2 != 0.0) operand1 / operand2 else 0.0
            else -> operand2
        }
        display = if (result % 1.0 == 0.0) result.toLong().toString() else "%.2f".format(result)
        currentOp = null
        shouldResetOnNextDigit = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .testTag("calculator_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ماشین‌حساب صندوقدار",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = PersianUtils.toPersianDigits(display),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val buttonRows = listOf(
                    listOf("C", "÷", "×", "DEL"),
                    listOf("7", "8", "9", "-"),
                    listOf("4", "5", "6", "+"),
                    listOf("1", "2", "3", "="),
                    listOf("0", "00", "000", ".")
                )

                for (row in buttonRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (btn in row) {
                            val isOp = btn in listOf("+", "-", "×", "÷", "=")
                            val isAction = btn in listOf("C", "DEL")

                            Button(
                                onClick = {
                                    when (btn) {
                                        "C" -> {
                                            display = "0"
                                            operand1 = 0.0
                                            currentOp = null
                                            shouldResetOnNextDigit = false
                                        }
                                        "DEL" -> {
                                            if (display.length > 1) {
                                                display = display.dropLast(1)
                                            } else {
                                                display = "0"
                                            }
                                        }
                                        "+", "-", "×", "÷" -> onOperator(btn)
                                        "=" -> onCalculate()
                                        "." -> {
                                            if (!display.contains(".")) {
                                                display += "."
                                            }
                                        }
                                        else -> onDigit(btn)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        btn == "=" -> MaterialTheme.colorScheme.primary
                                        isOp -> MaterialTheme.colorScheme.primaryContainer
                                        isAction -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = when {
                                        btn == "=" -> MaterialTheme.colorScheme.onPrimary
                                        isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isAction -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (btn == "DEL") {
                                    Icon(Icons.Default.Backspace, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        text = if (btn.all { it.isDigit() }) PersianUtils.toPersianDigits(btn) else btn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (onUseResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val value = display.toDoubleOrNull() ?: 0.0
                            onUseResult(value)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("انتقال به صندوق / مبلغ دریافتی", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
