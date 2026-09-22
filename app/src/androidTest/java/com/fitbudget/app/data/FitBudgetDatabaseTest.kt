package com.fitbudget.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitbudget.app.data.database.FitBudgetDatabase
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.data.database.entity.StepLogEntity
import com.fitbudget.app.data.database.entity.WeightLogEntity
import com.fitbudget.app.data.seed.FoodSeed
import com.fitbudget.app.domain.model.MealType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitBudgetDatabaseTest {

    private lateinit var database: FitBudgetDatabase

    @Before
    fun setUp() {
        database = FitBudgetDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun profileIsStoredAndReadBack() = runBlocking {
        val profile = ProfileEntity.default().copy(
            name = "Test User",
            heightCm = 172.0,
            currentWeightKg = 85.0,
            targetWeightKg = 75.0,
            dailyBudget = 100.0,
            onboardingComplete = true
        )
        database.profileDao().upsert(profile)

        val stored = database.profileDao().get()
        assertNotNull(stored)
        assertEquals("Test User", stored!!.name)
        assertEquals(85.0, stored.currentWeightKg, 0.001)
        assertTrue(stored.onboardingComplete)
        assertEquals(28.73, stored.bmi, 0.01)
    }

    @Test
    fun weightLogKeepsOnlyOneEntryPerDay() = runBlocking {
        val dao = database.weightDao()
        dao.upsert(WeightLogEntity(epochDay = 100, weightKg = 85.0))
        dao.upsert(WeightLogEntity(epochDay = 100, weightKg = 84.2))
        dao.upsert(WeightLogEntity(epochDay = 101, weightKg = 84.0))

        val all = dao.getAll()
        assertEquals(2, all.size)
        assertEquals(84.2, dao.getForDay(100)!!.weightKg, 0.001)
        assertEquals(84.0, dao.getLatest()!!.weightKg, 0.001)
        assertEquals(84.2, dao.getEarliest()!!.weightKg, 0.001)
    }

    @Test
    fun seededFoodsAreInsertedWithoutDuplicates() = runBlocking {
        val dao = database.foodDao()
        dao.insertAllIgnoringDuplicates(FoodSeed.foods(0L))
        val first = dao.count()
        assertTrue("seed should contain the required foods", first >= 19)

        // Re-seeding must not duplicate rows thanks to the unique name index.
        dao.insertAllIgnoringDuplicates(FoodSeed.foods(0L))
        assertEquals(first, dao.count())

        assertNotNull(dao.findByNameKey("boiled egg"))
        assertNotNull(dao.findByNameKey("roti (wheat)"))
        assertNull(dao.findByNameKey("not a real food"))
    }

    @Test
    fun mealAggregatesCountOnlyCompletedItems() = runBlocking {
        val dao = database.mealDao()
        dao.insertAll(
            listOf(
                mealEntry(day = 200, type = MealType.BREAKFAST, completed = true, cost = 21.0, kcal = 234.0),
                mealEntry(day = 200, type = MealType.BREAKFAST, completed = false, cost = 6.0, kcal = 220.0),
                mealEntry(day = 200, type = MealType.LUNCH, completed = true, cost = 12.0, kcal = 150.0)
            )
        )

        val aggregate = dao.aggregateRange(200, 200).single()
        assertEquals(3, aggregate.itemCount)
        assertEquals(2, aggregate.completedCount)
        assertEquals(39.0, aggregate.plannedCost, 0.001)
        assertEquals(33.0, aggregate.consumedCost, 0.001)
        assertEquals(604.0, aggregate.plannedCalories, 0.001)
        assertEquals(384.0, aggregate.consumedCalories, 0.001)

        val byMeal = dao.aggregateMealTypes(200, 200).associateBy { it.mealType }
        assertEquals(false, byMeal[MealType.BREAKFAST]!!.fullyCompleted)
        assertEquals(true, byMeal[MealType.LUNCH]!!.fullyCompleted)
    }

    @Test
    fun markingAWholeMealCompletesEveryItem() = runBlocking {
        val dao = database.mealDao()
        dao.insertAll(
            listOf(
                mealEntry(day = 300, type = MealType.DINNER, completed = false),
                mealEntry(day = 300, type = MealType.DINNER, completed = false)
            )
        )
        dao.setMealCompleted(300, MealType.DINNER, true, 12345L)

        val items = dao.getForDay(300)
        assertEquals(2, items.size)
        assertTrue(items.all { it.completed })
        assertTrue(items.all { it.completedAtMillis == 12345L })
    }

    @Test
    fun defaultRemindersAreInstalledOnce() = runBlocking {
        val dao = database.reminderDao()
        dao.insertAllIgnoringExisting(ReminderEntity.defaults())
        val installed = dao.getAll()
        assertEquals(9, installed.size)

        // Change one, re-run defaults, and confirm the user's value survived.
        dao.upsert(installed.first().copy(hour = 5, minute = 15))
        dao.insertAllIgnoringExisting(ReminderEntity.defaults())
        val reminder = dao.get(installed.first().type)!!
        assertEquals(5, reminder.hour)
        assertEquals(15, reminder.minute)
        assertEquals(9, dao.getAll().size)
    }

    @Test
    fun stepsFromSensorAndManualEntryAreKeptSeparate() = runBlocking {
        val dao = database.stepDao()
        dao.upsert(StepLogEntity(epochDay = 400, sensorSteps = 1200, manualSteps = 0))
        dao.addSensorSteps(400, 800, 1L)
        dao.setManualSteps(400, 500, 2L)

        val log = dao.getForDay(400)!!
        assertEquals(2000, log.sensorSteps)
        assertEquals(500, log.manualSteps)
        assertEquals(2500, log.totalSteps)
    }

    @Test
    fun waterTotalsAreSummedPerDay() = runBlocking {
        val dao = database.waterDao()
        dao.insert(com.fitbudget.app.data.database.entity.WaterLogEntity(epochDay = 500, amountMl = 250))
        dao.insert(com.fitbudget.app.data.database.entity.WaterLogEntity(epochDay = 500, amountMl = 500))
        dao.insert(com.fitbudget.app.data.database.entity.WaterLogEntity(epochDay = 501, amountMl = 1000))

        assertEquals(750, dao.totalForDay(500))
        assertEquals(1000, dao.totalForDay(501))
        assertEquals(0, dao.totalForDay(502))

        val totals = dao.totalsForRange(500, 501).associate { it.epochDay to it.value }
        assertEquals(750, totals[500])
        assertEquals(1000, totals[501])
    }

    private fun mealEntry(
        day: Long,
        type: MealType,
        completed: Boolean,
        cost: Double = 10.0,
        kcal: Double = 200.0
    ) = MealEntryEntity(
        epochDay = day,
        mealType = type,
        mealLabel = type.label,
        timeMinutes = type.defaultHour * 60,
        foodName = "Test food",
        servingLabel = "1 serving",
        quantity = 1.0,
        caloriesPerServing = kcal,
        proteinPerServing = 10.0,
        costPerServing = cost,
        completed = completed,
        completedAtMillis = if (completed) 1L else null
    )
}
