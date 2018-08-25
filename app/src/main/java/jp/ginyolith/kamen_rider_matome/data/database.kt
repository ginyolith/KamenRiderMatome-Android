package jp.ginyolith.kamen_rider_matome.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.*
import android.arch.persistence.room.migration.Migration
import android.content.Context
import jp.ginyolith.kamen_rider_matome.data.article.Article
import jp.ginyolith.kamen_rider_matome.data.article.local.ArticlesDao
import jp.ginyolith.kamen_rider_matome.data.blog.Blog
import jp.ginyolith.kamen_rider_matome.data.blog.local.BlogsDao
import java.util.*




@Database(entities = [Article::class, Blog::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RSSDatabase : RoomDatabase() {
    abstract fun articleDao() : ArticlesDao
    abstract fun blogDao() : BlogsDao

    companion object {
        private var INSTANCE: RSSDatabase? = null
        private const val database_name = "rss.db"
        private val lock = Any()

        fun getInstance(context: Context): RSSDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            RSSDatabase::class.java, database_name)
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

val migration_3_to_4 = object : Migration(3, 4){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Blog "
                + " ADD COLUMN initialized INTEGER")
    }

}