package com.gamefocus.ui.games

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamefocus.data.models.GameApp
import com.gamefocus.ui.components.PremiumEmptyState
import com.gamefocus.ui.components.PremiumErrorCard
import com.gamefocus.ui.components.ShimmerEffect
import com.gamefocus.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    viewModel: GamesViewModel = viewModel(),
    onGameClick: (GameApp) -> Unit = {}
) {
    val games by viewModel.games.collectAsState()
    val query by viewModel.query.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val gameCount by viewModel.gameCount.collectAsState()
    val error by viewModel.error.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GamingBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Games ($gameCount)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Sort",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name", color = TextPrimary) },
                        onClick = {
                            viewModel.setSortOption(GamesViewModel.SortOption.NAME)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Last Played", color = TextPrimary) },
                        onClick = {
                            viewModel.setSortOption(GamesViewModel.SortOption.LAST_PLAYED)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recently Installed", color = TextPrimary) },
                        onClick = {
                            viewModel.setSortOption(GamesViewModel.SortOption.RECENTLY_INSTALLED)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Time", color = TextPrimary) },
                        onClick = {
                            viewModel.setSortOption(GamesViewModel.SortOption.PLAY_TIME)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Favorites", color = TextPrimary) },
                        onClick = {
                            viewModel.setSortOption(GamesViewModel.SortOption.FAVORITES)
                            showSortMenu = false
                        }
                    )
                }
                IconButton(onClick = { viewModel.refreshGames() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Again",
                        tint = EmeraldGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::searchGames,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text("Search games...", color = TextSecondary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search games", tint = TextSecondary) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = EmeraldGreen,
                unfocusedBorderColor = Divider,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = EmeraldGreen
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterOption == GamesViewModel.FilterOption.ALL,
                onClick = { viewModel.setFilterOption(GamesViewModel.FilterOption.ALL) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                    selectedLabelColor = EmeraldGreen
                ),
                modifier = Modifier.semantics { contentDescription = "Filter: All" }
            )
            FilterChip(
                selected = filterOption == GamesViewModel.FilterOption.FAVORITES,
                onClick = { viewModel.setFilterOption(GamesViewModel.FilterOption.FAVORITES) },
                label = { Text("Favorites") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                    selectedLabelColor = EmeraldGreen
                ),
                modifier = Modifier.semantics { contentDescription = "Filter: Favorites" }
            )
            FilterChip(
                selected = filterOption == GamesViewModel.FilterOption.RECENTLY_PLAYED,
                onClick = { viewModel.setFilterOption(GamesViewModel.FilterOption.RECENTLY_PLAYED) },
                label = { Text("Recent") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                    selectedLabelColor = EmeraldGreen
                ),
                modifier = Modifier.semantics { contentDescription = "Filter: Recent" }
            )
            FilterChip(
                selected = filterOption == GamesViewModel.FilterOption.NEVER_PLAYED,
                onClick = { viewModel.setFilterOption(GamesViewModel.FilterOption.NEVER_PLAYED) },
                label = { Text("New") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen.copy(alpha = 0.15f),
                    selectedLabelColor = EmeraldGreen
                ),
                modifier = Modifier.semantics { contentDescription = "Filter: New" }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            error?.let { err ->
                if (games.isEmpty() && !isRefreshing) {
                    PremiumErrorCard(
                        title = "Unable to load games",
                        description = err,
                        buttonText = "Retry",
                        onButtonClick = { viewModel.clearError(); viewModel.refreshGames() }
                    )
                } else {
                    Text(
                        text = err,
                        color = GamingError,
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                    )
                }
            }

            if (isRefreshing && !isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = EmeraldGreen
                )
            }

            if (games.isEmpty() && !isRefreshing && error == null && !isLoading) {
                PremiumEmptyState(
                    icon = Icons.Default.Search,
                    title = "No games found",
                    description = "Tap the refresh button to scan",
                    buttonText = "Scan Now",
                    onButtonClick = viewModel::refreshGames
                )
            }

            if (isLoading) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        GameShimmerCard()
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games, key = { it.packageName }) { game ->
                    GameCard(game, onClick = { onGameClick(game) }, onFavoriteClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        viewModel.toggleFavorite(game)
                    })
                }
            }
        }
    }
}

@Composable
private fun GameShimmerCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                ShimmerEffect(modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                ShimmerEffect(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun GameCard(game: GameApp, onClick: () -> Unit = {}, onFavoriteClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = "${game.appName}, ${game.packageName}, Favorite: ${game.isFavorite}"
            },
        colors = CardDefaults.cardColors(containerColor = GamingCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GameIcon(game)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (game.lastSessionDate > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last played: ${formatDate(game.lastSessionDate)} • ${formatDuration(game.lastSessionDuration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.8f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Never played",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.semantics { contentDescription = "Favorite ${game.appName}" }
            ) {
                Icon(
                    imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (game.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (game.isFavorite) EmeraldGreen else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GameIcon(game: GameApp) {
    if (game.icon != null) {
        Image(
            painter = rememberDrawablePainter(game.icon),
            contentDescription = game.appName,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GamingSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = game.appName,
                tint = EmeraldGreen,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    return "${minutes}m"
}
