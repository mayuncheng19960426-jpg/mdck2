package com.sillyandroid.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sillyandroid.app.ui.screen.*
import com.sillyandroid.app.ui.theme.ColorPalette

// --- Route Constants ---
object Routes {
    const val CHATS = "chats"
    const val CHAT_DETAIL = "chat/{chatId}"
    const val CHARACTERS = "characters"
    const val CHARACTER_EDIT = "character/edit/{characterId}"
    const val WORLD_BOOK = "worldbook/{bookId}"
    const val AGENTS = "agents"
    const val AGENT_TASK = "agent/{taskId}"
    const val SETTINGS = "settings"

    fun chatDetail(chatId: Long) = "chat/$chatId"
    fun characterEdit(characterId: Long = -1) = "character/edit/$characterId"
    fun worldBook(bookId: Long = 1) = "worldbook/$bookId"
    fun agentTask(taskId: Long = -1) = "agent/$taskId"
}

// --- Bottom Nav Items ---
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.CHATS, "聊天", Icons.Default.Chat),
    BottomNavItem(Routes.CHARACTERS, "角色", Icons.Default.Person),
    BottomNavItem(Routes.AGENTS, "Agent", Icons.Default.Groups),
    BottomNavItem(Routes.SETTINGS, "设置", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SillyNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            // Show bottom bar only on top-level destinations
            val showBottomBar = currentDestination?.hierarchy?.any { dest ->
                bottomNavItems.any { it.route == dest.route }
            } == true

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ColorPalette.Accent,
                                selectedTextColor = ColorPalette.Accent,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHATS
        ) {
            composable(Routes.CHATS) {
                ChatListScreen(innerPadding, navController)
            }
            composable(Routes.CHAT_DETAIL) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: return@composable
                ChatScreen(chatId, innerPadding, navController)
            }
            composable(Routes.CHARACTERS) {
                CharacterListScreen(innerPadding, navController)
            }
            composable(Routes.CHARACTER_EDIT) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId")?.toLongOrNull() ?: -1
                CharacterEditScreen(characterId, innerPadding, navController)
            }
            composable(Routes.WORLD_BOOK) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId")?.toLongOrNull() ?: 1
                WorldBookScreen(bookId, innerPadding, navController)
            }
            composable(Routes.AGENTS) {
                AgentListScreen(innerPadding, navController)
            }
            composable(Routes.AGENT_TASK) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull() ?: -1
                AgentTaskScreen(taskId, innerPadding, navController)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(innerPadding, navController)
            }
        }
    }
}
