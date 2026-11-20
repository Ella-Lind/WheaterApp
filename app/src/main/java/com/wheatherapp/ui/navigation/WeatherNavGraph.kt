package com.wheatherapp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.wheatherapp.ui.screens.detail.DayDetailScreen
import com.wheatherapp.ui.screens.detail.DayDetailViewModel
import com.wheatherapp.ui.screens.favourites.FavouritesViewModel
import com.wheatherapp.ui.screens.favourites.SavedLocationsScreen
import com.wheatherapp.ui.screens.home.ForecastUiState
import com.wheatherapp.ui.screens.home.HomeScreen
import com.wheatherapp.ui.screens.home.HomeViewModel
import com.wheatherapp.ui.screens.settings.SettingsScreen
import com.wheatherapp.ui.screens.settings.SettingsViewModel
import com.wheatherapp.ui.util.ViewModelFactory

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
 * Central NavGraph managing navigation between Weather, Detail, Saved, and Settings views using dedicated ViewModels.
 */
@Composable
fun WeatherNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as WeatherApplication).container
    val viewModelFactory = ViewModelFactory(appContainer)

    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val favouritesViewModel: FavouritesViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val dayDetailViewModel: DayDetailViewModel = viewModel(factory = viewModelFactory)

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
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    uiState = homeUiState,
                    onSearchQueryChange = { query -> homeViewModel.onSearchQueryChanged(query) },
                    onSearchSubmitted = { query -> homeViewModel.onSearchSubmitted(query) },
                    onLocationSelect = { location -> homeViewModel.onSelectLocation(location) },
                    onDayClick = { dayIndex ->
                        navController.navigate(DetailDestination.createRoute(dayIndex))
                    },
                    onToggleFavourite = { location ->
                        homeViewModel.onToggleFavourite(location)
                    },
                    onRetry = {
                        homeViewModel.onRefresh()
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
                val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val detailUiState by dayDetailViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(dayIndex, homeUiState.forecastState) {
                    val forecastState = homeUiState.forecastState
                    if (forecastState is ForecastUiState.Success) {
                        val dateIso = forecastState.forecast.daily.getOrNull(dayIndex)?.dateIso ?: ""
                        dayDetailViewModel.loadFromForecast(forecastState.forecast, dateIso)
                    }
                }

                DayDetailScreen(
                    dayIndex = dayIndex,
                    uiState = detailUiState,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 3. Saved Locations screen
            composable(Screen.Saved.route) {
                val favouritesUiState by favouritesViewModel.uiState.collectAsStateWithLifecycle()

                SavedLocationsScreen(
                    uiState = favouritesUiState,
                    onFilterQueryChanged = { query -> favouritesViewModel.onFilterQueryChanged(query) },
                    onSelectLocation = { location ->
                        homeViewModel.onSelectLocation(location)
                        navController.navigate(Screen.Weather.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRemoveFavourite = { location ->
                        favouritesViewModel.onRemoveFavourite(location)
                    }
                )
            }

            // 4. Settings screen
            composable(Screen.Settings.route) {
                val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                SettingsScreen(
                    uiState = settingsUiState,
                    onTemperatureUnitChanged = { unit -> settingsViewModel.onTemperatureUnitChanged(unit) },
                    onSpeedUnitChanged = { unit -> settingsViewModel.onSpeedUnitChanged(unit) },
                    onRefreshIntervalChanged = { minutes -> settingsViewModel.onRefreshIntervalChanged(minutes) },
                    onThemeModeChanged = { mode -> settingsViewModel.onThemeModeChanged(mode) }
                )
            }
        }
    }
}
