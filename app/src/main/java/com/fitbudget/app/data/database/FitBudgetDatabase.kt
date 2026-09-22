package com.fitbudget.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fitbudget.app.data.database.dao.DayMetaDao
import com.fitbudget.app.data.database.dao.ExpenseDao
import com.fitbudget.app.data.database.dao.FoodDao
import com.fitbudget.app.data.database.dao.MealDao
import com.fitbudget.app.data.database.dao.ProfileDao
import com.fitbudget.app.data.database.dao.ReminderDao
import com.fitbudget.app.data.database.dao.StepDao
import com.fitbudget.app.data.database.dao.WaterDao
import com.fitbudget.app.data.database.dao.WeightDao
import com.fitbudget.app.data.database.dao.WorkoutDao
import com.fitbudget.app.data.database.entity.DayMetaEntity
import com.fitbudget.app.data.database.entity.ExpenseEntity
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.data.database.entity.StepLogEntity
import com.fitbudget.app.data.database.entity.WaterLogEntity
import com.fitbudget.app.data.database.entity.WeightLogEntity
import com.fitbudget.app.data.database.entity.WorkoutSessionEntity

@Database(
    entities = [
        ProfileEntity::class,
        FoodEntity::class,
        MealEntryEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        WorkoutSessionEntity::class,
        StepLogEntity::class,
        ReminderEntity::class,
        ExpenseEntity::class,
        DayMetaEntity::class
    ],
    version = FitBudgetDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FitBudgetDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun stepDao(): StepDao
    abstract fun reminderDao(): ReminderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun dayMetaDao(): DayMetaDao

    companion object {
        const val VERSION = 1
        const val NAME = "fitbudget.db"

        /**
         * Migrations are explicit and additive so that an app update never destroys user data.
         * `fallbackToDestructiveMigration` is deliberately NOT used.
         *
         * When bumping [VERSION], append a migration here, e.g.:
         *
         * ```
         * val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE profile ADD COLUMN notes TEXT")
         *     }
         * }
         * ```
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        @Volatile
        private var instance: FitBudgetDatabase? = null

        fun get(context: Context): FitBudgetDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): FitBudgetDatabase =
            Room.databaseBuilder(context.applicationContext, FitBudgetDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()

        /** In-memory instance for tests. */
        fun inMemory(context: Context): FitBudgetDatabase =
            Room.inMemoryDatabaseBuilder(context, FitBudgetDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        /** Used by the "reset all data" flow so the next access rebuilds a fresh instance. */
        fun closeAndClear() {
            synchronized(this) {
                instance?.takeIf { it.isOpen }?.close()
                instance = null
            }
        }

        @Suppress("unused")
        private fun noOpMigrationExample(db: SupportSQLiteDatabase) = Unit
    }
}
