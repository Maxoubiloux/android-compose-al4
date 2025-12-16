package com.example.android_compose_al4.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.android_compose_al4.viewmodel.BankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    onEvent: (UiEvent) -> Unit,
    viewModel: BankViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Wallet,
        Screen.Transfer,
        Screen.Map,
        Screen.Profile
    )
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) { 
                    HomeDashboard(
                        viewModel = viewModel,
                        onNavigateToHistory = { 
                            navController.navigate(Screen.History.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToTransfer = {
                            navController.navigate(Screen.Transfer.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) 
                }
                composable(Screen.Wallet.route) { 
                    WalletScreen(
                        viewModel = viewModel,
                        onAccountClick = { account ->
                            val id = account.id
                            if (!id.isNullOrBlank()) {
                                navController.navigate("account/$id") {
                                    launchSingleTop = true
                                }
                            } else {
                                viewModel.showUiMessage("Impossible d'ouvrir ce compte: identifiant manquant")
                            }
                        }
                    )
                }
                
                composable(Screen.History.route) {
                    HistoryScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.Map.route) { 
                    MapScreen()
                }
                composable(Screen.Profile.route) { 
                    ProfileScreen(viewModel = viewModel)
                }
                composable(Screen.Transfer.route) {
                    TransferScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                composable(
                    route = Screen.AccountDetail.route,
                    arguments = listOf(navArgument("accountId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val accountId = backStackEntry.arguments?.getString("accountId")
                    if (!accountId.isNullOrBlank()) {
                        AccountDetailScreen(
                            viewModel = viewModel,
                            accountId = accountId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }

            if (viewModel.uiState.value.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chargement...")
                }
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object Wallet : Screen("wallet", "Portefeuille", Icons.Default.Payment)
    object Transfer : Screen("transfer", "Virement", Icons.Default.SwapHoriz)
    object Map : Screen("map", "Carte", Icons.Default.Map)
    object Profile : Screen("profile", "Profil", Icons.Default.AccountCircle)
    object History : Screen("history", "Historique")
    object AccountDetail : Screen("account/{accountId}", "Compte")
}

sealed class UiEvent {
    object NavigateToHome : UiEvent()
    object NavigateToWallet : UiEvent()
    object NavigateToTransfer : UiEvent()
    object NavigateToMap : UiEvent()
    object NavigateToProfile : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class ShowMessage(val message: String) : UiEvent()
}
