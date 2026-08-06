
package com.nadrlab.budgetuser.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.budgetuser.viewmodel.BudgetViewModel

@Composable
fun StoresTab(viewModel: BudgetViewModel) {
    val storesWithDebt by viewModel.storesWithDebt.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("البقالات", fontSize = 22.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("إضافة", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (storesWithDebt.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Store, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد بقالات", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(storesWithDebt) { item ->
                    StoreCard(
                        storeWithDebt = item,
                        formatAmount = viewModel::formatAmount,
                        onDelete = { viewModel.deleteStore(item.store) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddStoreDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, address ->
                viewModel.addStore(name, phone, address)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StoreCard(
    storeWithDebt: BudgetViewModel.StoreWithDebt,
    formatAmount: (Double) -> String,
    onDelete: () -> Unit
) {
    val debt = storeWithDebt.debt

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (debt > 0) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(storeWithDebt.store.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (storeWithDebt.store.phone.isNotBlank()) {
                            Text(storeWithDebt.store.phone, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "حذف", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFF333333))
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المشتريات", color = Color.Gray, fontSize = 11.sp)
                    Text(formatAmount(storeWithDebt.totalPurchases), color = Color(0xFFF44336), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المدفوعات", color = Color.Gray, fontSize = 11.sp)
                    Text(formatAmount(storeWithDebt.totalPayments), color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (debt > 0) Color(0xFF3A1A1A) else Color(0xFF1A3A1A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (debt > 0) "المديونية" else if (debt < 0) "رصيد لك" else "مسدد",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatAmount(kotlin.math.abs(debt)),
                        color = if (debt > 0) Color(0xFFF44336) else if (debt < 0) Color(0xFF4CAF50) else Color.Gray,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddStoreDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة بقالة", color = Color(0xFF4CAF50)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("اسم البقالة") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("العنوان (اختياري)") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, phone, address) }) {
                Text("إضافة", color = Color(0xFF4CAF50))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        }
    )
}
