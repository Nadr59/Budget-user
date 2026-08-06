package com.nadrlab.baitbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onAdminLogin: (String) -> Unit,
    onUserLogin: (String) -> Unit
) {
    var showAdminLogin by remember { mutableStateOf(false) }
    var showUserLogin by remember { mutableStateOf(false) }
    var adminPassword by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountBalance,
            null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text("ميزانية البيت", fontSize = 32.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        Text("BaitBudget", color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(48.dp))

        if (!showAdminLogin && !showUserLogin) {
            Text("اختر نوع الحساب", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showAdminLogin = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8C547)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, null, tint = Color.Black)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("مشرف", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("إدارة كاملة + تقارير + استيراد", color = Color(0xFF555555), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showUserLogin = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("مستخدم عادي", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("إدخال مشتريات ومدفوعات + تصدير", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        if (showAdminLogin) {
            Text("دخول المشرف", color = Color(0xFFE8C547), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = adminPassword,
                onValueChange = { adminPassword = it },
                label = { Text("كلمة مرور المشرف") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = Color.Gray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE8C547), unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFFE8C547)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))
            Text("كلمة المرور الافتراضية: 1234", color = Color(0xFF666666), fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onAdminLogin(adminPassword) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8C547)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("دخول", color = Color.Black, fontSize = 16.sp) }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showAdminLogin = false; adminPassword = "" }) {
                Text("رجوع", color = Color.Gray)
            }
        }

        if (showUserLogin) {
            Text("دخول المستخدم", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("اسمك سيُستخدم لتعريف معاملاتك", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("اسمك (مطلوب)") },
                placeholder = { Text("مثال: أحمد") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { if (userName.isNotBlank()) onUserLogin(userName.trim()) },
                enabled = userName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "دخول",
                    color = if (userName.isNotBlank()) Color.White else Color.Gray,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showUserLogin = false; userName = "" }) {
                Text("رجوع", color = Color.Gray)
            }
        }
    }
}
