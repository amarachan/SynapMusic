/*
 * Copyright (C) 2026 SynapMusic Maintainers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.synap.music

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.os.Parcel
import androidx.core.database.getFloatOrNull
import androidx.media3.common.MediaItem
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.synap.music.enums.AlbumSortBy
import com.synap.music.enums.ArtistSortBy
import com.synap.music.enums.PlaylistSortBy
import com.synap.music.enums.SongSortBy
import com.synap.music.enums.SortOrder
import com.synap.music.models.*
import com.synap.music.utils.asMediaItem
import kotlin.jvm.Throws
import kotlinx.coroutines.flow.Flow

/**
 * The Data Access Object (DAO).
 * This interface defines all the SQL queries for the app. 
 * We use Room to handle the boilerplate of mapping these to Kotlin objects.
 */
@Dao
interface Database {
    companion object : Database by DatabaseInitializer.Instance.database

    /**
     * Sorted Track Lists.
     * We have multiple variations for sorting by date, title, and play time 
     * to keep the library feeling organized.
     * 
     * Note: These queries dynamically resolve the thumbnail URL using COALESCE 
     * (Song -> Album -> YouTube CDN) to ensure offline tracks always display artwork.
     */
    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) GROUP BY Song.id ORDER BY Song.ROWID ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRowIdAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) GROUP BY Song.id ORDER BY Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRowIdDesc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) GROUP BY Song.id ORDER BY Song.title ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) GROUP BY Song.id ORDER BY Song.title DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByTitleDesc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) GROUP BY Song.id ORDER BY totalPlayTimeMs ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) GROUP BY Song.id ORDER BY totalPlayTimeMs DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByPlayTimeDesc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id LEFT JOIN Event ON Song.id = Event.songId WHERE Song.totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) OR Song.id IN (SELECT songId FROM Event) GROUP BY Song.id ORDER BY COALESCE(MAX(Event.timestamp), 0) ASC, Song.rowid ASC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRecentlyPlayedAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id LEFT JOIN Event ON Song.id = Event.songId WHERE Song.totalPlayTimeMs > 0 OR Song.id IN (SELECT songId FROM Format) OR Song.id IN (SELECT songId FROM Event) GROUP BY Song.id ORDER BY COALESCE(MAX(Event.timestamp), 0) DESC, Song.rowid DESC")
    @RewriteQueriesToDropUnusedColumns
    fun songsByRecentlyPlayedDesc(): Flow<List<Song>>

    fun songs(sortBy: SongSortBy, sortOrder: SortOrder): Flow<List<Song>> {
        return when (sortBy) {
            SongSortBy.PlayTime -> when (sortOrder) {
                SortOrder.Ascending -> songsByPlayTimeAsc()
                SortOrder.Descending -> songsByPlayTimeDesc()
            }
            SongSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> songsByTitleAsc()
                SortOrder.Descending -> songsByTitleDesc()
            }
            SongSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> songsByRowIdAsc()
                SortOrder.Descending -> songsByRowIdDesc()
            }
            SongSortBy.RecentlyPlayed -> when (sortOrder) {
                SortOrder.Ascending -> songsByRecentlyPlayedAsc()
                SortOrder.Descending -> songsByRecentlyPlayedDesc()
            }
        }
    }

    @Transaction
    @Query("SELECT Song.id, Song.title, Song.artistsText, Song.durationText, COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, Song.likedAt, Song.totalPlayTimeMs FROM Song LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId LEFT JOIN Album ON SongAlbumMap.albumId = Album.id WHERE likedAt IS NOT NULL GROUP BY Song.id ORDER BY likedAt DESC")
    @RewriteQueriesToDropUnusedColumns
    fun favorites(): Flow<List<Song>>

    @Query("SELECT * FROM QueuedMediaItem")
    fun queue(): List<QueuedMediaItem>

    @Query("DELETE FROM QueuedMediaItem")
    fun clearQueue()

    @Query("SELECT * FROM SearchQuery WHERE `query` LIKE :value ORDER BY id DESC")
    fun queries(value: String): Flow<List<SearchQuery>>

    @Query("SELECT COUNT (*) FROM SearchQuery")
    fun queriesCount(): Flow<Int>

    @Query("DELETE FROM SearchQuery")
    fun clearQueries()

    @Query("SELECT * FROM Song WHERE id = :id")
    fun song(id: String): Flow<Song?>

    @Query("SELECT likedAt FROM Song WHERE id = :songId")
    fun likedAt(songId: String): Flow<Long?>

    @Query("UPDATE Song SET likedAt = :likedAt WHERE id = :songId")
    fun like(songId: String, likedAt: Long?): Int

    @Query("UPDATE Song SET durationText = :durationText WHERE id = :songId")
    fun updateDurationText(songId: String, durationText: String): Int

    @Query("SELECT * FROM Lyrics WHERE songId = :songId")
    fun lyrics(songId: String): Flow<Lyrics?>

    @Query("SELECT * FROM Artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongArtistMap WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as totalSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as anyFormatSongs
        FROM (
            SELECT * FROM Artist 
            ORDER BY CASE WHEN id LIKE 'synthetic_artist_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT artistId FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId) 
        GROUP BY name
        ORDER BY name ASC
    """)
    fun artistsWithStatusByNameAsc(): Flow<List<ArtistWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongArtistMap WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as totalSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as anyFormatSongs
        FROM (
            SELECT * FROM Artist 
            ORDER BY CASE WHEN id LIKE 'synthetic_artist_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT artistId FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId) 
        GROUP BY name
        ORDER BY name DESC
    """)
    fun artistsWithStatusByNameDesc(): Flow<List<ArtistWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongArtistMap WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as totalSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as anyFormatSongs
        FROM (
            SELECT * FROM Artist 
            ORDER BY CASE WHEN id LIKE 'synthetic_artist_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT artistId FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId) 
        GROUP BY name
        ORDER BY bookmarkedAt ASC
    """)
    fun artistsWithStatusByRowIdAsc(): Flow<List<ArtistWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongArtistMap WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as totalSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId WHERE artistId IN (SELECT id FROM Artist WHERE name = A.name)) as anyFormatSongs
        FROM (
            SELECT * FROM Artist 
            ORDER BY CASE WHEN id LIKE 'synthetic_artist_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT artistId FROM SongArtistMap JOIN Format ON SongArtistMap.songId = Format.songId) 
        GROUP BY name
        ORDER BY bookmarkedAt DESC
    """)
    fun artistsWithStatusByRowIdDesc(): Flow<List<ArtistWithDownloadStatus>>

    fun artistsWithStatus(sortBy: ArtistSortBy, sortOrder: SortOrder): Flow<List<ArtistWithDownloadStatus>> {
        return when (sortBy) {
            ArtistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> artistsWithStatusByNameAsc()
                SortOrder.Descending -> artistsWithStatusByNameDesc()
            }
            ArtistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> artistsWithStatusByRowIdAsc()
                SortOrder.Descending -> artistsWithStatusByRowIdDesc()
            }
        }
    }

    @Query("SELECT * FROM Album WHERE id = :id")
    fun album(id: String): Flow<Album?>

    @Query("SELECT timestamp FROM Album WHERE id = :id")
    fun albumTimestamp(id: String): Long?

    @Transaction
    @Query("SELECT * FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE SongAlbumMap.albumId = :albumId ORDER BY position ASC, Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun albumSongs(albumId: String): Flow<List<Song>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as totalSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as anyFormatSongs
        FROM (
            SELECT * FROM Album 
            ORDER BY CASE WHEN id LIKE 'synthetic_album_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR 
              A.title IN (SELECT DISTINCT title FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId JOIN Format ON SongAlbumMap.songId = Format.songId)
        GROUP BY title
        ORDER BY title ASC
    """)
    fun albumsWithStatusByTitleAsc(): Flow<List<AlbumWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as totalSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as anyFormatSongs
        FROM (
            SELECT * FROM Album 
            ORDER BY CASE WHEN id LIKE 'synthetic_album_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR 
              A.title IN (SELECT DISTINCT title FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId JOIN Format ON SongAlbumMap.songId = Format.songId)
        GROUP BY title
        ORDER BY year ASC
    """)
    fun albumsWithStatusByYearAsc(): Flow<List<AlbumWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as totalSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as anyFormatSongs
        FROM (
            SELECT * FROM Album 
            ORDER BY CASE WHEN id LIKE 'synthetic_album_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR 
              A.title IN (SELECT DISTINCT title FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId JOIN Format ON SongAlbumMap.songId = Format.songId)
        GROUP BY title
        ORDER BY bookmarkedAt ASC
    """)
    fun albumsWithStatusByRowIdAsc(): Flow<List<AlbumWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as totalSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as anyFormatSongs
        FROM (
            SELECT * FROM Album 
            ORDER BY CASE WHEN id LIKE 'synthetic_album_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR 
              A.title IN (SELECT DISTINCT title FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId JOIN Format ON SongAlbumMap.songId = Format.songId)
        GROUP BY title
        ORDER BY title DESC
    """)
    fun albumsWithStatusByTitleDesc(): Flow<List<AlbumWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as totalSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as anyFormatSongs
        FROM (
            SELECT * FROM Album 
            ORDER BY CASE WHEN id LIKE 'synthetic_album_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR 
              A.title IN (SELECT DISTINCT title FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId JOIN Format ON SongAlbumMap.songId = Format.songId)
        GROUP BY title
        ORDER BY year DESC
    """)
    fun albumsWithStatusByYearDesc(): Flow<List<AlbumWithDownloadStatus>>

    @Transaction
    @Query("""
        SELECT *, 
        (SELECT COUNT(*) FROM SongAlbumMap WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as totalSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title) AND Format.contentLength > 0) as downloadedSongs,
        (SELECT COUNT(*) FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId WHERE albumId IN (SELECT id FROM Album WHERE title = A.title)) as anyFormatSongs
        FROM (
            SELECT * FROM Album 
            ORDER BY CASE WHEN id LIKE 'synthetic_album_%' THEN 1 ELSE 0 END ASC, id ASC
        ) as A
        WHERE bookmarkedAt IS NOT NULL OR 
              A.title IN (SELECT DISTINCT title FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId JOIN Format ON SongAlbumMap.songId = Format.songId)
        GROUP BY title
        ORDER BY bookmarkedAt DESC
    """)
    fun albumsWithStatusByRowIdDesc(): Flow<List<AlbumWithDownloadStatus>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT albumId FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId) ORDER BY title ASC")
    fun albumsByTitleAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT albumId FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId) ORDER BY year ASC")
    fun albumsByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT albumId FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId) ORDER BY bookmarkedAt ASC")
    fun albumsByRowIdAsc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT albumId FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId) ORDER BY title DESC")
    fun albumsByTitleDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT albumId FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId) ORDER BY year DESC")
    fun albumsByYearDesc(): Flow<List<Album>>

    @Query("SELECT * FROM Album WHERE bookmarkedAt IS NOT NULL OR id IN (SELECT DISTINCT albumId FROM SongAlbumMap JOIN Format ON SongAlbumMap.songId = Format.songId) ORDER BY bookmarkedAt DESC")
    fun albumsByRowIdDesc(): Flow<List<Album>>

    fun albums(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<Album>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsByTitleAsc()
                SortOrder.Descending -> albumsByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsByYearAsc()
                SortOrder.Descending -> albumsByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsByRowIdAsc()
                SortOrder.Descending -> albumsByRowIdDesc()
            }
        }
    }

    fun albumsWithStatus(sortBy: AlbumSortBy, sortOrder: SortOrder): Flow<List<AlbumWithDownloadStatus>> {
        return when (sortBy) {
            AlbumSortBy.Title -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithStatusByTitleAsc()
                SortOrder.Descending -> albumsWithStatusByTitleDesc()
            }
            AlbumSortBy.Year -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithStatusByYearAsc()
                SortOrder.Descending -> albumsWithStatusByYearDesc()
            }
            AlbumSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> albumsWithStatusByRowIdAsc()
                SortOrder.Descending -> albumsWithStatusByRowIdDesc()
            }
        }
    }

    @Query("UPDATE Song SET totalPlayTimeMs = totalPlayTimeMs + :addition WHERE id = :id")
    fun incrementTotalPlayTimeMs(id: String, addition: Long)

    @Query("UPDATE Song SET totalPlayTimeMs = 0")
    fun clearTotalPlayTimeMs()

    @Query("UPDATE Song SET totalPlayTimeMs = 0 WHERE id = :songId")
    fun clearTotalPlayTimeMsFor(songId: String)

    @Transaction
    @Query("SELECT * FROM Playlist WHERE id = :id")
    fun playlistWithSongs(id: Long): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT id, name, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY name ASC")
    fun playlistPreviewsByNameAsc(): Flow<List<PlaylistPreview>>

    @Transaction
    @Query("SELECT id, name, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY ROWID ASC")
    fun playlistPreviewsByDateAddedAsc(): Flow<List<PlaylistPreview>>

    @Transaction
    @Query("SELECT id, name, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY songCount ASC")
    fun playlistPreviewsByDateSongCountAsc(): Flow<List<PlaylistPreview>>

    @Transaction
    @Query("SELECT id, name, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY name DESC")
    fun playlistPreviewsByNameDesc(): Flow<List<PlaylistPreview>>

    @Transaction
    @Query("SELECT id, name, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY ROWID DESC")
    fun playlistPreviewsByDateAddedDesc(): Flow<List<PlaylistPreview>>

    @Transaction
    @Query("SELECT id, name, (SELECT COUNT(*) FROM SongPlaylistMap WHERE playlistId = id) as songCount FROM Playlist ORDER BY songCount DESC")
    fun playlistPreviewsByDateSongCountDesc(): Flow<List<PlaylistPreview>>

    fun playlistPreviews(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ): Flow<List<PlaylistPreview>> {
        return when (sortBy) {
            PlaylistSortBy.Name -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByNameAsc()
                SortOrder.Descending -> playlistPreviewsByNameDesc()
            }
            PlaylistSortBy.SongCount -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByDateSongCountAsc()
                SortOrder.Descending -> playlistPreviewsByDateSongCountDesc()
            }
            PlaylistSortBy.DateAdded -> when (sortOrder) {
                SortOrder.Ascending -> playlistPreviewsByDateAddedAsc()
                SortOrder.Descending -> playlistPreviewsByDateAddedDesc()
            }
        }
    }

    @Query("SELECT thumbnailUrl FROM Song JOIN SongPlaylistMap ON id = songId WHERE playlistId = :id ORDER BY position LIMIT 4")
    fun playlistThumbnailUrls(id: Long): Flow<List<String?>>

    @Transaction
    @Query("SELECT * FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId WHERE SongArtistMap.artistId = :artistId AND (totalPlayTimeMs > 0 OR id IN (SELECT songId FROM Format)) ORDER BY Song.ROWID DESC")
    @RewriteQueriesToDropUnusedColumns
    fun artistSongs(artistId: String): Flow<List<Song>>

    @Query("SELECT * FROM Format WHERE songId = :songId")
    fun format(songId: String): Flow<Format?>

    @Query("SELECT * FROM Format")
    fun allFormats(): List<Format>

    @Query("DELETE FROM Format WHERE songId = :songId")
    fun deleteFormat(songId: String): Int

    /**
     * Retrieves all downloaded songs (Vault).
     * Uses COALESCE to provide a fallback chain for thumbnails (Song -> Album -> YouTube CDN)
     * so that Vault items display artwork even if they were saved without it.
     */
    @Transaction
    @Query("""
        SELECT Song.id, Song.title, Song.artistsText, Song.durationText, 
               COALESCE(Song.thumbnailUrl, Album.thumbnailUrl, 'https://i.ytimg.com/vi/' || Song.id || '/hqdefault.jpg') AS thumbnailUrl, 
               Song.likedAt, Song.totalPlayTimeMs, Format.contentLength, Format.compressionEngine 
        FROM Song 
        JOIN Format ON Song.id = Format.songId 
        LEFT JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId 
        LEFT JOIN Album ON SongAlbumMap.albumId = Album.id 
        WHERE Format.contentLength > 0 
        GROUP BY Song.id 
        ORDER BY Song.ROWID DESC
    """)
    fun songsWithContentLength(): Flow<List<SongWithContentLength>>

    @Query("""
        UPDATE SongPlaylistMap SET position = 
          CASE 
            WHEN position < :fromPosition THEN position + 1
            WHEN position > :fromPosition THEN position - 1
            ELSE :toPosition
          END 
        WHERE playlistId = :playlistId AND position BETWEEN MIN(:fromPosition,:toPosition) and MAX(:fromPosition,:toPosition)
    """)
    fun move(playlistId: Long, fromPosition: Int, toPosition: Int)

    @Query("DELETE FROM SongPlaylistMap WHERE playlistId = :id")
    fun clearPlaylist(id: Long)

    /**
     * Wipes an album's tracklist before applying a fresh sync from the YouTube API.
     * Hardware Lock: The NOT IN clause protects downloaded files. If a user downloaded a "Video" ID 
     * but the official Album sync provides "Audio" IDs, this ensures the offline Video ID 
     * is not deleted, keeping the album visible in the offline library.
     */
    @Query("DELETE FROM SongAlbumMap WHERE albumId = :id AND songId NOT IN (SELECT songId FROM Format WHERE contentLength > 0)")
    fun clearAlbum(id: String)

    @Query("SELECT loudnessDb FROM Format WHERE songId = :songId")
    fun loudnessDb(songId: String): Flow<Float?>

    @Query("SELECT * FROM Song WHERE title LIKE :query OR artistsText LIKE :query")
    fun search(query: String): Flow<List<Song>>

    @Query("SELECT albumId AS id, Album.title AS name FROM SongAlbumMap JOIN Album ON albumId = Album.id WHERE songId = :songId")
    fun songAlbumInfo(songId: String): Info?

    @Query("SELECT id, name FROM Artist LEFT JOIN SongArtistMap ON id = artistId WHERE songId = :songId")
    fun songArtistInfo(songId: String): List<Info>

    @Transaction
    @Query("SELECT Song.* FROM Event JOIN Song ON Song.id = songId GROUP BY songId ORDER BY SUM(CAST(playTime AS REAL) / (((:now - timestamp) / 86400000) + 1)) DESC LIMIT 1")
    @RewriteQueriesToDropUnusedColumns
    fun trending(now: Long = System.currentTimeMillis()): Flow<Song?>

    @Query("SELECT COUNT (*) FROM Event")
    fun eventsCount(): Flow<Int>

    @Query("DELETE FROM Event")
    fun clearEvents()

    @Query("DELETE FROM Event WHERE songId = :songId")
    fun clearEventsFor(songId: String)

    @Upsert
    fun upsert(songArtistMap: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Throws(SQLException::class)
    fun insert(event: Event)

    @Upsert
    fun upsert(format: Format)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchQuery: SearchQuery)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(songPlaylistMap: SongPlaylistMap): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(songArtistMap: SongArtistMap): Long

    @Upsert
    fun upsert(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(queuedMediaItems: List<QueuedMediaItem>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSongPlaylistMaps(songPlaylistMaps: List<SongPlaylistMap>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(album: Album, songAlbumMap: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(artists: List<Artist>, songArtistMaps: List<SongArtistMap>)

    @Query("SELECT * FROM Song WHERE id = :id")
    fun getSong(id: String): Song?

    @Query("SELECT * FROM Format WHERE songId = :songId")
    fun getFormat(songId: String): Format?

    @Query("SELECT * FROM Playlist WHERE id = :id")
    fun getPlaylistSync(id: Long): Playlist?

    @Query("SELECT * FROM Song JOIN SongPlaylistMap ON Song.id = SongPlaylistMap.songId WHERE playlistId = :id ORDER BY position ASC")
    fun getPlaylistSongsSync(id: Long): List<Song>

    @Query("SELECT * FROM Album WHERE id = :id")
    fun getAlbum(id: String): Album?

    @Query("SELECT * FROM Lyrics WHERE songId = :songId")
    fun getLyrics(songId: String): Lyrics?

    @Query("SELECT * FROM Album JOIN SongAlbumMap ON Album.id = SongAlbumMap.albumId WHERE songId = :songId")
    fun getSongAlbumsSync(songId: String): List<Album>

    @Query("SELECT * FROM Artist JOIN SongArtistMap ON Artist.id = SongArtistMap.artistId WHERE songId = :songId")
    fun getSongArtistsSync(songId: String): List<Artist>

    @Query("SELECT * FROM Artist WHERE id = :id")
    fun getArtist(id: String): Artist?

    @Query("SELECT position FROM SongAlbumMap WHERE songId = :songId AND albumId = :albumId")
    fun getAlbumPosition(songId: String, albumId: String): Int?

    @Query("SELECT * FROM Song JOIN SongAlbumMap ON Song.id = SongAlbumMap.songId WHERE albumId = :albumId ORDER BY position ASC")
    fun getAlbumSongsSync(albumId: String): List<Song>

    @Query("SELECT * FROM Song JOIN SongArtistMap ON Song.id = SongArtistMap.songId WHERE artistId = :artistId")
    fun getArtistSongsSync(artistId: String): List<Song>

    @Transaction
    fun insert(mediaItem: MediaItem, block: (Song) -> Song = { it }) {
        val songId = mediaItem.mediaId
        
        // Grab whatever we already have in the DB so we don't lose play counts or "Likes"
        val existingSong = getSong(songId)
        val existingAlbumInfo = songAlbumInfo(songId)
        val existingArtistInfo = songArtistInfo(songId)
        val albumId = mediaItem.mediaMetadata.extras?.getString("albumId") ?: existingAlbumInfo?.id
        val existingAlbum = albumId?.let { getAlbum(it) }
        val albumThumbnailUrl = mediaItem.mediaMetadata.extras?.getString("albumThumbnailUrl") ?: existingAlbum?.thumbnailUrl

        val youtubeFallbackThumbnail = if (songId.isNotBlank() && !songId.startsWith("http")) {
            "https://i.ytimg.com/vi/$songId/hqdefault.jpg"
        } else null

        val song = Song(
            id = songId,
            title = mediaItem.mediaMetadata.title!!.toString(),
            artistsText = mediaItem.mediaMetadata.artist?.toString() ?: existingSong?.artistsText,
            durationText = mediaItem.mediaMetadata.extras?.getString("durationText") ?: existingSong?.durationText,
            thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString() 
                ?: albumThumbnailUrl 
                ?: existingSong?.thumbnailUrl
                ?: youtubeFallbackThumbnail,
            likedAt = existingSong?.likedAt,
            totalPlayTimeMs = existingSong?.totalPlayTimeMs ?: 0
        ).let(block)
        
        upsert(song) // Safe update: preserves existing mappings and format records

        var albumTitle = mediaItem.mediaMetadata.albumTitle?.toString() ?: existingAlbumInfo?.name
        
        // If we still don't have an album title, check if it's a video result (YT often omits album for videos)
        // We'll treat the artist name as a fallback "Collection" name for videos if needed.
        if (albumTitle == null && mediaItem.mediaMetadata.extras?.getBoolean("isVideo") == true) {
            albumTitle = mediaItem.mediaMetadata.artist?.toString() ?: "Various Artists"
        }

        if (albumId != null) {
            val existingAlbum = getAlbum(albumId)

            // If this is an official ID, check if we created a temporary "synthetic" one earlier.
            if (!albumId.startsWith("synthetic_album_") && albumTitle != null) {
                val artistPrefix = (mediaItem.mediaMetadata.artist?.toString() ?: existingSong?.artistsText ?: "")
                    .lowercase().filter { it.isLetterOrDigit() }
                val syntheticId = "synthetic_album_${artistPrefix}_${albumTitle.lowercase().filter { it.isLetterOrDigit() }}"
                val syntheticAlbum = getAlbum(syntheticId)
                if (syntheticAlbum != null) {
                    remapAlbumSongs(syntheticId, albumId)
                    delete(syntheticAlbum)
                }
            }

            val cleanAlbumTitle = (albumTitle ?: existingAlbum?.title)?.takeIf { title ->
                val t = title.lowercase()
                !t.contains("save this song") && 
                !t.contains("views") && 
                !t.contains("plays") && 
                !t.contains("subscribers")
            }

            upsert(Album(
                id = albumId, 
                title = cleanAlbumTitle ?: "Single",
                thumbnailUrl = albumThumbnailUrl ?: existingAlbum?.thumbnailUrl, 
                authorsText = mediaItem.mediaMetadata.artist?.toString() ?: existingAlbum?.authorsText,
                bookmarkedAt = existingAlbum?.bookmarkedAt
            ))
            
            @Suppress("DEPRECATION")
            val position = mediaItem.mediaMetadata.extras?.get("position") as? Int
                ?: if (albumId == existingAlbumInfo?.id) getAlbumPosition(songId, albumId) else null
            
            upsert(SongAlbumMap(songId = song.id, albumId = albumId, position = position))
        } else if (albumTitle != null) {
            val t = albumTitle.lowercase()
            if (!t.contains("save this song") && !t.contains("views") && !t.contains("plays") && !t.contains("subscribers")) {
                // Synthetic Linking for Albums.
                val artistPrefix = (mediaItem.mediaMetadata.artist?.toString() ?: existingSong?.artistsText ?: "")
                    .lowercase().filter { it.isLetterOrDigit() }
                val syntheticId = "synthetic_album_${artistPrefix}_${albumTitle.lowercase().filter { it.isLetterOrDigit() }}"
                val existingAlbum = getAlbum(syntheticId)
                upsert(Album(
                    id = syntheticId, 
                    title = albumTitle, 
                    authorsText = mediaItem.mediaMetadata.artist?.toString() ?: existingAlbum?.authorsText,
                    bookmarkedAt = existingAlbum?.bookmarkedAt
                ))
                upsert(SongAlbumMap(songId = song.id, albumId = syntheticId, position = null))
            }
        }

        val artistNames = mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames")
            ?: mediaItem.mediaMetadata.artist?.toString()?.split(", ", "&", " and ")?.map { it.trim() }
            ?: existingArtistInfo.mapNotNull { it.name }.takeIf { it.isNotEmpty() }
            
        val artistIds = mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")
            ?: existingArtistInfo.map { it.id }.takeIf { it.isNotEmpty() }

        if (artistNames != null) {
            // YT likes to leak technical junk like "token" or "continuation" into the artist names.
            // We block that here so your library stays clean.
            val forbidden = setOf("token", "trackingparams", "clicktracking", "continuation", "and", "&", ",", "•")
            val finalArtistIds = artistIds ?: artistNames.map { name ->
                "synthetic_artist_${name.lowercase().filter { it.isLetterOrDigit() }}"
            }
            
            if (artistNames.size == finalArtistIds.size) {
                artistNames.forEachIndexed { index, rawName ->
                    // Clean off messy prefixes like "and " or "& " that YT sometimes attaches.
                    val cleanName = rawName.replace(Regex("^(?i)(and|&|,|•)\\s+"), "").trim()
                    if (cleanName.lowercase() in forbidden || cleanName.isEmpty()) return@forEachIndexed

                    val id = finalArtistIds[index]
                    val existingArtist = getArtist(id)
                    
                    // Same deal as albums: merge temporary local profiles into real official ones.
                    if (!id.startsWith("synthetic_artist_")) {
                        val syntheticId = "synthetic_artist_${cleanName.lowercase().filter { it.isLetterOrDigit() }}"
                        val syntheticArtist = getArtist(syntheticId)
                        if (syntheticArtist != null) {
                            remapArtistSongs(syntheticId, id)
                            delete(syntheticArtist)
                        }
                    }

                    // Try to keep the highest quality thumbnail we've found so far
                    val artistThumbnailUrl = mediaItem.mediaMetadata.extras?.getString("artistThumbnailUrl") 
                        ?: existingArtist?.thumbnailUrl
                    
                    upsert(Artist(
                        id = id, 
                        name = cleanName, 
                        thumbnailUrl = artistThumbnailUrl,
                        bookmarkedAt = existingArtist?.bookmarkedAt
                    ))
                    insert(SongArtistMap(songId = song.id, artistId = id)) 
                }
            }
        }
    }

    @Transaction
    fun cleanDatabaseMetadata() {
        // YT Music sends back junk like "token" or "and Anand Bakshi".
        // This task sweeps the DB to fix any existing mess.
        val forbidden = setOf("token", "trackingparams", "clicktracking", "continuation", "and", "&", ",", "•")
        
        // 1. Scrub Artist Names
        allArtists().forEach { artist ->
            val name = artist.name ?: return@forEach
            val lowercaseName = name.lowercase().trim()
            
            // Delete generic profiles that aren't actually people
            if (lowercaseName in forbidden || lowercaseName == "various artists") {
                delete(artist)
                return@forEach
            }

            // Strip out "and ", "& ", etc. if they got stuck to the front of a name
            val cleanName = name.replace(Regex("^(?i)(and|&|,|•)\\s+"), "").trim()
            
            if (cleanName != name) {
                val existing = getArtistByName(cleanName)
                if (existing != null && existing.id != artist.id) {
                    // We found a better, clean version. Move songs over and toss the messy one.
                    remapArtistSongs(artist.id, existing.id)
                    delete(artist)
                } else {
                    // No duplicate? Just fix the name on the current profile.
                    upsert(artist.copy(name = cleanName))
                }
            }
        }

        // 2. Scrub Album Titles
        allAlbums().forEach { album ->
            val title = album.title ?: return@forEach
            val cleanTitle = title.replace(Regex("^(?i)(and|&|,|•)\\s+"), "").trim()
            
            if (cleanTitle != title) {
                val existing = getAlbumByTitle(cleanTitle)
                if (existing != null && existing.id != album.id) {
                    remapAlbumSongs(album.id, existing.id)
                    delete(album)
                } else {
                    upsert(album.copy(title = cleanTitle))
                }
            }
        }
    }

    @Query("SELECT * FROM Artist")
    fun allArtists(): List<Artist>

    @Query("SELECT * FROM Album")
    fun allAlbums(): List<Album>

    @Query("SELECT * FROM Artist WHERE name = :name LIMIT 1")
    fun getArtistByName(name: String): Artist?

    @Query("SELECT * FROM Album WHERE title = :title LIMIT 1")
    fun getAlbumByTitle(title: String): Album?

    @Query("UPDATE SongArtistMap SET artistId = :newId WHERE artistId = :oldId")
    fun remapArtistSongs(oldId: String, newId: String)

    @Query("UPDATE SongAlbumMap SET albumId = :newId WHERE albumId = :oldId")
    fun remapAlbumSongs(oldId: String, newId: String)

    @Transaction
    fun addSongsToPlaylist(songIds: List<String>, playlistId: Long, startPosition: Int) {
        var currentPosition = startPosition
        songIds.forEach { id ->
            getSong(id)?.let { song ->
                insert(song.asMediaItem)
                insert(SongPlaylistMap(songId = id, playlistId = playlistId, position = currentPosition))
                currentPosition++
            }
        }
    }

    @Transaction
    fun addAlbumsToPlaylist(albumIds: List<String>, playlistId: Long, startPosition: Int) {
        var currentPosition = startPosition
        albumIds.forEach { albumId ->
            val songs = getAlbumSongsSync(albumId)
            songs.forEach { song: Song ->
                insert(song.asMediaItem)
                insert(SongPlaylistMap(songId = song.id, playlistId = playlistId, position = currentPosition))
                currentPosition++
            }
        }
    }

    @Transaction
    fun addArtistsToPlaylist(artistIds: List<String>, playlistId: Long, startPosition: Int) {
        var currentPosition = startPosition
        artistIds.forEach { artistId ->
            val songs = getArtistSongsSync(artistId)
            songs.forEach { song: Song ->
                insert(song.asMediaItem)
                insert(SongPlaylistMap(songId = song.id, playlistId = playlistId, position = currentPosition))
                currentPosition++
            }
        }
    }

    @Update
    fun update(artist: Artist)

    @Update
    fun update(album: Album)

    @Update
    fun update(playlist: Playlist)

    @Upsert
    fun upsert(lyrics: Lyrics)

    @Upsert
    fun upsert(album: Album)

    @Upsert
    fun upsert(songAlbumMaps: List<SongAlbumMap>)

    @Transaction
    fun upsert(album: Album, songAlbumMaps: List<SongAlbumMap>) {
        upsert(album)
        upsert(songAlbumMaps)
    }

    @Upsert
    fun upsert(songAlbumMap: SongAlbumMap)

    @Transaction
    fun atomicAlbumUpdate(albumId: String, album: Album, mediaItems: List<MediaItem>) {
        clearAlbum(albumId)
        upsert(album)
        mediaItems.forEach { mediaItem ->
            insert(mediaItem)
        }
        
        upsert(mediaItems.mapIndexed { index, mediaItem ->
            SongAlbumMap(songId = mediaItem.mediaId, albumId = albumId, position = index)
        })
    }

    @Upsert
    fun upsert(artist: Artist)

    @Delete
    fun delete(album: Album)

    @Delete
    fun delete(artist: Artist)

    @Delete
    fun delete(searchQuery: SearchQuery)

    @Delete
    fun delete(playlist: Playlist)

    @Delete
    fun delete(songPlaylistMap: SongPlaylistMap)

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
    }
}

@androidx.room.Database(
    entities = [
        Song::class,
        SongPlaylistMap::class,
        Playlist::class,
        Artist::class,
        SongArtistMap::class,
        Album::class,
        SongAlbumMap::class,
        SearchQuery::class,
        QueuedMediaItem::class,
        Format::class,
        Event::class,
        Lyrics::class,
    ],
    views = [
        SortedSongPlaylistMap::class
    ],
    version = 24,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = DatabaseInitializer.From3To4Migration::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = DatabaseInitializer.From7To8Migration::class),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 11, to = 12, spec = DatabaseInitializer.From11To12Migration::class),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = DatabaseInitializer.From20To21Migration::class),
        AutoMigration(from = 21, to = 22, spec = DatabaseInitializer.From21To22Migration::class),
        AutoMigration(from = 23, to = 24),
    ],
)
@TypeConverters(Converters::class)
abstract class DatabaseInitializer protected constructor() : RoomDatabase() {
    abstract val database: Database

    companion object {
        lateinit var Instance: DatabaseInitializer

        operator fun invoke(context: Context) {
            if (!::Instance.isInitialized) {
                Instance = Room
                    .databaseBuilder(context, DatabaseInitializer::class.java, "synap_vault.db")
                    .addMigrations(
                        From8To9Migration(),
                        From10To11Migration(),
                        From14To15Migration(),
                        From22To23Migration()
                    )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }

    @DeleteTable.Entries(DeleteTable(tableName = "QueuedMediaItem"))
    class From3To4Migration : AutoMigrationSpec

    @RenameColumn.Entries(RenameColumn("Song", "albumInfoId", "albumId"))
    class From7To8Migration : AutoMigrationSpec

    class From8To9Migration : Migration(8, 9) {
        override fun migrate(it: SupportSQLiteDatabase) {
            it.query(SimpleSQLiteQuery("SELECT DISTINCT browseId, text, Info.id FROM Info JOIN Song ON Info.id = Song.albumId;"))
                .use { cursor ->
                    val albumValues = ContentValues(2)
                    while (cursor.moveToNext()) {
                        albumValues.put("id", cursor.getString(0))
                        albumValues.put("title", cursor.getString(1))
                        it.insert("Album", CONFLICT_IGNORE, albumValues)

                        it.execSQL(
                            "UPDATE Song SET albumId = '${cursor.getString(0)}' WHERE albumId = ${
                                cursor.getLong(
                                    2
                                )
                            }"
                        )
                    }
                }

            it.query(SimpleSQLiteQuery("SELECT GROUP_CONCAT(text, ''), SongWithAuthors.songId FROM Info JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId GROUP BY songId;"))
                .use { cursor ->
                    val songValues = ContentValues(1)
                    while (cursor.moveToNext()) {
                        songValues.put("artistsText", cursor.getString(0))
                        it.update(
                            "Song",
                            CONFLICT_IGNORE,
                            songValues,
                            "id = ?",
                            arrayOf(cursor.getString(1))
                        )
                    }
                }

            it.query(SimpleSQLiteQuery("SELECT browseId, text, Info.id FROM Info JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId WHERE browseId NOT NULL;"))
                .use { cursor ->
                    val artistValues = ContentValues(2)
                    while (cursor.moveToNext()) {
                        artistValues.put("id", cursor.getString(0))
                        artistValues.put("name", cursor.getString(1))
                        it.insert("Artist", CONFLICT_IGNORE, artistValues)

                        it.execSQL(
                            "UPDATE SongWithAuthors SET authorInfoId = '${cursor.getString(0)}' WHERE authorInfoId = ${
                                cursor.getLong(
                                    2
                                )
                            }"
                        )
                    }
                }

            it.execSQL("INSERT INTO SongArtistMap(songId, artistId) SELECT songId, authorInfoId FROM SongWithAuthors")

            it.execSQL("DROP TABLE Info;")
            it.execSQL("DROP TABLE SongWithAuthors;")
        }
    }

    class From10To11Migration : Migration(10, 11) {
        override fun migrate(it: SupportSQLiteDatabase) {
            it.query(SimpleSQLiteQuery("SELECT id, albumId FROM Song;")).use { cursor ->
                val songAlbumMapValues = ContentValues(2)
                while (cursor.moveToNext()) {
                    songAlbumMapValues.put("songId", cursor.getString(0))
                    songAlbumMapValues.put("albumId", cursor.getString(1))
                    it.insert("SongAlbumMap", CONFLICT_IGNORE, songAlbumMapValues)
                }
            }

            it.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, `loudnessDb` REAL, `contentLength` INTEGER, PRIMARY KEY(`id`))")

            it.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength FROM Song;")
            it.execSQL("DROP TABLE Song;")
            it.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    @RenameTable("SongInPlaylist", "SongPlaylistMap")
    @RenameTable("SortedSongInPlaylist", "SortedSongPlaylistMap")
    class From11To12Migration : AutoMigrationSpec

    class From14To15Migration : Migration(14, 15) {
        override fun migrate(it: SupportSQLiteDatabase) {
            it.query(SimpleSQLiteQuery("SELECT id, loudnessDb, contentLength FROM Song;"))
                .use { cursor ->
                    val formatValues = ContentValues(3)
                    while (cursor.moveToNext()) {
                        formatValues.put("songId", cursor.getString(0))
                        formatValues.put("loudnessDb", cursor.getFloatOrNull(1))
                        formatValues.put("contentLength", cursor.getFloatOrNull(2))
                        it.insert("Format", CONFLICT_IGNORE, formatValues)
                    }
                }

            it.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            it.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs FROM Song;")
            it.execSQL("DROP TABLE Song;")
            it.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }

    @DeleteColumn.Entries(
        DeleteColumn("Artist", "shuffleVideoId"),
        DeleteColumn("Artist", "shufflePlaylistId"),
        DeleteColumn("Artist", "radioVideoId"),
        DeleteColumn("Artist", "radioPlaylistId"),
    )
    class From20To21Migration : AutoMigrationSpec

    @DeleteColumn.Entries(DeleteColumn("Artist", "info"))
    class From21To22Migration : AutoMigrationSpec

    class From22To23Migration : Migration(22, 23) {
        override fun migrate(it: SupportSQLiteDatabase) {
            it.execSQL("CREATE TABLE IF NOT EXISTS Lyrics (`songId` TEXT NOT NULL, `fixed` TEXT, `synced` TEXT, PRIMARY KEY(`songId`), FOREIGN KEY(`songId`) REFERENCES `Song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")

            it.query(SimpleSQLiteQuery("SELECT id, lyrics, synchronizedLyrics FROM Song;")).use { cursor ->
                val lyricsValues = ContentValues(3)
                while (cursor.moveToNext()) {
                    lyricsValues.put("songId", cursor.getString(0))
                    lyricsValues.put("fixed", cursor.getString(1))
                    lyricsValues.put("synced", cursor.getString(2))
                    it.insert("Lyrics", CONFLICT_IGNORE, lyricsValues)
                }
            }

            it.execSQL("CREATE TABLE IF NOT EXISTS Song_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT, `thumbnailUrl` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            it.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs FROM Song;")
            it.execSQL("DROP TABLE Song;")
            it.execSQL("ALTER TABLE Song_new RENAME TO Song;")
        }
    }
}

class Converters {
    @TypeConverter
    fun mediaItemFromByteArray(value: ByteArray?): MediaItem? {
        return value?.let { byteArray ->
            runCatching {
                val parcel = Parcel.obtain()
                parcel.unmarshall(byteArray, 0, byteArray.size)
                parcel.setDataPosition(0)
                val bundle = parcel.readBundle(MediaItem::class.java.classLoader)
                parcel.recycle()

                bundle?.let(MediaItem.CREATOR::fromBundle)
            }.getOrNull()
        }
    }

    @TypeConverter
    fun mediaItemToByteArray(mediaItem: MediaItem?): ByteArray? {
        return mediaItem?.toBundle()?.let { persistableBundle ->
            val parcel = Parcel.obtain()
            parcel.writeBundle(persistableBundle)
            val bytes = parcel.marshall()
            parcel.recycle()

            bytes
        }
    }
}

val Database.internal: RoomDatabase
    get() = DatabaseInitializer.Instance

fun query(block: () -> Unit) = DatabaseInitializer.Instance.queryExecutor.execute(block)

fun transaction(block: () -> Unit) = with(DatabaseInitializer.Instance) {
    transactionExecutor.execute {
        runInTransaction(block)
    }
}

val RoomDatabase.path: String?
    get() = openHelper.writableDatabase.path
