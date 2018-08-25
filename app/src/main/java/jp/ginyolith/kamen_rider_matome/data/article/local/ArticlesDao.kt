package jp.ginyolith.kamen_rider_matome.data.article.local

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import jp.ginyolith.kamen_rider_matome.data.article.Article

@Dao
interface ArticlesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg articles : Article)

    @Query("select * from article order by pubDate desc")
    fun selectAll() : List<Article>
}