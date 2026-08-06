
package com.nadrlab.budgetuser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onStart: (String) -> Unit) {
    var userName by remember { mutableStateOf("") }

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
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "ميزانية البيت",
            fontSize = 34.sp,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )

        Text(
            "BaitBudget",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "تطبيق إدارة ميزانية البيت\nومديونية البقالات",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(48.dp))

        Text(
            "ما اسمك؟",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("أدخل اسمك هنا", color = Color(0xFF666666)) },
            leadingIcon = {
                Icon(Icons.Default.Person, null, tint = Color(0xFF4CAF50))
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFF444444),
                cursorColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "اسمك سيُستخدم لتعريف معاملاتك عند التصدير",
            color = Color(0xFF666666),
            fontSize = 11.sp
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onStart(userName.trim()) },
            enabled = userName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xFF333333)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "ابدأ",
                color = if (userName.isNotBlank()) Color.White else Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
