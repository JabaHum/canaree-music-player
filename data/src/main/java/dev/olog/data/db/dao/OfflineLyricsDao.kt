package dev.olog.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.olog.data.db.entities.OfflineLyricsEntity

@Dao
internal abstract class OfflineLyricsDao {

    @Query("SELECT * FROM offline_lyrics WHERE trackId = :trackId")
     abstract fun observeLyrics(trackId: Long): LiveData<List<OfflineLyricsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     abstract suspend fun saveLyrics(lyrics: OfflineLyricsEntity)

}