package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.WorkoutDao
import com.fitbudget.app.data.database.entity.WorkoutSessionEntity
import com.fitbudget.app.data.seed.WorkoutLibrary
import com.fitbudget.app.data.seed.WorkoutTemplate
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val dayRepository: DayRepository
) {

    val history: Flow<List<WorkoutSessionEntity>> = workoutDao.observeAll()

    fun observeRecent(limit: Int = 10): Flow<List<WorkoutSessionEntity>> =
        workoutDao.observeRecent(limit)

    fun observeCountForDay(epochDay: Long): Flow<Int> = workoutDao.observeCountForDay(epochDay)

    fun templates(): List<WorkoutTemplate> = WorkoutLibrary.templates

    fun template(id: String): WorkoutTemplate? = WorkoutLibrary.byId(id)

    suspend fun getAll(): List<WorkoutSessionEntity> = workoutDao.getAll()

    /** Saves a finished session. Calories are a rough estimate scaled by how much was completed. */
    suspend fun saveSession(
        template: WorkoutTemplate,
        startedAtMillis: Long,
        finishedAtMillis: Long,
        durationSeconds: Int,
        completed: Int,
        skipped: Int,
        note: String? = null,
        epochDay: Long = DateTimeUtils.todayEpochDay()
    ): Long {
        dayRepository.ensureDay(epochDay)
        val total = template.exercises.size.coerceAtLeast(1)
        val fraction = (completed.toDouble() / total).coerceIn(0.0, 1.0)
        return workoutDao.insert(
            WorkoutSessionEntity(
                epochDay = epochDay,
                templateId = template.id,
                templateName = template.name,
                category = template.category,
                startedAtMillis = startedAtMillis,
                finishedAtMillis = finishedAtMillis,
                durationSeconds = durationSeconds,
                exercisesCompleted = completed,
                exercisesSkipped = skipped,
                exercisesTotal = template.exercises.size,
                estimatedCalories = (template.estimatedCalories * fraction).toInt(),
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun delete(session: WorkoutSessionEntity) = workoutDao.delete(session)

    suspend fun clear() = workoutDao.clear()
}
