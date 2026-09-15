package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.PersianUtils
import com.example.viewmodel.PosTab
import com.example.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // RTL Support for complete Persian Persian/Farsi Experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    PosMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosMainApp(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val config by viewModel.storeConfig.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = config?.storeName ?: "صندوق فروشگاهی ووکامرس",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "امروز: ${PersianUtils.toPersianDigits(PersianUtils.getCurrentPersianDate())} | متصل به وردپرس",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                actions = {
                    // WooCommerce connection badge
                    val isConnected = config?.isWooConnected == true
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isConnected) Color(0xFFD1FAE5) else Color(0xFFF1F5F9))
                            .clickable { viewModel.syncWithWooCommerce() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isConnected) Color(0xFF059669) else Color(0xFF94A3B8),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (isConnected) "سایت آنلاین" else "آفلاین",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFF065F46) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Quick Calculator
                    IconButton(
                        onClick = { viewModel.toggleCalculator(true) },
                        modifier = Modifier.testTag("appbar_calculator_btn")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "ماشین‌حساب")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(PosTab.POS, "فروش حضوری", Icons.Default.PointOfSale),
                    Triple(PosTab.INVENTORY, "انبار و سایت", Icons.Default.Inventory2),
                    Triple(PosTab.ACCOUNTING, "حسابداری", Icons.Default.Analytics),
                    Triple(PosTab.CUSTOMERS, "مشتریان و چک", Icons.Default.AccountBalanceWallet),
                    Triple(PosTab.SETTINGS, "تنظیمات", Icons.Default.Settings)
                )

                navItems.forEach { (tab, label, icon) ->
                    val isSelected = uiState.activeTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                PosTab.POS -> PosScreen(viewModel = viewModel)
                PosTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                PosTab.ACCOUNTING -> AccountingScreen(viewModel = viewModel)
                PosTab.CUSTOMERS -> CustomersAndChequesScreen(viewModel = viewModel)
                PosTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
