package com.fitbudget.app.domain.model

/** Biological sex is only used for the BMR estimate formula. */
enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other");

    companion object {
        fun fromName(value: String?): Gender = entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class ActivityLevel(
    val label: String,
    val description: String,
    val factor: Double
) {
    SEDENTARY("Sedentary", "Desk job, very little movement", 1.2),
    LIGHT("Lightly active", "Light walking, 1–3 days a week", 1.375),
    MODERATE("Moderately active", "Exercise 3–5 days a week", 1.55),
    VERY_ACTIVE("Very active", "Hard exercise 6–7 days a week", 1.725),
    EXTRA_ACTIVE("Extra active", "Physical job or twice-daily training", 1.9);

    companion object {
        fun fromName(value: String?): ActivityLevel = entries.firstOrNull { it.name == value } ?: LIGHT
    }
}

enum class DietPreference(val label: String) {
    VEGETARIAN("Vegetarian"),
    EGGETARIAN("Eggetarian"),
    NON_VEGETARIAN("Non-vegetarian");

    companion object {
        fun fromName(value: String?): DietPreference = entries.firstOrNull { it.name == value } ?: VEGETARIAN
    }
}

enum class FoodCategory(val label: String) {
    VEG("Veg"),
    EGG("Egg"),
    NON_VEG("Non-veg");

    fun allowedFor(preference: DietPreference): Boolean = when (preference) {
        DietPreference.VEGETARIAN -> this == VEG
        DietPreference.EGGETARIAN -> this == VEG || this == EGG
        DietPreference.NON_VEGETARIAN -> true
    }

    companion object {
        fun fromName(value: String?): FoodCategory = entries.firstOrNull { it.name == value } ?: VEG
    }
}

/** Broad role of a food inside a generated plan, used to keep meals sensible. */
enum class FoodRole {
    PROTEIN,
    STAPLE,
    VEGETABLE,
    FRUIT,
    DAIRY,
    SNACK,
    BEVERAGE;

    companion object {
        fun fromName(value: String?): FoodRole = entries.firstOrNull { it.name == value } ?: SNACK
    }
}

enum class MealType(
    val label: String,
    val defaultHour: Int,
    val defaultMinute: Int,
    /** Share of the daily calorie target this meal should roughly cover. */
    val calorieShare: Double
) {
    BREAKFAST("Breakfast", 8, 0, 0.28),
    LUNCH("Lunch", 13, 0, 0.32),
    SNACK("Evening Snack", 17, 0, 0.12),
    DINNER("Dinner", 20, 0, 0.28);

    companion object {
        fun fromName(value: String?): MealType = entries.firstOrNull { it.name == value } ?: BREAKFAST
    }
}

enum class ReminderType(
    val label: String,
    val notificationTitle: String,
    val notificationMessage: String,
    val defaultHour: Int,
    val defaultMinute: Int,
    val channelId: String,
    /** In-app route the notification opens. */
    val route: String,
    /** Only used by interval reminders such as water. */
    val defaultIntervalMinutes: Int = 0
) {
    BREAKFAST(
        "Breakfast", "🍳 Breakfast time",
        "Stay on track with your FitBudget plan.", 8, 0,
        NotificationChannels.MEALS, "diet"
    ),
    LUNCH(
        "Lunch", "🍛 Lunch time",
        "Eat your planned lunch and log it.", 13, 0,
        NotificationChannels.MEALS, "diet"
    ),
    SNACK(
        "Evening snack", "🥜 Snack time",
        "A light, high-protein snack keeps you full.", 17, 0,
        NotificationChannels.MEALS, "diet"
    ),
    DINNER(
        "Dinner", "🍽️ Dinner time",
        "Finish the day inside your budget.", 20, 0,
        NotificationChannels.MEALS, "diet"
    ),
    WATER(
        "Water", "💧 Water reminder",
        "Have a glass of water.", 9, 0,
        NotificationChannels.WATER, "water", defaultIntervalMinutes = 120
    ),
    WORKOUT(
        "Workout", "🏋️ Workout time",
        "30 minutes for yourself.", 18, 0,
        NotificationChannels.ACTIVITY, "workout"
    ),
    WALKING(
        "Walking", "🏃 Walk time",
        "A brisk walk now would hit your step goal.", 7, 0,
        NotificationChannels.ACTIVITY, "steps"
    ),
    WEIGHT(
        "Weight check", "⚖️ Weight check",
        "Today's measurement?", 7, 0,
        NotificationChannels.GENERAL, "weight"
    ),
    SLEEP(
        "Sleep", "😴 Wind down",
        "Sleep on time — recovery is part of the plan.", 22, 30,
        NotificationChannels.GENERAL, "home"
    );

    val isInterval: Boolean get() = defaultIntervalMinutes > 0

    val mealType: MealType?
        get() = when (this) {
            BREAKFAST -> MealType.BREAKFAST
            LUNCH -> MealType.LUNCH
            SNACK -> MealType.SNACK
            DINNER -> MealType.DINNER
            else -> null
        }

    companion object {
        fun fromName(value: String?): ReminderType = entries.firstOrNull { it.name == value } ?: BREAKFAST
    }
}

/** Notification channel ids, kept next to [ReminderType] so they cannot drift apart. */
object NotificationChannels {
    const val MEALS = "fitbudget_meals"
    const val WATER = "fitbudget_water"
    const val ACTIVITY = "fitbudget_activity"
    const val GENERAL = "fitbudget_general"
}

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System default");

    companion object {
        fun fromName(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class UnitSystem(val label: String) {
    METRIC("Metric (kg, cm)"),
    IMPERIAL("Imperial (lb, ft/in)");

    companion object {
        fun fromName(value: String?): UnitSystem = entries.firstOrNull { it.name == value } ?: METRIC
    }
}

enum class WorkoutCategory(val label: String, val description: String) {
    FULL_BODY("Full Body", "Balanced session for the whole body"),
    HOME_WORKOUT("Home Workout", "No equipment, small space"),
    WALKING("Walking", "Low impact cardio"),
    STRENGTH("Strength", "Build and keep muscle while losing fat"),
    MOBILITY("Mobility", "Loosen up stiff joints");

    companion object {
        fun fromName(value: String?): WorkoutCategory = entries.firstOrNull { it.name == value } ?: FULL_BODY
    }
}

enum class StepSource { SENSOR, MANUAL, MIXED }
