package com.example.dualmind.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dualmind.ui.dashboard.DashboardScreen
import com.example.dualmind.ui.recording.RecordingScreen

// A clean way to manage our route names so we don't make typos
object Screen {
    const val Dashboard = "dashboard"
    const val Recording = "recording"
    const val Summary = "summary/{meetingId}" // Expects an ID to be passed in

    // Helper function to build the summary route
    fun createSummaryRoute(meetingId: Int) = "summary/$meetingId"
}

@Composable
fun DualMindNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // NavHost is the container that swaps out the screens based on the current route
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard // The app opens to the Dashboard first
    ) {

        // 1. DASHBOARD SCREEN
        composable(route = Screen.Dashboard) {
            DashboardScreen(
                onNavigateToRecording = {
                    navController.navigate(Screen.Recording)
                },
                onNavigateToSummary = { meetingId ->
                    navController.navigate(Screen.createSummaryRoute(meetingId))
                }
            )
        }

        // 2. RECORDING SCREEN
        composable(route = Screen.Recording) {
            RecordingScreen(
                // You could pass a callback here to pop the back stack,
                // but Android users can also just use the system back swipe/button
            )
        }

        // 3. SUMMARY SCREEN
        composable(route = Screen.Summary) { backStackEntry ->
            val meetingIdString = backStackEntry.arguments?.getString("meetingId")
            val meetingId = meetingIdString?.toIntOrNull() ?: -1

            // Add this to display the screen and handle the back button!
            com.example.dualmind.ui.summary.SummaryScreen(
                meetingId = meetingId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}