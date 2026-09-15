package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingBag
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreConfigEntity
import com.example.ui.components.QuickCalculatorDialog
import com.example.ui.components.ReceiptDialog
import com.example.utils.PersianUtils
import com.example.viewmodel.CartItem
import com.example.viewmodel.PosUiState
import com.example.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val config by viewModel.storeConfig.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showBarcodeModal by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("همه") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, uiState.searchQuery, uiState.selectedCategory) {
        products.filter { product ->
            val matchQuery = uiState.searchQuery.isBlank() ||
                    product.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    product.sku.contains(uiState.searchQuery, ignoreCase = true) ||
                    product.barcode.contains(uiState.searchQuery)
            val matchCategory = uiState.selectedCategory == "همه" || product.category == uiState.selectedCategory
            matchQuery && matchCategory
        }
    }

    val totalCartCount = uiState.cart.sumOf { it.quantity }
    val totalCartPrice = uiState.cart.sumOf { it.totalPrice }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Quick Bar: Search, Barcode Scanner Gun simulation, Calculator, Custom Item
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("جستجوی نام، بارکد یا کد کالا...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pos_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Barcode Gun Quick Scanner Simulation
                    IconButton(
                        onClick = { showBarcodeModal = true },
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                            .testTag("barcode_scan_btn")
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "اسکن بارکد",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Calculator Button
                    IconButton(
                        onClick = { viewModel.toggleCalculator(true) },
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(10.dp))
                            .testTag("calculator_btn")
                    ) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = "ماشین‌حساب",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    // Custom item button
                    IconButton(
                        onClick = { viewModel.toggleQuickItemDialog(true) },
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                            .testTag("add_custom_item_btn")
                    ) {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = "کالای آزاد",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Categories horizontal scroll
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { category ->
                    val isSelected = uiState.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Product Catalog Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("کالایی یافت نشد", style = MaterialTheme.typography.titleMedium)
                        Text("می‌توانید با دکمه کالای آزاد جنس جدید بفروشید یا از بخش ووکامرس همگام‌سازی کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val cartQty = uiState.cart.find { it.product.id == product.id }?.quantity ?: 0
                        ProductGridCard(
                            product = product,
                            cartQuantity = cartQty,
                            onAddToCart = { viewModel.addToCart(product) },
                            onDecrease = { viewModel.updateCartItemQuantity(product.id, -1) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Cart Bar
        AnimatedVisibility(
            visible = uiState.cart.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCartSheet = true }
                    .testTag("bottom_cart_summary_bar"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(containerColor = MaterialTheme.colorScheme.onPrimary) {
                            Text(
                                text = PersianUtils.toPersianDigits(totalCartCount.toString()),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "سبد فروش حضوری",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = PersianUtils.formatCurrency(totalCartPrice),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "تسویه",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Cart & Checkout
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CartCheckoutSheetContent(
                uiState = uiState,
                config = config ?: StoreConfigEntity(),
                customers = customers,
                onUpdateQty = { id, delta -> viewModel.updateCartItemQuantity(id, delta) },
                onRemoveItem = { id -> viewModel.removeFromCart(id) },
                onClearCart = { viewModel.clearCart() },
                onSelectCustomer = { c -> viewModel.setCustomer(c) },
                onSetPaymentMethod = { m -> viewModel.setPaymentMethod(m) },
                onSetCashReceived = { a -> viewModel.setCashReceived(a) },
                onSetCardDetails = { rrn, term -> viewModel.setCardDetails(rrn, term) },
                onSetChequeDetails = { sayad, bank, date -> viewModel.setChequeDetails(sayad, bank, date) },
                onSetDiscount = { pct, fix -> viewModel.setInvoiceDiscount(pct, fix) },
                onToggleTax = { en -> viewModel.toggleTax(en) },
                onCompleteSale = {
                    viewModel.completeSale()
                    showCartSheet = false
                }
            )
        }
    }

    // Barcode quick scan dialog
    if (showBarcodeModal) {
        BarcodeSimulationDialog(
            products = products,
            onDismiss = { showBarcodeModal = false },
            onProductFound = { p ->
                viewModel.addToCart(p)
                showBarcodeModal = false
            }
        )
    }

    // Calculator Dialog
    if (uiState.showCalculatorDialog) {
        QuickCalculatorDialog(
            onDismiss = { viewModel.toggleCalculator(false) },
            onUseResult = { result ->
                viewModel.setCashReceived(result)
            }
        )
    }

    // Quick custom item dialog
    if (uiState.showQuickItemDialog) {
        QuickCustomItemDialog(
            onDismiss = { viewModel.toggleQuickItemDialog(false) },
            onConfirm = { name, price, cat ->
                viewModel.addCustomItem(name, price, cat)
            }
        )
    }

    // Printable Thermal Receipt Dialog
    if (uiState.showReceiptDialog && uiState.lastCompletedOrder != null) {
        ReceiptDialog(
            order = uiState.lastCompletedOrder!!,
            config = config ?: StoreConfigEntity(),
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
fun ProductGridCard(
    product: ProductEntity,
    cartQuantity: Int,
    onAddToCart: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
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
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                // Stock Quantity Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            if (product.stockQuantity > 5) Color(0xCC059669) else Color(0xCCE11D48),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "موجودی: ${PersianUtils.toPersianDigits(product.stockQuantity.toString())}",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cart badge if added
                if (cartQuantity > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = PersianUtils.toPersianDigits(cartQuantity.toString()),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PersianUtils.formatCurrency(product.activePrice),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick Add Icon Button
                    IconButton(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "افزودن به سبد",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartCheckoutSheetContent(
    uiState: PosUiState,
    config: StoreConfigEntity,
    customers: List<CustomerEntity>,
    onUpdateQty: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onSetPaymentMethod: (String) -> Unit,
    onSetCashReceived: (Double) -> Unit,
    onSetCardDetails: (String, String) -> Unit,
    onSetChequeDetails: (String, String, String) -> Unit,
    onSetDiscount: (Double, Double) -> Unit,
    onToggleTax: (Boolean) -> Unit,
    onCompleteSale: () -> Unit
) {
    val subtotal = uiState.cart.sumOf { it.totalPrice }
    val discount = (subtotal * (uiState.invoiceDiscountPercent / 100.0)) + uiState.invoiceDiscountFixed
    val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
    val tax = if (uiState.applyTax) afterDiscount * (config.taxRatePercent / 100.0) else 0.0
    val totalNet = afterDiscount + tax

    var showDiscountInput by remember { mutableStateOf(false) }
    var discountInputStr by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sheet Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسویه و صدور فاکتور فروش",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearCart) {
                    Text("خالی کردن سبد", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        // Cart Items List
        items(uiState.cart, key = { it.product.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${PersianUtils.formatCurrency(item.unitPrice)} × ${PersianUtils.toPersianDigits(item.quantity.toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onUpdateQty(item.product.id, -1) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "کاهش", modifier = Modifier.size(14.dp))
                        }

                        Text(
                            text = PersianUtils.toPersianDigits(item.quantity.toString()),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = { onUpdateQty(item.product.id, 1) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزایش", modifier = Modifier.size(14.dp))
                        }

                        IconButton(
                            onClick = { onRemoveItem(item.product.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Customer Selection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomerPicker = !showCustomerPicker },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = uiState.selectedCustomer?.name ?: "مشتری حضوری / نقدی",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.selectedCustomer != null) {
                                Text(
                                    text = "مانده حساب: ${PersianUtils.formatCurrency(uiState.selectedCustomer!!.balance)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.selectedCustomer!!.balance > 0) Color(0xFFE11D48) else Color(0xFF059669)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (uiState.selectedCustomer == null) "تغییر مشتری" else "تغییر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showCustomerPicker) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "انتخاب از مشتریان ثبت شده",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCustomer(null)
                                    showCustomerPicker = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text("• مشتری حضوری ناشناس", style = MaterialTheme.typography.bodySmall)
                        }
                        customers.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectCustomer(c)
                                        showCustomerPicker = false
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${c.name} (${PersianUtils.toPersianDigits(c.phone)})", style = MaterialTheme.typography.bodySmall)
                                Text(PersianUtils.formatCurrency(c.balance), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Financial Summary & Discounts / Tax
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جمع اقلام:", style = MaterialTheme.typography.bodySmall)
                        Text(PersianUtils.formatCurrency(subtotal), style = MaterialTheme.typography.bodySmall)
                    }

                    // Discount buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.0, 5.0, 10.0, 15.0).forEach { pct ->
                                val active = uiState.invoiceDiscountPercent == pct
                                SuggestionChip(
                                    onClick = { onSetDiscount(pct, 0.0) },
                                    label = { Text(if (pct == 0.0) "بدون تخفیف" else "${PersianUtils.toPersianDigits(pct.toInt().toString())}٪", fontSize = 10.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }

                    // Tax toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مالیات بر ارزش افزوده (۱۰٪)", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = uiState.applyTax,
                            onCheckedChange = { onToggleTax(it) },
                            modifier = Modifier.height(30.dp)
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مبلغ نهایی قابل پرداخت:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            PersianUtils.formatCurrency(totalNet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Payment Method Selector
        item {
            Text("انتخاب روش پرداخت:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair("CASH", "نقد"),
                    Pair("POS_CARD", "کارت‌خوان"),
                    Pair("CREDIT", "نسیه"),
                    Pair("CHEQUE", "چک صیاد")
                ).forEach { (key, label) ->
                    val selected = uiState.paymentMethod == key
                    Button(
                        onClick = { onSetPaymentMethod(key) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Payment specifics
            when (uiState.paymentMethod) {
                "CASH" -> {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("مبلغ دریافتی نقد:", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(totalNet, 500_000.0, 1_000_000.0, 2_000_000.0).forEach { amt ->
                                if (amt >= totalNet) {
                                    OutlinedButton(
                                        onClick = { onSetCashReceived(amt) },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(PersianUtils.formatCurrency(amt), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        if (uiState.cashReceived >= totalNet && uiState.cashReceived > 0) {
                            val change = uiState.cashReceived - totalNet
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("باقی‌مانده برای عودت به مشتری:", fontWeight = FontWeight.Bold, color = Color(0xFF065F46), fontSize = 12.sp)
                                    Text(PersianUtils.formatCurrency(change), fontWeight = FontWeight.Bold, color = Color(0xFF065F46), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                "POS_CARD" -> {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = uiState.cardRrn,
                            onValueChange = { onSetCardDetails(it, uiState.cardTerminalName) },
                            label = { Text("شماره پیگیری کارت‌خوان (RRN)", fontSize = 11.sp) },
                            placeholder = { Text("مثال: ۱۲۳۴۵۶۷۸") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                "CHEQUE" -> {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = uiState.chequeSayad,
                            onValueChange = { onSetChequeDetails(it, uiState.chequeBank, uiState.chequeDueDate) },
                            label = { Text("شناسه ۱۶ رقمی صیاد", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                "CREDIT" -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                    ) {
                        Text(
                            text = "مبلغ فاکتور به عنوان بدهی در حساب دفتری مشتری '${uiState.selectedCustomer?.name ?: "مشتری حضوری"}' ثبت خواهد شد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Finalize Sale Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onCompleteSale,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("finalize_sale_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت نهایی و صدور فاکتور حرارتی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BarcodeSimulationDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onProductFound: (ProductEntity) -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("شبیه‌ساز اسکنر بارکدخوان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "در فروش حضوری با دستگاه بارکدخوان، بارکد کالا فوری خوانده شده و به سبد اضافه می‌شود. برای تست یکی از کالاهای زیر را لمس کنید یا بارکد وارد کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text("کد بارکد یا SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val match = products.find { it.barcode == manualBarcode || it.sku == manualBarcode }
                        if (match != null) {
                            onProductFound(match)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualBarcode.isNotBlank()
                ) {
                    Text("جستجو و افزودن به سبد")
                }

                HorizontalDivider()
                Text("بارکدهای تستی کالاهای موجود:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(products.take(6)) { prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onProductFound(prod) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prod.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.weight(1f))
                                Badge {
                                    Text(PersianUtils.toPersianDigits(prod.barcode.ifBlank { prod.sku }))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

@Composable
fun QuickCustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("خدمات و بسته‌بندی") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن کالای آزاد / خدمات به سبد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("عنوان جنس یا خدمات") },
                    placeholder = { Text("مثال: کارتن بسته‌بندی، هزینه پیک...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("قیمت فروش (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && p > 0) {
                        onConfirm(name, p, category)
                    }
                },
                enabled = name.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("افزودن به سبد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
