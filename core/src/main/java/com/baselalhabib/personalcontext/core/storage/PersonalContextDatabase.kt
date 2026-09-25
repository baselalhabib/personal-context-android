package com.baselalhabib.personalcontext.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.baselalhabib.personalcontext.core.entities.NoteEntity

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class PersonalContextDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: PersonalContextDatabase? = null

        fun getInstance(context: Context): PersonalContextDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PersonalContextDatabase::class.java,
                    "personal_context_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
