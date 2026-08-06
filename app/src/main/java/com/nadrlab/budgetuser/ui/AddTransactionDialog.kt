package com.nadrlab.budgetuser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nadrlab.budgetuser.data.model.Store

private fun normalizeNumbers(text: String): String {
    return text
        .replace('٠', '0').replace('١', '1').replace('٢', '2')
        .replace('٣', '3').replace('٤', '4').replace('٥', '5')
        .replace('٦', '6').replace('٧', '7').replace('٨', '8')
        .replace('٩', '9')
        .replace('٫', '.')
}

private fun isValidAmount(text: String): Boolean {
    val normalized = normalizeNumbers(text.trim())
    val amount = normalized.toDoubleOrNull()
    return amount != null && amount > 0
}

@Composable
fun AddTransactionDialog(
    title: String,
    titleColor: Color,
    stores: List<Store>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, String, String) -> Unit
) {
    var selectedStoreIndex by remember { mutableIntStateOf(0) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showStoreDropdown by remember { mutableStateOf(false) }

    val canSubmit = isValidAmount(amountText) && stores.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(title, color = titleColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                if (stores.isEmpty()) {
                    Text("لا توجد بقالات. أضف بقالة أولاً من تبويب البقالات", color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("إغلاق", color = Color.White) }
                    return@Card
                }

                // ═══ اختيار البقالة ═══
                Text("البقالة:", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))

                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStoreDropdown = !showStoreDropdown },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stores.getOrNull(selectedStoreIndex)?.name ?: "اختر",
                                color = Color.White, fontSize = 15.sp
                            )
                            Icon(
                                if (showStoreDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                null, tint = Color(0xFF4CAF50)
                            )
                        }
                    }

                    if (showStoreDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 50.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            stores.forEachIndexed { index, store ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedStoreIndex = index
                                            showStoreDropdown = false
                                        }
                                        .background(if (index == selectedStoreIndex) Color(0xFF1A3A1A) else Color.Transparent)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        store.name,
                                        color = if (index == selectedStoreIndex) Color(0xFF4CAF50) else Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                                if (index < stores.lastIndex) Divider(color = Color(0xFF333333), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ═══ المبلغ (يقبل أرقام عربية وإنجليزية) ═══
                Text("المبلغ:", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newText ->
                        // يقبل أرقام عربية وإنجليزية ونقطة
                        amountText = newText.filter { c ->
                            c.isDigit() || c == '.' || c == '٫' ||
                            c in '٠'..'٩'
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: 50 أو ٥٠", color = Color(0xFF666666)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                if (amountText.isNotBlank() && !isValidAmount(amountText)) {
                    Spacer(Modifier.height(4.dp))
                    Text("أدخل رقماً صحيحاً", color = Color(0xFFFF9800), fontSize = 11.sp)
                }

                Spacer(Modifier.height(12.dp))

                // ═══ الوصف ═══
                Text("الوصف (اختياري):", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: مشتريات يومية", color = Color(0xFF666666)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(12.dp))

                // ═══ ملاحظة ═══
                Text("ملاحظة (اختياري):", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("أي ملاحظة إضافية", color = Color(0xFF666666)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(20.dp))

                // ═══ الأزرار ═══
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء", fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            val normalizedAmount = normalizeNumbers(amountText.trim())
                            val amount = normalizedAmount.toDoubleOrNull()
                            if (amount != null && amount > 0 && stores.isNotEmpty()) {
                                val storeId = stores.getOrNull(selectedStoreIndex)?.id
                                if (storeId != null) {
                                    onConfirm(storeId, amount, description, note)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = titleColor,
                            disabledContainerColor = Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "تسجيل",
                            color = if (canSubmit) Color.White else Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
