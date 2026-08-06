package com.nadrlab.baitbudget.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsTab(viewModel: BudgetViewModel) {
    val allTimePurchases by viewModel.allTimePurchases.collectAsState()
    val allTimePayments by viewModel.allTimePayments.collectAsState()
    val monthPurchases by viewModel.monthPurchases.collectAsState()
    val monthPayments by viewModel.monthPayments.collectAsState()
    val storesWithDebt by viewModel.storesWithDebt.collectAsState()
    val userSummaries by viewModel.userSummaries.collectAsState()

    val totalDebt = storesWithDebt.fold(0.0) { acc, item -> acc + item.debt }.coerceAtLeast(0.0)
    val monthName = remember { SimpleDateFormat("MMMM yyyy", Locale("ar")).format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("التقارير", fontSize = 22.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Text("📊 $monthName", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "مشتريات الشهر", viewModel.formatAmount(monthPurchases), Color(0xFFF44336))
            ReportCard(Modifier.weight(1f), Icons.Default.Payment, "مدفوعات الشهر", viewModel.formatAmount(monthPayments), Color(0xFF4CAF50))
        }
        Spacer(Modifier.height(8.dp))
        ReportCard(Modifier.fillMaxWidth(), Icons.Default.TrendingDown, "صافي الشهر", viewModel.formatAmount(monthPurchases - monthPayments), if (monthPurchases - monthPayments > 0) Color(0xFFF44336) else Color(0xFF4CAF50))

        Spacer(Modifier.height(24.dp))

        Text("📈 إجمالي", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "إجمالي المشتريات", viewModel.formatAmount(allTimePurchases), Color(0xFFF44336))
            ReportCard(Modifier.weight(1f), Icons.Default.Payment, "إجمالي المدفوعات", viewModel.formatAmount(allTimePayments), Color(0xFF4CAF50))
        }
        Spacer(Modifier.height(8.dp))
        ReportCard(Modifier.fillMaxWidth(), Icons.Default.Warning, "إجمالي المديونية", viewModel.formatAmount(totalDebt), if (totalDebt > 0) Color(0xFFF44336) else Color(0xFF4CAF50))

        Spacer(Modifier.height(24.dp))

        if (userSummaries.isNotEmpty()) {
            Text("👥 ملخص المستخدمين", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            for (user in userSummaries) {
                val userDebt = user.totalPurchases - user.totalPayments
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (userDebt > 0) Color(0xFF2A1A1A) else if (userDebt < 0) Color(0xFF1A2A1A) else Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(user.senderTag, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                if (userDebt > 0) "عليه ${viewModel.formatAmount(userDebt)}"
                                else if (userDebt < 0) "له ${viewModel.formatAmount(kotlin.math.abs(userDebt))}"
                                else "مسدد",
                                color = if (userDebt > 0) Color(0xFFF44336) else if (userDebt < 0) Color(0xFF4CAF50) else Color.Gray,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Divider(color = Color(0xFF333333))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("مشتريات", color = Color.Gray, fontSize = 11.sp)
                                Text(viewModel.formatAmount(user.totalPurchases), color = Color(0xFFF44336), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("مدفوعات", color = Color.Gray, fontSize = 11.sp)
                                Text(viewModel.formatAmount(user.totalPayments), color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي المستخدمين", color = Color.Gray, fontSize = 12.sp)
                    Text("${userSummaries.size}", color = Color(0xFF2196F3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("🏪 ملخص البقالات", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (storesWithDebt.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("لا توجد بقالات مسجلة", color = Color.Gray, modifier = Modifier.padding(16.dp))
            }
        } else {
            for (item in storesWithDebt) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.debt > 0) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.store.name, color = Color.White, fontSize = 14.sp)
                        Text(
                            if (item.debt > 0) "عليك: ${viewModel.formatAmount(item.debt)}"
                            else if (item.debt < 0) "لك: ${viewModel.formatAmount(kotlin.math.abs(item.debt))}"
                            else "مسدد",
                            color = if (item.debt > 0) Color(0xFFF44336) else if (item.debt < 0) Color(0xFF4CAF50) else Color.Gray,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ReportCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
