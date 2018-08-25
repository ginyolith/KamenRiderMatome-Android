/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.ginyolith.kamen_rider_matome.data.blog.local;

import android.arch.persistence.room.*
import jp.ginyolith.kamen_rider_matome.data.blog.Blog

/**
 * Data Access Object for the tasks table.
 */
@Dao
interface BlogsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(vararg blog : Blog)

    @Update
    fun update(vararg blog : Blog)

    @Query("select * from blog")
    fun selectAll() : List<Blog>

    @Query("select * from blog where _id = :id")
    fun selectById(id : Long) : Blog?
}
