package com.wheatherapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wheatherapp.WeatherApplication
import com.wheatherapp.data.network.NetworkMonitor
import com.wheatherapp.ui.screens.detail.DayDetailScreen
import com.wheatherapp.ui.screens.favourites.SavedLocationsScreen
import com.wheatherapp.ui.screens.home.HomeScreen
import com.wheatherapp.ui.screens.home.WeatherViewModel
import com.wheatherapp.ui.screens.settings.SettingsScreen

/**
 * Bottom navigation bar tab destinations.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Weather : Screen("weather", "Weather", Icons.Filled.WbSunny)
    data object Saved : Screen("saved", "Saved", Icons.Filled.Favorite)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomTabs = listOf(Weather, Saved, Settings)
    }
}

/**
 * Route definition for Day Detail screen.
 */
object DetailDestination {
    const val route = "detail/{dayIndex}"
    const val argDayIndex = "dayIndex"

    fun createRoute(dayIndex: Int): String = "detail/$dayIndex"
}

/**
 * Central NavGraph managing navigation between Weather, Detail, Saved, and Settings views.
 */
@Composable
fun WeatherNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    weatherViewModel: WeatherViewModel = run {
        val context = LocalContext.current
        val appContainer = (context.applicationContext as WeatherApplication).container
        viewModel(
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WeatherViewModel(
                        weatherRepository = appContainer.weatherRepository,
                        networkMonitor = NetworkMonitor(context)
                    ) as T
                }
            }
        )
    }
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = Screen.bottomTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomTabs.forEach { tab ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(tab.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Weather.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Weather (Main / Home screen)
            composable(Screen.Weather.route) {
                val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()
                val userSettings by weatherViewModel.userSettings.collectAsStateWithLifecycle()
                val searchQuery by weatherViewModel.searchQuery.collectAsStateWithLifecycle()
                val searchResults by weatherViewModel.searchResults.collectAsStateWithLifecycle()
                val favouriteLocations by weatherViewModel.favouriteLocations.collectAsStateWithLifecycle()

                HomeScreen(
                    uiState = uiState,
                    userSettings = userSettings,
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    favouriteLocations = favouriteLocations,
                    onSearchQueryChange = { query -> weatherViewModel.onSearchQueryChange(query) },
                    onSearchSubmitted = { query -> weatherViewModel.onSearchSubmitted(query) },
                    onLocationSelect = { location -> weatherViewModel.selectLocation(location) },
                    onDayClick = { dayIndex ->
                        navController.navigate(DetailDestination.createRoute(dayIndex))
                    },
                    onToggleFavourite = { location ->
                        weatherViewModel.toggleFavourite(location)
                    },
                    onRetry = {
                        weatherViewModel.refreshForecast()
                    }
                )
            }

            // 2. Day Detail screen
            composable(
                route = DetailDestination.route,
                arguments = listOf(
                    navArgument(DetailDestination.argDayIndex) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val dayIndex = backStackEntry.arguments?.getInt(DetailDestination.argDayIndex) ?: 0
                val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()
                val userSettings by weatherViewModel.userSettings.collectAsStateWithLifecycle()

                DayDetailScreen(
                    dayIndex = dayIndex,
                    uiState = uiState,
                    userSettings = userSettings,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 3. Saved Locations screen
            composable(Screen.Saved.route) {
                val favouriteLocations by weatherViewModel.favouriteLocations.collectAsStateWithLifecycle()
                val userSettings by weatherViewModel.userSettings.collectAsStateWithLifecycle()

                SavedLocationsScreen(
                    favouriteLocations = favouriteLocations,
                    userSettings = userSettings,
                    onSelectLocation = { location ->
                        weatherViewModel.selectLocation(location)
                        navController.navigate(Screen.Weather.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRemoveFavourite = { location ->
                        weatherViewModel.toggleFavourite(location)
                    }
                )
            }

            // 4. Settings screen
            composable(Screen.Settings.route) {
                val userSettings by weatherViewModel.userSettings.collectAsStateWithLifecycle()

                SettingsScreen(
                    userSettings = userSettings,
                    onUpdateSettings = { newSettings ->
                        weatherViewModel.updateUserSettings(newSettings)
                    }
                )
            }
        }
    }
}
