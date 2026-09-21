package za.ac.vc.emgppt.pantry

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.ac.vc.emgppt.pantry.ui.auth.AuthViewModel
import za.ac.vc.emgppt.pantry.ui.auth.OnboardingScreen
import za.ac.vc.emgppt.pantry.ui.auth.SignInScreen
import za.ac.vc.emgppt.pantry.ui.group.CookModeScreen
import za.ac.vc.emgppt.pantry.ui.group.ScanScreen
import za.ac.vc.emgppt.pantry.ui.home.HomeScreen
import za.ac.vc.emgppt.pantry.ui.main.MainViewModel
import za.ac.vc.emgppt.pantry.ui.pantry.PantryScreen
import za.ac.vc.emgppt.pantry.ui.recipes.RecipeDetailScreen
import za.ac.vc.emgppt.pantry.ui.recipes.RecipesScreen
import za.ac.vc.emgppt.pantry.ui.settings.SettingsScreen
import za.ac.vc.emgppt.pantry.ui.shopping.ShoppingListsScreen
import za.ac.vc.emgppt.pantry.ui.theme.Cream
import za.ac.vc.emgppt.pantry.ui.theme.Ivory
import za.ac.vc.emgppt.pantry.ui.theme.PantryTheme
import za.ac.vc.emgppt.pantry.ui.theme.SplashScreen
import za.ac.vc.emgppt.pantry.ui.theme.Terracotta

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Pantry", "MainActivity onCreate")
        enableEdgeToEdge()
        setContent {
            PantryTheme {
                PantryRoot(authViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("Pantry", "MainActivity onStart")
    }

    override fun onStop() {
        Log.d("Pantry", "MainActivity onStop")
        super.onStop()
    }
}

@Composable
private fun PantryRoot(authViewModel: AuthViewModel) {
    val authState by authViewModel.state.collectAsState()
    val navController = rememberNavController()
    val start = if (authState.signedIn) {
        if (authState.isNewUser) "onboarding" else "main"
    } else "splash"

    NavHost(navController = navController, startDestination = start) {
        composable("splash") {
            LaunchedEffect(authState.signedIn) {
                kotlinx.coroutines.delay(1500)
                val target = if (authState.signedIn) {
                    if (authState.isNewUser) "onboarding" else "main"
                } else "signIn"
                navController.navigate(target) {
                    popUpTo("splash") { inclusive = true }
                }
            }
            SplashScreen()
        }
        composable("signIn") {
            SignInScreen(authViewModel) {
                val target = if (authViewModel.state.value.isNewUser) "onboarding" else "main"
                navController.navigate(target) { popUpTo("signIn") { inclusive = true } }
            }
        }
        composable("onboarding") {
            val mainViewModel: MainViewModel = viewModel()
            OnboardingScreen(mainViewModel) {
                navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
            }
        }
        composable("main") {
            MainTabs(
                authViewModel = authViewModel,
                onSignedOut = {
                    navController.navigate("signIn") { popUpTo("main") { inclusive = true } }
                }
            )
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun MainTabs(authViewModel: AuthViewModel, onSignedOut: () -> Unit) {
    val mainViewModel: MainViewModel = viewModel()
    val state by mainViewModel.state.collectAsState()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val tabs = listOf(
        Tab("home", "Home", Icons.Outlined.Home),
        Tab("pantry", "Pantry", Icons.Outlined.Kitchen),
        Tab("recipes", "Recipes", Icons.Outlined.Restaurant),
        Tab("profile", "Profile", Icons.Outlined.Person)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    LaunchedEffect(Unit) { mainViewModel.refreshAll() }
    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            mainViewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (current in tabs.map { it.route }) {
                NavigationBar(containerColor = Ivory) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Terracotta,
                                indicatorColor = Terracotta,
                                unselectedIconColor = Terracotta.copy(alpha = 0.55f),
                                unselectedTextColor = Terracotta.copy(alpha = 0.55f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                if (state.loading && state.recipes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Terracotta)
                    }
                } else {
                    HomeScreen(state, onRecipeClick = { navController.navigate("recipe/$it") }, contentPadding = padding)
                }
            }
            composable("pantry") {
                PantryScreen(
                    state,
                    mainViewModel,
                    onScanClick = { navController.navigate("scan") },
                    onListsClick = { navController.navigate("lists") },
                    contentPadding = padding
                )
            }
            composable("recipes") {
                RecipesScreen(state, mainViewModel, onRecipeClick = { navController.navigate("recipe/$it") }, contentPadding = padding)
            }
            composable("profile") {
                SettingsScreen(
                    state = state,
                    viewModel = mainViewModel,
                    onSignOut = {
                        authViewModel.signOut()
                        onSignedOut()
                    },
                    contentPadding = padding
                )
            }
            composable(
                "recipe/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RecipeDetailScreen(
                    id,
                    mainViewModel,
                    onStartCooking = { navController.navigate("cook/$id") },
                    onOpenShoppingLists = { navController.navigate("lists") }
                )
            }
            composable(
                "cook/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) {
                val cookState by mainViewModel.state.collectAsState()
                CookModeScreen(cookState.recipeDetail)
            }
            composable("scan") {
                ScanScreen(mainViewModel) {
                    navController.popBackStack()
                }
            }
            composable("lists") {
                ShoppingListsScreen(state, mainViewModel, contentPadding = padding)
            }
        }
    }
}
