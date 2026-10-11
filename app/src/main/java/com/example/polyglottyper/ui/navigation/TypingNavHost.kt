package com.example.polyglottyper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.ui.game.GameScreen
import com.example.polyglottyper.ui.home.HomeScreen
import com.example.polyglottyper.ui.rain.RainScreen
import com.example.polyglottyper.ui.stats.StatsScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object StatsRoute

@Serializable
data class GameRoute(
    val languageCode: String,
    val modeId: String,
    val layoutId: String,
)

@Serializable
data class RainRoute(val languageCode: String, val layoutId: String)

@Composable
fun TypingNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onStartGame = { language, mode, layout ->
                    navController.navigate(GameRoute(language.code, mode.id, layout.id))
                },
                onPlayDaily = { language, layout ->
                    navController.navigate(GameRoute(language.code, GameMode.DailyChallenge.id, layout.id))
                },
                onStartRain = { language, layout ->
                    navController.navigate(RainRoute(language.code, layout.id))
                },
                onOpenStats = { navController.navigate(StatsRoute) },
            )
        }
        composable<GameRoute> {
            GameScreen(onExit = navController::popBackStack)
        }
        composable<RainRoute> {
            RainScreen(onExit = navController::popBackStack)
        }
        composable<StatsRoute> {
            StatsScreen(onBack = navController::popBackStack)
        }
    }
}
