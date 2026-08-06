package com.nadrlab.baitbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.data.model.Transaction
import com.nadrlab.baitbudget.data.model.TransactionType
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTab(
    viewModel: BudgetViewModel,
    isAdmin: Boolean = true,
    onAddPurchase: () -> Unit,
    onAddPayment: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val stores by viewModel.allStores.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    var selectedFilter by remember { mutableStateOf("الكل") }

    val senderTags = remember(allTransactions) {
        allTransactions
            .map { it.senderTag }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val transactions = remember(allTransactions, selectedFilter) {
        if (selectedFilter == "الكل") allTransactions
        else allTransactions.filter { it.senderTag == selectedFilter }
    }

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

        // ═══ فلتر المستخدمين (للمشرف فقط) ═══
        if (isAdmin && senderTags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == "الكل",
                        onClick = { selectedFilter = "الكل" },
                        label = { Text("الكل", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF50),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1A1A1A),
                            labelColor = Color.Gray
                        )
                    )
                }
                items(senderTags) { tag ->
                    FilterChip(
                        selected = selectedFilter == tag,
                        onClick = { selectedFilter = tag },
                        label = { Text(tag, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1A1A1A),
                            labelColor = Color.Gray
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (selectedFilter != "الكل") {
            Text(
                "عرض: $selectedFilter (${transactions.size})",
                color = Color(0xFF2196F3),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
        }

        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (selectedFilter == "الكل") "لا توجد معاملات" else "لا توجد معاملات لـ $selectedFilter",
                        color = Color.Gray, fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(transactions) { transaction ->
                    val storeName = stores.find { it.id == transaction.storeId }?.name ?: "غير معروف"
                    TransactionCard(
                        transaction = transaction,
                        storeName = storeName,
                        dateFormatted = dateFormat.format(Date(transaction.date)),
                        formatAmount = viewModel::formatAmount,
                        onDelete = { viewModel.deleteTransaction(transaction) },
                        isAdmin = isAdmin,
                        showSender = isAdmin && transaction.senderTag.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    storeName: String,
    dateFormatted: String,
    formatAmount: (Double) -> String,
    onDelete: () -> Unit,
    isAdmin: Boolean = true,
    showSender: Boolean = false
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
                if (showSender) {
                    Text("👤 ${transaction.senderTag}", color = Color(0xFF2196F3), fontSize = 11.sp)
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
                if (isAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
