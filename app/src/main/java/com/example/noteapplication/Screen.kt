package com.example.noteapplication

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Home: Screen("home")
    object Signin: Screen("signin")
    object Signup: Screen("signup")
    object AddNote : Screen("add_note")

    object UpdateNote : Screen("update_note_screen/{noteId}/{title}/{content}/{imagePath}") {
        fun createRoute(noteId: String, title: String, content: String, imagePath: String): String {
            return "update_note_screen/${noteId}/${Uri.encode(title)}/${Uri.encode(content)}/${Uri.encode(imagePath)}"
        }
    }
}

@Composable
fun Mynavigation()
{
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Signin.route
    ){
        composable(Screen.Signin.route){
            SignInScreen(navController = navController)
        }
        composable( Screen.Home.route){
            HomeScreen(navController = navController)
        }
        composable(Screen.Signup.route){
            SignUpScreen(navController = navController)
        }
        composable(Screen.AddNote.route) {
            AddNoteScreen(navController = navController)
        }
        composable(
            route = Screen.UpdateNote.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("content") { type = NavType.StringType },
                navArgument("imagePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val content = backStackEntry.arguments?.getString("content") ?: ""
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""

            UpdateNoteScreen(
                navController = navController,
                noteId = noteId,
                initialTitle = title,
                initialContent = content,
                initialImageUrl = imagePath
            )
        }
    }
}
