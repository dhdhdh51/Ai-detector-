package com.fitbudget.app.domain

import com.fitbudget.app.domain.model.ActivityLevel
import com.fitbudget.app.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HealthCalculatorTest {

    // ---------------------------------------------------------------- age

    @Test
    fun `age is computed from completed years`() {
        val dob = LocalDate.of(2006, 11, 22)
        assertEquals(18, HealthCalculator.age(dob, LocalDate.of(2025, 9, 22)))
        assertEquals(19, HealthCalculator.age(dob, LocalDate.of(2025, 11, 22)))
        assertEquals(18, HealthCalculator.age(dob, LocalDate.of(2025, 11, 21)))
    }

    @Test
    fun `age of a future birth date is zero rather than negative`() {
        val dob = LocalDate.of(2030, 1, 1)
        assertEquals(0, HealthCalculator.age(dob, LocalDate.of(2025, 1, 1)))
    }

    // ---------------------------------------------------------------- BMI

    @Test
    fun `bmi uses weight over height in metres squared`() {
        // 85 kg at 172 cm -> 85 / 1.72^2 = 28.73
        assertEquals(28.73, HealthCalculator.bmi(85.0, 172.0), 0.01)
        assertEquals(22.86, HealthCalculator.bmi(70.0, 175.0), 0.01)
    }

    @Test
    fun `bmi returns zero for impossible input instead of throwing`() {
        assertEquals(0.0, HealthCalculator.bmi(85.0, 0.0), 0.0)
        assertEquals(0.0, HealthCalculator.bmi(85.0, -10.0), 0.0)
        assertEquals(0.0, HealthCalculator.bmi(0.0, 172.0), 0.0)
    }

    @Test
    fun `bmi categories match the standard bands`() {
        assertEquals("Underweight", HealthCalculator.bmiCategory(17.0))
        assertEquals("Healthy range", HealthCalculator.bmiCategory(22.0))
        assertEquals("Overweight", HealthCalculator.bmiCategory(27.0))
        assertEquals("Obese range", HealthCalculator.bmiCategory(31.0))
        assertEquals("—", HealthCalculator.bmiCategory(0.0))
    }

    @Test
    fun `healthy weight range brackets the healthy bmi band`() {
        val range = HealthCalculator.healthyWeightRange(172.0)
        assertEquals(54.7, range.start, 0.1)
        assertEquals(73.6, range.endInclusive, 0.1)
    }

    // ---------------------------------------------------------------- progress

    @Test
    fun `weight difference is current minus target`() {
        assertEquals(10.0, HealthCalculator.weightDifference(85.0, 75.0), 0.001)
        assertEquals(-2.0, HealthCalculator.weightDifference(73.0, 75.0), 0.001)
    }

    @Test
    fun `progress percent follows the specified formula`() {
        // (85 - 80) / (85 - 75) * 100 = 50
        assertEquals(50.0, HealthCalculator.progressPercent(85.0, 80.0, 75.0), 0.001)
        assertEquals(0.0, HealthCalculator.progressPercent(85.0, 85.0, 75.0), 0.001)
        assertEquals(100.0, HealthCalculator.progressPercent(85.0, 75.0, 75.0), 0.001)
    }

    @Test
    fun `progress percent is clamped between zero and one hundred`() {
        assertEquals(0.0, HealthCalculator.progressPercent(85.0, 90.0, 75.0), 0.001)
        assertEquals(100.0, HealthCalculator.progressPercent(85.0, 70.0, 75.0), 0.001)
    }

    @Test
    fun `progress percent handles a start weight equal to the target`() {
        assertEquals(100.0, HealthCalculator.progressPercent(75.0, 75.0, 75.0), 0.001)
        assertEquals(0.0, HealthCalculator.progressPercent(75.0, 78.0, 75.0), 0.001)
    }

    // ---------------------------------------------------------------- energy

    @Test
    fun `bmr follows mifflin st jeor for men`() {
        // 10*85 + 6.25*172 - 5*18 + 5 = 1840
        assertEquals(1840.0, HealthCalculator.bmr(85.0, 172.0, 18, Gender.MALE), 0.5)
    }

    @Test
    fun `bmr follows mifflin st jeor for women`() {
        // 10*65 + 6.25*160 - 5*30 - 161 = 1339
        assertEquals(1339.0, HealthCalculator.bmr(65.0, 160.0, 30, Gender.FEMALE), 0.5)
    }

    @Test
    fun `bmr is zero for impossible body measurements`() {
        assertEquals(0.0, HealthCalculator.bmr(0.0, 172.0, 25, Gender.MALE), 0.0)
        assertEquals(0.0, HealthCalculator.bmr(80.0, 0.0, 25, Gender.MALE), 0.0)
    }

    @Test
    fun `tdee multiplies bmr by the activity factor`() {
        assertEquals(2530.0, HealthCalculator.tdee(1840.0, ActivityLevel.LIGHT), 0.5)
        assertEquals(2208.0, HealthCalculator.tdee(1840.0, ActivityLevel.SEDENTARY), 0.5)
    }

    @Test
    fun `calorie target applies a deficit when losing weight`() {
        val tdee = 2530.0
        val target = HealthCalculator.calorieTarget(tdee, Gender.MALE, losingWeight = true)
        assertTrue("target $target should be below maintenance", target < tdee)
        assertTrue("target $target should keep a sane floor", target >= 1500.0)
        // 20% of 2530 = 506 -> 2024 rounded to nearest 10
        assertEquals(2020.0, target, 0.1)
    }

    @Test
    fun `calorie target equals maintenance when not losing weight`() {
        assertEquals(
            2530.0,
            HealthCalculator.calorieTarget(2530.0, Gender.MALE, losingWeight = false),
            0.1
        )
    }

    @Test
    fun `calorie target never drops below the safety floor`() {
        val target = HealthCalculator.calorieTarget(1400.0, Gender.FEMALE, losingWeight = true)
        assertTrue("target $target respects the 1200 kcal floor", target >= 1200.0)
    }

    @Test
    fun `deficit range is a sane ascending band`() {
        val range = HealthCalculator.deficitRange(2530.0)
        assertTrue(range.first <= range.last)
        assertTrue(range.first >= 200)
        assertTrue(range.last <= 750)
        assertEquals(0..0, HealthCalculator.deficitRange(0.0))
    }

    @Test
    fun `weekly weight change uses 7700 kcal per kilogram`() {
        // 500 kcal deficit * 7 days / 7700 = 0.4545 kg
        assertEquals(0.4545, HealthCalculator.weeklyWeightChangeKg(2500.0, 2000.0), 0.001)
        assertEquals(0.0, HealthCalculator.weeklyWeightChangeKg(2500.0, 2500.0), 0.001)
        assertTrue(HealthCalculator.weeklyWeightChangeKg(2000.0, 2500.0) < 0)
    }

    @Test
    fun `weeks to target is null when there is nothing to lose`() {
        assertNull(HealthCalculator.weeksToTarget(75.0, 75.0, 0.5))
        assertNull(HealthCalculator.weeksToTarget(85.0, 75.0, 0.0))
    }

    @Test
    fun `weeks to target rounds up`() {
        assertEquals(20, HealthCalculator.weeksToTarget(85.0, 75.0, 0.5))
        assertEquals(21, HealthCalculator.weeksToTarget(85.0, 75.0, 0.49))
    }

    @Test
    fun `protein target scales with body weight`() {
        assertEquals(120.0, HealthCalculator.proteinTargetGrams(85.0), 0.1)
        assertEquals(0.0, HealthCalculator.proteinTargetGrams(0.0), 0.1)
    }

    @Test
    fun `suggested water stays inside the supported range`() {
        assertEquals(3000, HealthCalculator.suggestedWaterMl(85.0))
        assertEquals(2000, HealthCalculator.suggestedWaterMl(40.0))
        assertEquals(4000, HealthCalculator.suggestedWaterMl(200.0))
        assertEquals(2500, HealthCalculator.suggestedWaterMl(0.0))
    }

    @Test
    fun `average weekly change scales the difference over the period`() {
        // -2 kg over 28 days = -0.5 kg per week
        assertEquals(-0.5, HealthCalculator.averageWeeklyChangeKg(85.0, 83.0, 28), 0.001)
        assertEquals(0.0, HealthCalculator.averageWeeklyChangeKg(85.0, 83.0, 0), 0.001)
    }
}
