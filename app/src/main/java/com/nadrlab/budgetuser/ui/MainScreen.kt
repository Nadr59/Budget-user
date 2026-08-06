
package com.nadrlab.budgetuser.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.budgetuser.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: BudgetViewModel) {
    val stores by viewModel.allStores.collectAsState()
    val storesWithDebt by viewModel.storesWithDebt.collectAsState()
    val monthPurchases by viewModel.monthPurchases.collectAsState()
    val monthPayments by viewModel.monthPayments.collectAsState()
    val weekPurchases by viewModel.weekPurchases.collectAsState()
    val weekPayments by viewModel.weekPayments.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddPurchase by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var showChangeName by remember { mutableStateOf(false) }
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val totalDebt = storesWithDebt.fold(0.0) { acc, item -> acc + item.debt }

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
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeTab(
                    viewModel = viewModel,
                    storesWithDebt = storesWithDebt,
                    totalDebt = totalDebt,
                    weekPurchases = weekPurchases, weekPayments = weekPayments,
                    monthPurchases = monthPurchases, monthPayments = monthPayments,
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
            }
        }
    }

    if (showAddPurchase) {
        AddTransactionDialog(
            title = "تسجيل شراء",
            titleColor = Color(0xFFF44336),
            stores = stores,
            onDismiss = { showAddPurchase = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPurchase(storeId, amount, desc, note)
                showAddPurchase = false
            }
        )
    }

    if (showAddPayment) {
        AddTransactionDialog(
            title = "تسجيل دفع",
            titleColor = Color(0xFF4CAF50),
            stores = stores,
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
            onConfirm = { newName ->
                viewModel.changeUserName(newName)
                showChangeName = false
            }
        )
    }
}

@Composable
fun HomeTab(
    viewModel: BudgetViewModel,
    storesWithDebt: List<BudgetViewModel.StoreWithDebt>,
    totalDebt: Double,
    weekPurchases: Double, weekPayments: Double,
    monthPurchases: Double, monthPayments: Double,
    userName: String,
    onAddPurchase: () -> Unit, onAddPayment: () -> Unit,
    onChangeName: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ميزانية البيت", fontSize = 26.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (totalDebt > 0) Color(0xFF3A1A1A) else Color(0xFF1A3A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (totalDebt > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                    null,
                    tint = if (totalDebt > 0) Color(0xFFF44336) else Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (totalDebt > 0) "المديونية الكلية" else if (totalDebt < 0) "رصيد لك" else "كل الحسابات مسدّدة",
                    color = Color.White, fontSize = 14.sp
                )
                Text(
                    viewModel.formatAmount(kotlin.math.abs(totalDebt)),
                    color = if (totalDebt > 0) Color(0xFFF44336) else if (totalDebt < 0) Color(0xFF4CAF50) else Color.Gray,
                    fontSize = 36.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddPurchase, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("شراء", color = Color.White, fontSize = 15.sp)
            }
            Button(
                onClick = onAddPayment, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Payment, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("دفع", color = Color.White, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                isExporting = true
                scope.launch {
                    try {
                        val msg = viewModel.exportDataForSharing()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, msg)
                        }
                        context.startActivity(Intent.createChooser(intent, "إرسال عبر"))
                    } catch (_: Exception) {}
                    isExporting = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isExporting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (isExporting) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Share, null, tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("📤 تصدير وإرسال عبر الواتساب", color = Color.White, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        Text("هذا الأسبوع", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "مشتريات", viewModel.formatAmount(weekPurchases), Color(0xFFF44336))
            SummaryCard(Modifier.weight(1f), Icons.Default.Payment, "مدفوعات", viewModel.formatAmount(weekPayments), Color(0xFF4CAF50))
            SummaryCard(Modifier.weight(1f), Icons.Default.TrendingDown, "الصافي", viewModel.formatAmount(weekPurchases - weekPayments), if (weekPurchases - weekPayments > 0) Color(0xFFFF9800) else Color(0xFF4CAF50))
        }

        Spacer(Modifier.height(16.dp))

        Text("هذا الشهر", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "مشتريات", viewModel.formatAmount(monthPurchases), Color(0xFFF44336))
            SummaryCard(Modifier.weight(1f), Icons.Default.Payment, "مدفوعات", viewModel.formatAmount(monthPayments), Color(0xFF4CAF50))
            SummaryCard(Modifier.weight(1f), Icons.Default.TrendingDown, "الصافي", viewModel.formatAmount(monthPurchases - monthPayments), if (monthPurchases - monthPayments > 0) Color(0xFFFF9800) else Color(0xFF4CAF50))
        }

        Spacer(Modifier.height(20.dp))

        if (storesWithDebt.any { it.debt != 0.0 }) {
            Text("حسابات البقالات", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            for (item in storesWithDebt.filter { it.debt != 0.0 }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.debt > 0) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
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
                            Text(item.store.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            viewModel.formatAmount(kotlin.math.abs(item.debt)),
                            color = if (item.debt > 0) Color(0xFFF44336) else Color(0xFF4CAF50),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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
                    Icon(Icons.Default.Store, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("ابدأ بإضافة بقالة", color = Color.Gray, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ChangeNameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير الاسم", color = Color(0xFF4CAF50)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("الاسم الجديد") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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

@Composable
fun SummaryCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, color = Color.Gray, fontSize = 10.sp)
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
