package com.example.android_compose_al4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.android_compose_al4.ui.screens.HomeScreen
import com.example.android_compose_al4.ui.screens.LoginScreen
import com.example.android_compose_al4.ui.theme.Androidcomposeal4Theme
import com.example.android_compose_al4.viewmodel.BankUiEvent
import com.example.android_compose_al4.viewmodel.BankViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankApp()
        }
    }
}

@Composable
fun BankApp() {
    Androidcomposeal4Theme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()
        

        val viewModel: BankViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        

        LaunchedEffect(key1 = true) {
            viewModel.events.collect { event ->
                when (event) {
                    is BankUiEvent.ShowMessage -> {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = event.message
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = "login"
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        snackbarHostState = snackbarHostState,
                        onEvent = { },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BankApp()
}