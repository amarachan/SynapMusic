package com.synap.music.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synap.music.Database
import com.synap.music.R
import com.synap.music.models.Playlist
import com.synap.music.ui.styling.LocalAppearance
import com.synap.music.utils.secondary
import com.synap.music.utils.semiBold

@Composable
fun PlaylistSelectionDialog(
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    val playlists by Database.playlistPreviews(com.synap.music.enums.PlaylistSortBy.Name, com.synap.music.enums.SortOrder.Ascending).collectAsState(initial = emptyList())
    val lazyListState = rememberLazyListState()

    var isCreatingPlaylist by remember { mutableStateOf(false) }

    if (isCreatingPlaylist) {
        TextFieldDialog(
            hintText = "Enter the playlist name",
            onDismiss = { isCreatingPlaylist = false },
            onDone = { text ->
                isCreatingPlaylist = false
                com.synap.music.query {
                    Database.insert(Playlist(name = text))
                }
            }
        )
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            BasicText(
                text = "Add to Playlist",
                style = typography.s.semiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCreatingPlaylist = true }
                            .padding(vertical = 12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.accent),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicText(
                            text = "Create new playlist",
                            style = typography.xs.semiBold.copy(color = colorPalette.accent)
                        )
                    }
                }

                itemsIndexed(items = playlists) { _, preview ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistSelected(preview.playlist, preview.songCount) }
                            .padding(vertical = 12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.playlist),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            BasicText(
                                text = preview.playlist.name,
                                style = typography.xs.semiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            BasicText(
                                text = "${preview.songCount} songs",
                                style = typography.xxs.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                DialogTextButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}
