
package com.nadrlab.budgetuser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nadrlab.budgetuser.ui.BaitBudgetTheme
import com.nadrlab.budgetuser.ui.MainScreen
import com.nadrlab.budgetuser.ui.WelcomeScreen
import com.nadrlab.budgetuser.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            BaitBudgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    val viewModel: BudgetViewModel = viewModel()
                    val isReady by viewModel.isReady.collectAsState()

                    if (isReady) {
                        MainScreen(viewModel = viewModel)
                    } else {
                        WelcomeScreen(
                            onStart = { name -> viewModel.setUserName(name) }
                        )
                    }
                }
            }
        }
    }
}
