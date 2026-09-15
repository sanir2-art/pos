package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.ProductEntity
import com.example.utils.PersianUtils
import com.example.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val config by viewModel.storeConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LOW_STOCK, OUT_OF_STOCK
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val filteredList = remember(products, searchQuery, selectedFilter) {
        products.filter { p ->
            val matchSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "LOW_STOCK" -> p.stockQuantity in 1..5
                "OUT_OF_STOCK" -> p.stockQuantity <= 0
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    val totalInventoryValue = products.sumOf { it.activePrice * it.stockQuantity }
    val totalInventoryCost = products.sumOf { it.purchasePrice * it.stockQuantity }
    val potentialProfit = totalInventoryValue - totalInventoryCost

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // WooCommerce Sync Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (config?.isWooConnected == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp)
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
                        Icon(
                            imageVector = if (config?.isWooConnected == true) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (config?.isWooConnected == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (config?.isWooConnected == true) "متصل به فروشگاه ووکامرس" else "حالت آفلاین (بدون اتصال به سایت)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (config?.lastSyncTime ?: 0L > 0)
                            "آخرین به‌روزرسانی: ${PersianUtils.formatToPersianDateTime(config!!.lastSyncTime)}"
                        else "تعداد کل کالاهای انبار: ${PersianUtils.toPersianDigits(products.size.toString())} قلم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = { viewModel.syncWithWooCommerce() },
                    enabled = !uiState.isSyncingWoo,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("sync_woo_btn")
                ) {
                    if (uiState.isSyncingWoo) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("همگام‌سازی", fontSize = 12.sp)
                    }
                }
            }
        }

        // Sync Message Feedback
        if (!uiState.syncMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = uiState.syncMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Inventory Valuation Summary Card (ارزش ریالی انبار)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ارزش فروش انبار", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(PersianUtils.formatCurrency(totalInventoryValue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("بهای تمام شده خرید", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(PersianUtils.formatCurrency(totalInventoryCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("سود پیش‌بینی انبار", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(PersianUtils.formatCurrency(potentialProfit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                }
            }
        }

        // Search and Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجوی نام یا کد انبار...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = { showAddProductDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("add_product_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("کالای جدید", fontSize = 12.sp)
            }
        }

        // Filter chips (همه، موجودی کم، ناموجود)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Pair("ALL", "همه کالاها (${PersianUtils.toPersianDigits(products.size.toString())})"),
                Pair("LOW_STOCK", "موجودی کم"),
                Pair("OUT_OF_STOCK", "ناموجود")
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        // Products List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { product ->
                InventoryItemCard(
                    product = product,
                    onEdit = { editingProduct = product }
                )
            }
        }
    }

    // Quick Edit Product Dialog
    if (editingProduct != null) {
        QuickEditProductDialog(
            product = editingProduct!!,
            onDismiss = { editingProduct = null },
            onSave = { newStock, newPrice ->
                viewModel.updateProductStockAndPrice(editingProduct!!.id, newStock, newPrice)
                editingProduct = null
            }
        )
    }

    // Add New Product Dialog
    if (showAddProductDialog) {
        AddNewProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { name, sku, barcode, price, cost, stock, cat ->
                viewModel.addNewProduct(name, sku, barcode, price, cost, stock, cat)
                showAddProductDialog = false
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    product: ProductEntity,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("inventory_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "کد انبار: ${PersianUtils.toPersianDigits(product.sku)} | بارکد: ${PersianUtils.toPersianDigits(product.barcode)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "فروش: ${PersianUtils.formatCurrency(product.activePrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "خرید: ${PersianUtils.formatCurrency(product.purchasePrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Stock & Edit Action
            Column(horizontalAlignment = Alignment.End) {
                Badge(
                    containerColor = if (product.stockQuantity > 5) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)
                ) {
                    Text(
                        text = "موجودی: ${PersianUtils.toPersianDigits(product.stockQuantity.toString())}",
                        color = if (product.stockQuantity > 5) Color(0xFF065F46) else Color(0xFF9F1239),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش سریع", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuickEditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (Int, Double) -> Unit
) {
    var stockStr by remember { mutableStateOf(product.stockQuantity.toString()) }
    var priceStr by remember { mutableStateOf(product.activePrice.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش سریع موجودی و قیمت فروش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("تعداد موجودی در انبار") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("قیمت فروش حضوری و سایت (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = "با ذخیره، موجودی در دیتابیس صندوق ثبت شده و در صورت اتصال ووکامرس، مستقیماً به سایت نیز ارسال می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = stockStr.toIntOrNull() ?: product.stockQuantity
                    val p = priceStr.toDoubleOrNull() ?: product.activePrice
                    onSave(s, p)
                }
            ) {
                Text("ذخیره و همگام‌سازی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AddNewProductDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("10") }
    var category by remember { mutableStateOf("عمومی") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعریف کالای جدید در انبار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام محصول *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("قیمت فروش (تومان) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("بهای تمام شده خرید (تومان) - جهت محاسبه سود") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("موجودی اولیه") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("دسته‌بندی") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("کد SKU") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("بارکد کالا") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: (price * 0.7)
                    val stock = stockStr.toIntOrNull() ?: 0
                    if (name.isNotBlank() && price > 0) {
                        onSave(name, sku, barcode, price, cost, stock, category)
                    }
                },
                enabled = name.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("ثبت محصول")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
