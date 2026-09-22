package com.fitbudget.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.fitbudget.app.di.AppContainer
import com.fitbudget.app.domain.model.MealType
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles the inline notification buttons ("Mark eaten", "+250 ml"). */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        scope.launch {
            try {
                val container = AppContainer.from(appContext)
                when {
                    intent.action?.startsWith(ACTION_ADD_WATER) == true -> {
                        val amount = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
                        container.waterRepository.add(amount, DateTimeUtils.todayEpochDay())
                        NotificationManagerCompat.from(appContext)
                            .cancel(NotificationHelper.notificationId(ReminderType.WATER))
                    }

                    intent.action?.startsWith(ACTION_COMPLETE_MEAL) == true -> {
                        val mealName = intent.getStringExtra(EXTRA_MEAL_TYPE)
                        val mealType = MealType.fromName(mealName)
                        val today = DateTimeUtils.todayEpochDay()
                        container.dietRepository.ensurePlanForDay(today)
                        container.dietRepository.setMealCompleted(today, mealType, true)
                        val reminderType = ReminderType.entries
                            .firstOrNull { it.mealType == mealType }
                        reminderType?.let {
                            NotificationManagerCompat.from(appContext)
                                .cancel(NotificationHelper.notificationId(it))
                        }
                    }
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Notification action failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationAction"
        const val ACTION_ADD_WATER = "com.fitbudget.app.action.ADD_WATER"
        const val ACTION_COMPLETE_MEAL = "com.fitbudget.app.action.COMPLETE_MEAL"
        const val EXTRA_AMOUNT_ML = "com.fitbudget.app.extra.AMOUNT_ML"
        const val EXTRA_MEAL_TYPE = "com.fitbudget.app.extra.MEAL_TYPE"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun addWaterIntent(context: Context, amountMl: Int): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "$ACTION_ADD_WATER.$amountMl"
                putExtra(EXTRA_AMOUNT_ML, amountMl)
            }
            return PendingIntent.getBroadcast(
                context,
                9000 + amountMl,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun completeMealIntent(context: Context, mealType: MealType): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "$ACTION_COMPLETE_MEAL.${mealType.name}"
                putExtra(EXTRA_MEAL_TYPE, mealType.name)
            }
            return PendingIntent.getBroadcast(
                context,
                9500 + mealType.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
