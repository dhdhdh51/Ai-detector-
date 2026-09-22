# FitBudget ProGuard/R8 rules

# Room generated code
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# WorkManager workers are instantiated reflectively
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Broadcast receivers referenced from the manifest / AlarmManager
-keep class com.fitbudget.app.notifications.** { *; }

# Kotlin metadata / coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Enum valueOf used by Room type converters
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
