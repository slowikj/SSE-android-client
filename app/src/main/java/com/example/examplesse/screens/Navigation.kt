package com.example.examplesse.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.examplesse.screens.profiles.ProfilesScreen

sealed class Screen(val route: String) {
    object Profiles : Screen("profiles")

    object Details : Screen("details")

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Profiles.route) {
        composable(route = Screen.Profiles.route) {
            ProfilesScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }

        composable(
            route = Screen.Details.route + "/{name}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            )
        ) { entry ->
            DetailsScreen(name = entry.arguments?.getString("name") ?: "")
        }
    }
}

