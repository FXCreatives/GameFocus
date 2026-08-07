package com.gamefocus.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gamefocus.ui.about.AboutScreen
import com.gamefocus.ui.achievements.AchievementScreen
import com.gamefocus.ui.analytics.AnalyticsScreen
import com.gamefocus.ui.dashboard.DashboardScreen
import com.gamefocus.ui.focus.FocusModeScreen
import com.gamefocus.ui.games.GameDetailsScreen
import com.gamefocus.ui.games.GamesScreen
import com.gamefocus.ui.settings.SettingsScreen
import com.gamefocus.ui.timeline.TimelineScreen
import com.gamefocus.ui.debug.DebugScreen
import com.gamefocus.ui.theme.EmeraldGreen
import com.gamefocus.ui.theme.GamingBackground
import com.gamefocus.ui.theme.GamingCard
import com.gamefocus.ui.theme.GamingSurface
import com.gamefocus.ui.theme.GamingError
import com.gamefocus.ui.theme.TextPrimary
import com.gamefocus.ui.theme.TextSecondary
import com.gamefocus.data.local.AppDatabase
import com.gamefocus.data.local.GameDao
import com.gamefocus.data.models.GameApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Games : Screen("games", "Games", Icons.AutoMirrored.Filled.List)
    object Timeline : Screen("timeline", "Timeline", Icons.AutoMirrored.Filled.List)
    object Analytics : Screen("analytics", "Statistics", Icons.Filled.Info)
    object Focus : Screen("focus", "Focus Mode", Icons.AutoMirrored.Filled.ArrowBack)
    object Achievements : Screen("achievements", "Achievements", Icons.Filled.Star)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object About : Screen("about", "About", Icons.Filled.Info)
    object Debug : Screen("debug", "Debug", Icons.AutoMirrored.Filled.ArrowBack)
    object GameDetails : Screen("game_details/{packageName}", "Game Details", Icons.Default.Star) {
        fun createRoute(packageName: String) = "game_details/$packageName"
    }
}

private val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Games,
    Screen.Settings
)

private val drawerItems = listOf(
    Screen.Timeline,
    Screen.Analytics,
    Screen.Focus,
    Screen.Achievements,
    Screen.About,
    Screen.Debug
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameFocusNavHost() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun getScreenTitle(route: String?): String {
        if (route == null) return "GameFocus"
        val screen = listOf(
            Screen.Dashboard,
            Screen.Games,
            Screen.Timeline,
            Screen.Analytics,
            Screen.Focus,
            Screen.Achievements,
            Screen.Settings,
            Screen.About,
            Screen.Debug,
            Screen.GameDetails
        ).find { screen ->
            if (screen.route.contains("{")) {
                route.startsWith(screen.route.substringBefore("{"))
            } else {
                route == screen.route
            }
        }
        return screen?.title ?: route.replaceFirstChar { it.uppercase() }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = GamingSurface,
                drawerContentColor = TextPrimary
            ) {
                DrawerHeader()
                Spacer(modifier = Modifier.height(12.dp))
                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { androidx.compose.material3.Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(24.dp)) },
                        label = { Text(screen.title, fontSize = 14.sp) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                            closeDrawer()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                            selectedTextColor = EmeraldGreen,
                            unselectedTextColor = TextSecondary,
                            unselectedIconColor = TextSecondary
                        )
                    )
                }
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                if (currentRoute != Screen.Dashboard.route) {
                    TopAppBar(
                        title = {
                            Text(
                                text = getScreenTitle(currentRoute),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Open menu",
                                    tint = TextPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = GamingBackground,
                            titleContentColor = TextPrimary
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = GamingSurface,
                    tonalElevation = 8.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination?.route
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                androidx.compose.material3.Icon(
                                    screen.icon, 
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = screen.title,
                                    fontSize = 12.sp
                                ) 
                            },
                            selected = currentDestination == screen.route,
                            onClick = {
                                if (currentDestination != screen.route) {
                                    navController.navigate(screen.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldGreen,
                                selectedTextColor = EmeraldGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = EmeraldGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onScanGames = { navController.navigate(Screen.Games.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
                composable(Screen.Games.route) {
                    GamesScreen(
                        onGameClick = { game ->
                            navController.navigate(Screen.GameDetails.createRoute(game.packageName))
                        }
                    )
                }
                composable(Screen.Timeline.route) { TimelineScreen() }
                composable(Screen.Analytics.route) { AnalyticsScreen() }
                composable(Screen.Focus.route) { FocusModeScreen() }
                composable(Screen.Achievements.route) { AchievementScreen() }
                composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
                composable(Screen.Debug.route) { DebugScreen() }
                composable(Screen.GameDetails.route) { backStackEntry ->
                    val packageName = backStackEntry.arguments?.getString("packageName")
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val db = remember { AppDatabase.getInstance(context) }
                    val gameDao = db.gameDao()
                    val gameState = remember { mutableStateOf<GameApp?>(null) }
                    val game by gameState
                    var loadError by remember { mutableStateOf<String?>(null) }

                    androidx.compose.runtime.LaunchedEffect(packageName) {
                        if (packageName != null) {
                            try {
                                val entity = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    gameDao.getGameByPackageName(packageName)
                                }
                                if (entity != null) {
                                    gameState.value = GameApp(
                                        packageName = entity.packageName,
                                        appName = entity.appName,
                                        icon = null,
                                        category = "GAME",
                                        installDate = entity.installDate,
                                        updateDate = entity.updateDate
                                    )
                                } else {
                                    loadError = "Game not found in database"
                                }
                            } catch (e: Exception) {
                                loadError = "Failed to load game: ${e.message}"
                            }
                        } else {
                            loadError = "Invalid package name"
                        }
                    }

                    val currentGame = gameState.value
                    when {
                        loadError != null -> {
                            androidx.compose.material3.Scaffold(
                                topBar = {
                                    androidx.compose.material3.TopAppBar(
                                        title = { Text("Game Details", color = TextPrimary, fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = TextSecondary
                                                )
                                            }
                                        },
                                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                            containerColor = GamingBackground,
                                            titleContentColor = TextPrimary
                                        )
                                    )
                                }
                            ) { padding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GamingBackground)
                                        .padding(padding)
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = GamingError,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Game Not Found",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = loadError ?: "Unknown error",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    androidx.compose.material3.Button(
                                        onClick = { navController.popBackStack() },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Go Back", color = GamingBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        currentGame != null -> {
                            GameDetailsScreen(
                                game = currentGame,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        else -> {
                            // Loading state
                            androidx.compose.material3.Scaffold(
                                topBar = {
                                    androidx.compose.material3.TopAppBar(
                                        title = { Text("Game Details", color = TextPrimary, fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = TextSecondary
                                                )
                                            }
                                        },
                                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                            containerColor = GamingBackground,
                                            titleContentColor = TextPrimary
                                        )
                                    )
                                }
                            ) { padding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GamingBackground)
                                        .padding(padding)
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(color = EmeraldGreen)
                                }
                            }
                        }
                    }
                }
                composable("about") { AboutScreen() }
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(EmeraldGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "GameFocus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Stay Focused. Play Smarter.",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldGreen,
            fontWeight = FontWeight.Medium
        )
    }
}
