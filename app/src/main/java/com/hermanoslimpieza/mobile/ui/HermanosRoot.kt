package com.hermanoslimpieza.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.hermanoslimpieza.mobile.data.HermanosApi
import com.hermanoslimpieza.mobile.data.TokenStore

@Composable
fun HermanosRoot(api: HermanosApi, tokenStore: TokenStore) {
    val vm: AppViewModel = viewModel(factory = SimpleVmFactory { AppViewModel(api, tokenStore) })
    var authenticated by remember { mutableStateOf(vm.hasSession) }

    if (!authenticated) {
        LoginScreen(
            loading = vm.state.loading,
            error = vm.state.error,
            onLogin = { email, pass -> vm.login(email, pass) { authenticated = true } },
            onDismissError = vm::clearError
        )
        return
    }

    LaunchedEffect(Unit) { vm.refreshCore() }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBottom = route in listOf("today", "calendar", "crm", "new")

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    val items = listOf(
                        Triple("today", "Hoy", Icons.Default.Today),
                        Triple("calendar", "Calendario", Icons.Default.CalendarMonth),
                        Triple("crm", "CRM", Icons.Default.Chat),
                        Triple("new", "Nuevo", Icons.Default.AddCircle)
                    )
                    items.forEach { (r, label, icon) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = {
                                nav.navigate(r) {
                                    popUpTo("today") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            val error = vm.state.error
            if (error != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = vm::clearError) { Text("Cerrar") }
                    }
                ) { Text(error) }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.padding(padding)
        ) {
            composable("today") {
                TodayScreen(
                    state = vm.state,
                    onRefresh = { vm.loadToday() },
                    onOpen = { id -> vm.loadAppointment(id) { nav.navigate("appointment/$id") } },
                    onLogout = { vm.logout { authenticated = false } }
                )
            }
            composable("calendar") {
                CalendarScreen(
                    state = vm.state,
                    onMonth = vm::loadCalendar,
                    onOpen = { id -> vm.loadAppointment(id) { nav.navigate("appointment/$id") } }
                )
            }
            composable("crm") {
                CrmScreen(
                    state = vm.state,
                    onRefresh = vm::loadChats,
                    onOpen = { chat -> vm.openChat(chat) { nav.navigate("chat") } }
                )
            }
            composable("chat") {
                ChatScreen(
                    state = vm.state,
                    onBack = { nav.popBackStack() },
                    onRefresh = vm::refreshChat,
                    onSend = vm::sendMessage,
                    onAnalyze = vm::analyzeChat,
                    onCreateFromAi = { nav.navigate("new?fromAi=1") }
                )
            }
            composable(
                route = "new?fromAi={fromAi}",
                arguments = listOf(navArgument("fromAi") {
                    type = NavType.IntType
                    defaultValue = 0
                })
            ) { entry ->
                NewServiceScreen(
                    state = vm.state,
                    initial = if (entry.arguments?.getInt("fromAi") == 1) vm.state.extracted else null,
                    onSave = { draft ->
                        vm.createAppointment(draft) { id ->
                            vm.loadAppointment(id) {
                                nav.navigate("appointment/$id")
                            }
                        }
                    }
                )
            }
            composable(
                "appointment/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                AppointmentDetailScreen(
                    state = vm.state,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

class SimpleVmFactory<T : androidx.lifecycle.ViewModel>(
    private val creator: () -> T
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <R : androidx.lifecycle.ViewModel> create(modelClass: Class<R>): R = creator() as R
}
