package com.nadrlab.budgetuser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.budgetuser.data.model.Transaction
import com.nadrlab.budgetuser.data.model.TransactionType
import com.nadrlab.budgetuser.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsTab(
    viewModel: BudgetViewModel,
    onAddPurchase: () -> Unit,
    onAddPayment: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val stores by viewModel.allStores.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    var filterType by remember { mutableIntStateOf(0) }
    var filterStoreId by remember { mutableStateOf<Long?>(null) }
    var showStoreFilter by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, filterType, filterStoreId) {
        transactions.filter { tx ->
            val typeMatch = when (filterType) {
                1 -> tx.type == TransactionType.PURCHASE
                2 -> tx.type == TransactionType.PAYMENT
                else -> true
            }
            val storeMatch = filterStoreId == null || tx.storeId == filterStoreId
            typeMatch && storeMatch
        }.sortedByDescending { it.date }
    }

    val filteredPurchases = filteredTransactions
        .filter { it.type == TransactionType.PURCHASE }
        .sumOf { it.amount }
    val filteredPayments = filteredTransactions
        .filter { it.type == TransactionType.PAYMENT }
        .sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp)
    ) {
        Text("المعاملات", fontSize = 22.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddPurchase, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("شراء", color = Color.White)
            }
            Button(
                onClick = onAddPayment, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Payment, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("دفع", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChipCustom("الكل", filterType == 0) { filterType = 0 }
            FilterChipCustom("شراء", filterType == 1, Color(0xFFF44336)) { filterType = 1 }
            FilterChipCustom("دفع", filterType == 2, Color(0xFF4CAF50)) { filterType = 2 }
            Spacer(Modifier.weight(1f))
            FilterChipCustom(
                if (filterStoreId != null) stores.find { it.id == filterStoreId }?.name ?: "بقالة" else "بقالة",
                filterStoreId != null,
                Color(0xFF2196F3)
            ) { showStoreFilter = true }
        }

        Spacer(Modifier.height(10.dp))

        if (filteredTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("العمليات", "${filteredTransactions.size}", Color(0xFF2196F3))
                    MiniStat("مشتريات", viewModel.formatAmount(filteredPurchases), Color(0xFFF44336))
                    MiniStat("مدفوعات", viewModel.formatAmount(filteredPayments), Color(0xFF4CAF50))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (filteredTransactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (transactions.isEmpty()) "لا توجد معاملات" else "لا توجد نتائج لهذا الفلتر",
                        color = Color.Gray, fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    val storeName = stores.find { it.id == transaction.storeId }?.name ?: "غير معروف"
                    TransactionCard(
                        transaction = transaction,
                        storeName = storeName,
                        dateFormatted = dateFormat.format(Date(transaction.date)),
                        formatAmount = viewModel::formatAmount,
                        onDelete = { viewModel.deleteTransaction(transaction) }
                    )
                }
            }
        }
    }

    if (showStoreFilter) {
        AlertDialog(
            onDismissRequest = { showStoreFilter = false },
            title = { Text("اختر البقالة", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { filterStoreId = null; showStoreFilter = false },
                        colors = CardDefaults.cardColors(
                            containerColor = if (filterStoreId == null) Color(0xFF1A2A3A) else Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "الكل",
                            color = if (filterStoreId == null) Color(0xFF2196F3) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (filterStoreId == null) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    for (store in stores) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { filterStoreId = store.id; showStoreFilter = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (filterStoreId == store.id) Color(0xFF1A2A3A) else Color(0xFF1A1A1A)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                store.name,
                                color = if (filterStoreId == store.id) Color(0xFF2196F3) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (filterStoreId == store.id) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStoreFilter = false }) {
                    Text("إغلاق", color = Color(0xFF2196F3))
                }
            }
        )
    }
}

@Composable
fun FilterChipCustom(label: String, selected: Boolean, accentColor: Color = Color(0xFFE8C547), onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accentColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            color = if (selected) accentColor else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    storeName: String,
    dateFormatted: String,
    formatAmount: (Double) -> String,
    onDelete: () -> Unit
) {
    val isPurchase = transaction.type == TransactionType.PURCHASE

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPurchase) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isPurchase) "شراء" else "دفع",
                        color = if (isPurchase) Color(0xFFF44336) else Color(0xFF4CAF50),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(storeName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (transaction.description.isNotBlank()) {
                    Text(transaction.description, color = Color.Gray, fontSize = 12.sp)
                }
                Text(dateFormatted, color = Color(0xFF666666), fontSize = 11.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatAmount(transaction.amount),
                    color = if (isPurchase) Color(0xFFF44336) else Color(0xFF4CAF50),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
