package jp.ginyolith.kamen_rider_matome.data

import android.arch.persistence.room.*
import android.content.Context
import java.util.*


@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg articles : Article)

    @Query("select * from article order by pubDate desc")
    fun selectAll() : List<Article>
}

@Dao
interface BlogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(vararg blog : Blog)

    @Update
    fun update(vararg blog : Blog)

    @Query("select * from blog")
    fun selectAll() : List<Blog>
}

@Database(entities = [Article::class, Blog::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RSSDatabase : RoomDatabase() {
    abstract fun articleDao() : ArticleDao
    abstract fun blogDao() : BlogDao

    companion object {
        private var INSTANCE: RSSDatabase? = null
        private const val database_name = "rss.db"
        private val lock = Any()

        fun getInstance(context: Context): RSSDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            RSSDatabase::class.java, database_name)
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build()
                }
                return INSTANCE!!
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date?  = value?.run { Date(value) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.run { date.time }

    @TypeConverter
    fun fromLong(value : Long?) : Blog.Enum? = value?.run { Blog.Enum.fromOrdinal(value.toInt()) }

    @TypeConverter
    fun enumToLong(blogEnum : Blog.Enum?) : Long? = blogEnum?.run { blogEnum.ordinal.toLong() }
}