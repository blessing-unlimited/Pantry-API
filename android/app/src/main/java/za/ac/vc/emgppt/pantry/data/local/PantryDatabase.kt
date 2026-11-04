package za.ac.vc.emgppt.pantry.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single-user on-device database for pantry rows, shopping lists, the sync
 * outbox, and the last recipe match list (Android Developers, 2024).
 */
@Database(
    entities = [
        PantryItemEntity::class,
        ShoppingListEntity::class,
        PendingOpEntity::class,
        RecipeCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao

    companion object {
        fun create(context: Context): PantryDatabase {
            return Room.databaseBuilder(context, PantryDatabase::class.java, "pantry.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
