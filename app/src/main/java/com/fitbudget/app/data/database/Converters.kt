package com.fitbudget.app.data.database

import androidx.room.TypeConverter
import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.FoodRole
import com.fitbudget.app.domain.model.Gender
import com.fitbudget.app.domain.model.MealType
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.domain.model.WorkoutCategory

/**
 * Enums are stored as their stable `name` strings. Unknown values decode to a sane default so a
 * future downgrade or a hand-edited database can never crash the app.
 */
class Converters {

    @TypeConverter fun genderToString(value: Gender): String = value.name
    @TypeConverter fun stringToGender(value: String): Gender = Gender.fromName(value)

    @TypeConverter fun activityToString(value: ActivityLevel): String = value.name
    @TypeConverter fun stringToActivity(value: String): ActivityLevel = ActivityLevel.fromName(value)

    @TypeConverter fun dietToString(value: DietPreference): String = value.name
    @TypeConverter fun stringToDiet(value: String): DietPreference = DietPreference.fromName(value)

    @TypeConverter fun foodCategoryToString(value: FoodCategory): String = value.name
    @TypeConverter fun stringToFoodCategory(value: String): FoodCategory = FoodCategory.fromName(value)

    @TypeConverter fun foodRoleToString(value: FoodRole): String = value.name
    @TypeConverter fun stringToFoodRole(value: String): FoodRole = FoodRole.fromName(value)

    @TypeConverter fun mealTypeToString(value: MealType?): String? = value?.name
    @TypeConverter fun stringToMealType(value: String?): MealType? =
        value?.let { raw -> MealType.entries.firstOrNull { it.name == raw } }

    @TypeConverter fun reminderTypeToString(value: ReminderType): String = value.name
    @TypeConverter fun stringToReminderType(value: String): ReminderType = ReminderType.fromName(value)

    @TypeConverter fun workoutCategoryToString(value: WorkoutCategory): String = value.name
    @TypeConverter fun stringToWorkoutCategory(value: String): WorkoutCategory =
        WorkoutCategory.fromName(value)
}
