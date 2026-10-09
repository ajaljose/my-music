package com.example.musicplayer.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.musicplayer.ui.theme.AccentGold

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {}
) {
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Favourites
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141519))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.8.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val selected = when (screen.route) {
                    Screen.Home.route -> currentRoute == Screen.Home.route
                    Screen.Search.route -> currentRoute == Screen.Search.route
                    Screen.Library.route -> currentRoute in listOf(
                        Screen.Library.route,
                        Screen.Songs.route,
                        Screen.Albums.route,
                        Screen.Artists.route,
                        Screen.Playlists.route,
                        Screen.Folders.route
                    )
                    Screen.Favourites.route -> currentRoute == Screen.Favourites.route ||
                            (currentRoute?.startsWith("playlist_detail") == true && currentRoute.contains("isFavorites=true"))
                    else -> currentRoute == screen.route
                }

                val contentColor = if (selected) AccentGold else Color(0xFF8E929E)
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onItemClick()
                            if (!selected) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    screen.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title,
                            tint = contentColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = screen.title,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
