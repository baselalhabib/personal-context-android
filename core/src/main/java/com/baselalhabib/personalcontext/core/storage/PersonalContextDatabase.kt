package com.baselalhabib.personalcontext.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.baselalhabib.personalcontext.core.entities.AppUsageEntity
import com.baselalhabib.personalcontext.core.entities.CallLogEntity
import com.baselalhabib.personalcontext.core.entities.ContactEntity
import com.baselalhabib.personalcontext.core.entities.LocationEntity
import com.baselalhabib.personalcontext.core.entities.MessageEntity
import com.baselalhabib.personalcontext.core.entities.NoteEntity

@Database(
    entities = [
        NoteEntity::class,
        MessageEntity::class,
        AppUsageEntity::class,
        LocationEntity::class,
        CallLogEntity::class,
        ContactEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PersonalContextDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun messageDao(): MessageDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun locationDao(): LocationDao
    abstract fun callLogDao(): CallLogDao
    abstract fun contactDao(): ContactDao

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
