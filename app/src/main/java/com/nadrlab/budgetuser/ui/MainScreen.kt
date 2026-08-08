package com.nadrlab.budgetuser.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.budgetuser.data.model.TransactionType
import com.nadrlab.budgetuser.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: BudgetViewModel) {
    val stores by viewModel.allStores.collectAsState()
    val storesWithDebt by viewModel.storesWithDebt.collectAsState()
    val lastTransaction by viewModel.lastTransaction.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddPurchase by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var showChangeName by remember { mutableStateOf(false) }
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A1A), contentColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("الرئيسية", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1A3A1A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Receipt, null) },
                    label = { Text("المعاملات", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1A3A1A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text("البقالات", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1A3A1A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Assessment, null) },
                    label = { Text("التقارير", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE8C547), selectedTextColor = Color(0xFFE8C547),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF2A2A1A)
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeTab(
                    viewModel = viewModel,
                    lastTransaction = lastTransaction,
                    userName = userName,
                    onAddPurchase = { showAddPurchase = true },
                    onAddPayment = { showAddPayment = true },
                    onChangeName = { showChangeName = true }
                )
                1 -> TransactionsTab(
                    viewModel = viewModel,
                    onAddPurchase = { showAddPurchase = true },
                    onAddPayment = { showAddPayment = true }
                )
                2 -> StoresTab(viewModel = viewModel)
                3 -> ReportsTab(
                    viewModel = viewModel,
                    storesWithDebt = storesWithDebt
                )
            }
        }
    }

    if (showAddPurchase) {
        AddTransactionDialog(
            title = "تسجيل شراء", titleColor = Color(0xFFF44336), stores = stores,
            onDismiss = { showAddPurchase = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPurchase(storeId, amount, desc, note)
                showAddPurchase = false
            }
        )
    }

    if (showAddPayment) {
        AddTransactionDialog(
            title = "تسجيل دفع", titleColor = Color(0xFF4CAF50), stores = stores,
            onDismiss = { showAddPayment = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPayment(storeId, amount, desc, note)
                showAddPayment = false
            }
        )
    }

    if (showChangeName) {
        ChangeNameDialog(
            currentName = userName,
            onDismiss = { showChangeName = false },
            onConfirm = { newName -> viewModel.changeUserName(newName); showChangeName = false }
        )
    }
}

