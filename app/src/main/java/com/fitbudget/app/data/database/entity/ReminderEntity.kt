package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.util.DateTimeUtils

/** One configurable reminder. Exactly one row per [ReminderType]. */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val type: ReminderType,
    val enabled: Boolean = true,
    val hour: Int = type.defaultHour,
    val minute: Int = type.defaultMinute,
    /** Bit 0 = Monday … bit 6 = Sunday. */
    val daysMask: Int = DateTimeUtils.ALL_DAYS_MASK,
    /** Repeat interval for interval reminders (water). 0 for one-shot daily reminders. */
    val intervalMinutes: Int = type.defaultIntervalMinutes,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
) {
    val minutesOfDay: Int get() = DateTimeUtils.minutesOfDay(hour, minute)

    companion object {
        fun defaults(): List<ReminderEntity> = ReminderType.entries.map { ReminderEntity(type = it) }
    }
}
