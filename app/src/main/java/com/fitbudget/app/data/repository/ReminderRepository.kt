package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.ReminderDao
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.model.ReminderType
import com.fitbudget.app.notifications.ReminderScheduler
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Single place that persists reminder settings and keeps the OS alarms in sync with them.
 * Every mutation writes to Room first and then re-arms the alarm, so a process death or reboot
 * can always rebuild the exact same schedule from the database.
 */
class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler
) {

    val reminders: Flow<List<ReminderEntity>> = reminderDao.observeAll()
        .map { list -> list.sortedBy { it.type.ordinal } }

    suspend fun get(type: ReminderType): ReminderEntity? = reminderDao.get(type)

    suspend fun getAll(): List<ReminderEntity> =
        reminderDao.getAll().sortedBy { it.type.ordinal }

    /** Installs the default reminder set on first launch, without touching existing rows. */
    suspend fun ensureDefaults() {
        reminderDao.insertAllIgnoringExisting(ReminderEntity.defaults())
    }

    suspend fun save(reminder: ReminderEntity) {
        reminderDao.upsert(reminder)
        rescheduleOne(reminder.type)
    }

    suspend fun setEnabled(type: ReminderType, enabled: Boolean) {
        val current = get(type) ?: ReminderEntity(type)
        save(current.copy(enabled = enabled))
    }

    suspend fun setTime(type: ReminderType, hour: Int, minute: Int) {
        val current = get(type) ?: ReminderEntity(type)
        save(current.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59)))
    }

    suspend fun setDays(type: ReminderType, daysMask: Int) {
        val current = get(type) ?: ReminderEntity(type)
        save(current.copy(daysMask = daysMask and DateTimeUtils.ALL_DAYS_MASK))
    }

    suspend fun setInterval(type: ReminderType, intervalMinutes: Int) {
        val current = get(type) ?: ReminderEntity(type)
        save(current.copy(intervalMinutes = intervalMinutes.coerceIn(15, 720)))
    }

    suspend fun setSound(type: ReminderType, enabled: Boolean) {
        val current = get(type) ?: ReminderEntity(type)
        save(current.copy(soundEnabled = enabled))
    }

    suspend fun setVibration(type: ReminderType, enabled: Boolean) {
        val current = get(type) ?: ReminderEntity(type)
        save(current.copy(vibrationEnabled = enabled))
    }

    suspend fun rescheduleOne(type: ReminderType) {
        val reminder = get(type) ?: return
        val profile = profileRepository.get()
        val settings = settingsRepository.current()
        if (!settings.remindersEnabled || !reminder.enabled) {
            scheduler.cancel(type)
            return
        }
        scheduler.schedule(
            reminder = reminder,
            wakeMinutes = profile?.wakeMinutes ?: DEFAULT_WAKE_MINUTES,
            sleepMinutes = profile?.sleepMinutes ?: DEFAULT_SLEEP_MINUTES
        )
    }

    /** Re-arms every reminder. Used on first launch, after reboot and by the daily worker. */
    suspend fun rescheduleAll() {
        ensureDefaults()
        val profile = profileRepository.get()
        val settings = settingsRepository.current()
        scheduler.scheduleAll(
            reminders = getAll(),
            wakeMinutes = profile?.wakeMinutes ?: DEFAULT_WAKE_MINUTES,
            sleepMinutes = profile?.sleepMinutes ?: DEFAULT_SLEEP_MINUTES,
            masterEnabled = settings.remindersEnabled
        )
    }

    suspend fun cancelAll() {
        scheduler.cancelAll()
    }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    /** Used by the reminders screen to show "next: tomorrow 8:00 AM". */
    suspend fun nextTrigger(type: ReminderType): LocalDateTime? {
        val reminder = get(type) ?: return null
        if (!reminder.enabled) return null
        val profile = profileRepository.get()
        return scheduler.nextTriggerDateTime(
            reminder = reminder,
            wakeMinutes = profile?.wakeMinutes ?: DEFAULT_WAKE_MINUTES,
            sleepMinutes = profile?.sleepMinutes ?: DEFAULT_SLEEP_MINUTES
        )
    }

    suspend fun clear() {
        scheduler.cancelAll()
        reminderDao.clear()
    }

    companion object {
        const val DEFAULT_WAKE_MINUTES = 6 * 60 + 30
        const val DEFAULT_SLEEP_MINUTES = 22 * 60 + 30
    }
}