// ═══════════════════════════════════════════
// الرئيسية — بسيطة ونظيفة
// ═══════════════════════════════════════════
@Composable
fun HomeTab(
    viewModel: BudgetViewModel,
    lastTransaction: BudgetViewModel.LastTransactionInfo?,
    userName: String,
    onAddPurchase: () -> Unit,
    onAddPayment: () -> Unit,
    onChangeName: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale("ar")) }

    // ═══ تنبيه التكرارات ═══
    val duplicateWarnings by viewModel.duplicateWarnings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ═══ العنوان ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("مشتريات المستخدم", fontSize = 24.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(userName, color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("  •  ", color = Color.Gray, fontSize = 12.sp)
                    Text(SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date()), color = Color.Gray, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onChangeName) {
                Icon(Icons.Default.Edit, "تغيير الاسم", tint = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══ تنبيه التكرارات ═══
        if (duplicateWarnings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "⚠️ تنبيه: ${duplicateWarnings.size} عملية مكررة مشبوهة",
                            color = Color(0xFFFF9800), fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "انتقل إلى التقارير للمراجعة",
                            color = Color(0xFF886644), fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        // ═══ آخر عملية ═══
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastTransaction != null) {
                    val isPurchase = lastTransaction.type == TransactionType.PURCHASE
                    Column(modifier = Modifier.weight(1f)) {
                        Text("آخر عملية", color = Color(0xFFE8C547), fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPurchase) Color(0xFFF44336).copy(alpha = 0.2f)
                                    else Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    if (isPurchase) "  شراء  " else "  دفع  ",
                                    color = if (isPurchase) Color(0xFFF44336) else Color(0xFF4CAF50),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(lastTransaction.storeName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        if (lastTransaction.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(lastTransaction.description, color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(dateFormat.format(Date(lastTransaction.date)), color = Color(0xFF666666), fontSize = 12.sp)
                        }
                    }
                    Text(
                        viewModel.formatAmount(lastTransaction.amount),
                        color = if (isPurchase) Color(0xFFF44336) else Color(0xFF4CAF50),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Receipt, null, tint = Color(0xFF444444), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد معاملات بعد", color = Color.Gray, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("ابدأ بتسجيل أول شراء أدناه", color = Color(0xFF555555), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ═══ أزرار ═══
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onAddPurchase, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("شراء", color = Color.White, fontSize = 16.sp)
            }
            Button(
                onClick = onAddPayment, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.Payment, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("دفع", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══ تصدير ═══
        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    try {
                        val msg = viewModel.exportDataForSharing()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, msg)
                        }
                        context.startActivity(Intent.createChooser(intent, "إرسال عبر"))
                    } catch (_: Exception) {}
                    isExporting = false
                }
            },
            modifier = Modifier.fillMaxWidth(), enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Share, null, tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("📤 إرسال عبر الواتساب", color = Color.White, fontSize = 15.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════
// التقارير — إحصائيات + تكرارات + تقرير + بقالات
// ═══════════════════════════════════════════
@Composable
fun ReportsTab(
    viewModel: BudgetViewModel,
    storesWithDebt: List<BudgetViewModel.StoreWithDebt>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val purchaseCount by viewModel.purchaseCount.collectAsState()
    val paymentCount by viewModel.paymentCount.collectAsState()
    val allTimePurchases by viewModel.allTimePurchases.collectAsState()
    val allTimePayments by viewModel.allTimePayments.collectAsState()
    val todayTransactions by viewModel.todayTransactions.collectAsState()
    val duplicateWarnings by viewModel.duplicateWarnings.collectAsState()
    var showDetailedReport by remember { mutableStateOf(false) }
    var isSendingReport by remember { mutableStateOf(false) }

    val totalDebt = allTimePurchases - allTimePayments

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("التقارير", fontSize = 22.sp, color = Color(0xFFE8C547), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // ═══ إحصائيات ═══
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, null, tint = Color(0xFFE8C547), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إحصائيات العمليات", color = Color(0xFFE8C547), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(Icons.Default.Receipt, "العمليات", "$transactionCount", Color(0xFF2196F3))
                    StatItem(Icons.Default.ShoppingCart, "مشتريات", "$purchaseCount", Color(0xFFF44336))
                    StatItem(Icons.Default.Payment, "مدفوعات", "$paymentCount", Color(0xFF4CAF50))
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF2A2A3E))
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(Icons.Default.TrendingUp, "إجمالي المشتريات", viewModel.formatAmount(allTimePurchases), Color(0xFFF44336))
                    StatItem(Icons.Default.TrendingDown, "إجمالي المدفوعات", viewModel.formatAmount(allTimePayments), Color(0xFF4CAF50))
                }
                if (todayTransactions.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF2A2A3E))
                    Spacer(Modifier.height(10.dp))
                    Text("📌 ${todayTransactions.size} عملية اليوم", color = Color(0xFF888888), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ═══ المديونية ═══
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (totalDebt > 0) Color(0xFF2A1A1A) else if (totalDebt < 0) Color(0xFF1A2A1A) else Color(0xFF1A1A2E)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (totalDebt > 0) "المديونية الكلية" else if (totalDebt < 0) "رصيد لك" else "كل شيء مسدد",
                        color = Color.White, fontSize = 14.sp
                    )
                    Text("${transactionCount} عملية مسجلة", color = Color(0xFF666666), fontSize = 11.sp)
                }
                Text(
                    viewModel.formatAmount(kotlin.math.abs(totalDebt)),
                    color = if (totalDebt > 0) Color(0xFFF44336) else if (totalDebt < 0) Color(0xFF4CAF50) else Color.Gray,
                    fontSize = 26.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ═══ تنبيه التكرارات ═══
        if (duplicateWarnings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⚠️ ${duplicateWarnings.size} عملية مكررة مشبوهة",
                            color = Color(0xFFFF9800), fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    for (dup in duplicateWarnings) {
                        val isPurchase = dup.type == TransactionType.PURCHASE
                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A10)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${dup.storeName} - ${if (isPurchase) "شراء" else "دفع"}",
                                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        viewModel.formatAmount(dup.amount),
                                        color = if (isPurchase) Color(0xFFF44336) else Color(0xFF4CAF50),
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "1️⃣ ${dateFormat.format(Date(dup.transaction1.date))}",
                                    color = Color(0xFF888888), fontSize = 10.sp
                                )
                                Text(
                                    "2️⃣ ${dateFormat.format(Date(dup.transaction2.date))}",
                                    color = Color(0xFF888888), fontSize = 10.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "الفارق: ${dup.timeDiff / 1000 / 60} دقيقة",
                                    color = Color(0xFFFF9800), fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ═══ أزرار التقارير ═══
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showDetailedReport = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE8C547)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Assessment, null, tint = Color(0xFFE8C547), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("📋 تقرير مفصل", color = Color(0xFFE8C547), fontSize = 13.sp)
            }

            Button(
                onClick = {
                    isSendingReport = true
                    scope.launch {
                        try {
                            val report = viewModel.generateReportForAdmin()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, "إرسال التقرير للمشرف"))
                        } catch (_: Exception) {}
                        isSendingReport = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSendingReport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isSendingReport) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("📤 إرسال للمشرف", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ═══ حسابات البقالات ═══
        Text("حسابات البقالات", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (storesWithDebt.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Store, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد بقالات مسجلة", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            for (item in storesWithDebt) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.debt > 0) Color(0xFF2A1A1A)
                        else if (item.debt < 0) Color(0xFF1A2A1A)
                        else Color(0xFF1A1A2E)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(item.store.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "شراء: ${viewModel.formatAmount(item.totalPurchases)} • دفع: ${viewModel.formatAmount(item.totalPayments)}",
                                    color = Color(0xFF666666), fontSize = 10.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (item.debt > 0) "عليك" else if (item.debt < 0) "لك" else "مسدد",
                                color = Color(0xFF888888), fontSize = 10.sp
                            )
                            Text(
                                viewModel.formatAmount(kotlin.math.abs(item.debt)),
                                color = if (item.debt > 0) Color(0xFFF44336)
                                else if (item.debt < 0) Color(0xFF4CAF50)
                                else Color.Gray,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showDetailedReport) {
        DetailedReportDialog(viewModel = viewModel, onDismiss = { showDetailedReport = false })
    }
}

// ═══════════════════════════════════════════
// عنصر إحصائي
// ═══════════════════════════════════════════
@Composable
fun StatItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ═══════════════════════════════════════════
// حوار التقرير المفصل
// ═══════════════════════════════════════════
@Composable
fun DetailedReportDialog(viewModel: BudgetViewModel, onDismiss: () -> Unit) {
    val reportItems by viewModel.detailedReportItems.collectAsState()
    val allTimePurchases by viewModel.allTimePurchases.collectAsState()
    val allTimePayments by viewModel.allTimePayments.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, null, tint = Color(0xFFE8C547))
                Spacer(Modifier.width(8.dp))
                Text("تقرير العمليات", color = Color(0xFFE8C547), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (reportItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ReceiptLong, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد عمليات مسجلة", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A2A3E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("التاريخ", color = Color(0xFFE8C547), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("البقالة", color = Color(0xFFE8C547), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("الوصف", color = Color(0xFFE8C547), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("المبلغ", color = Color(0xFFE8C547), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    items(reportItems) { item ->
                        val isPurchase = item.type == TransactionType.PURCHASE
                        val bgColor = if (reportItems.indexOf(item) % 2 == 0) Color(0xFF151520) else Color(0xFF1A1A2E)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(viewModel.formatDate(item.date), color = Color(0xFFAAAAAA), fontSize = 10.sp, modifier = Modifier.weight(1.5f), maxLines = 1)
                            Text(item.storeName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (item.description.isNotBlank()) item.description else "-", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                viewModel.formatAmount(item.amount),
                                color = if (isPurchase) Color(0xFFF44336) else Color(0xFF4CAF50),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF333344))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المشتريات:", color = Color(0xFFF44336), fontSize = 12.sp)
                            Text(viewModel.formatAmount(allTimePurchases), color = Color(0xFFF44336), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المدفوعات:", color = Color(0xFF4CAF50), fontSize = 12.sp)
                            Text(viewModel.formatAmount(allTimePayments), color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFF333344))
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المديونية:", color = Color(0xFFE8C547), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                viewModel.formatAmount(kotlin.math.abs(allTimePurchases - allTimePayments)),
                                color = if (allTimePurchases > allTimePayments) Color(0xFFF44336) else Color(0xFF4CAF50),
                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("عدد العمليات: ${reportItems.size}", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", color = Color(0xFFE8C547)) }
        }
    )
}

// ═══════════════════════════════════════════
// حوار تغيير الاسم
// ═══════════════════════════════════════════
@Composable
fun ChangeNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير الاسم", color = Color(0xFF4CAF50)) },
        text = {
            OutlinedTextField(
                value = newName, onValueChange = { newName = it },
                label = { Text("الاسم الجديد") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (newName.isNotBlank()) onConfirm(newName.trim()) }) {
                Text("حفظ", color = Color(0xFF4CAF50))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        }
    )
}
