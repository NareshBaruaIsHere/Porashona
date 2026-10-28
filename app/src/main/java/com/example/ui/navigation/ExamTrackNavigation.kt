package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.addedit.AddEditSubjectScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.detail.SubjectDetailScreen
import com.example.ui.settings.SettingsScreen

object Destinations {
    const val DASHBOARD = "dashboard"
    const val ADD_SUBJECT = "add_subject"
    const val EDIT_SUBJECT = "edit_subject/{subjectId}"
    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    const val SETTINGS = "settings"

    fun editSubject(subjectId: String) = "edit_subject/$subjectId"
    fun subjectDetail(subjectId: String) = "subject_detail/$subjectId"
}

@Composable
fun ExamTrackNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Destinations.DASHBOARD
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Destinations.DASHBOARD) {
            DashboardScreen(
                onNavigateToAddSubject = {
                    navController.navigate(Destinations.ADD_SUBJECT)
                },
                onNavigateToEditSubject = { subjectId ->
                    navController.navigate(Destinations.editSubject(subjectId))
                },
                onNavigateToSubjectDetail = { subjectId ->
                    navController.navigate(Destinations.subjectDetail(subjectId))
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                }
            )
        }

        composable(Destinations.ADD_SUBJECT) {
            AddEditSubjectScreen(
                subjectId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Destinations.EDIT_SUBJECT,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            AddEditSubjectScreen(
                subjectId = subjectId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubjectDeleted = {
                    navController.popBackStack(Destinations.DASHBOARD, inclusive = false)
                }
            )
        }

        composable(
            route = Destinations.SUBJECT_DETAIL,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectDetailScreen(
                subjectId = subjectId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { editSubjectId ->
                    navController.navigate(Destinations.editSubject(editSubjectId))
                }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
