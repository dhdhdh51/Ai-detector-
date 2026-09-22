package com.fitbudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterCalculatorTest {

    @Test
    fun `remaining water never goes negative`() {
        assertEquals(1000, WaterCalculator.snapshot(1500, 2500).remainingMl)
        assertEquals(0, WaterCalculator.snapshot(3000, 2500).remainingMl)
    }

    @Test
    fun `goal is met at or above the target`() {
        assertFalse(WaterCalculator.snapshot(2499, 2500).isGoalMet)
        assertTrue(WaterCalculator.snapshot(2500, 2500).isGoalMet)
        assertTrue(WaterCalculator.snapshot(4000, 2500).isGoalMet)
    }

    @Test
    fun `fraction and percent are clamped`() {
        assertEquals(0.6, WaterCalculator.snapshot(1500, 2500).fraction, 0.001)
        assertEquals(60, WaterCalculator.snapshot(1500, 2500).percent)
        assertEquals(1.0, WaterCalculator.snapshot(5000, 2500).fraction, 0.001)
        assertEquals(100, WaterCalculator.snapshot(5000, 2500).percent)
    }

    @Test
    fun `a zero target cannot divide by zero`() {
        val snapshot = WaterCalculator.snapshot(1000, 0)
        assertEquals(0.0, snapshot.fraction, 0.001)
        assertFalse(snapshot.isGoalMet)
    }

    @Test
    fun `negative intake is clamped to zero`() {
        assertEquals(0, WaterCalculator.snapshot(-500, 2500).consumedMl)
    }

    @Test
    fun `glasses remaining rounds up to whole 250 ml glasses`() {
        assertEquals(4, WaterCalculator.snapshot(1500, 2500).glassesRemaining)
        assertEquals(5, WaterCalculator.snapshot(1400, 2500).glassesRemaining)
        assertEquals(0, WaterCalculator.snapshot(2500, 2500).glassesRemaining)
    }

    @Test
    fun `total sums only positive entries`() {
        assertEquals(1750, WaterCalculator.total(listOf(250, 500, 1000)))
        assertEquals(750, WaterCalculator.total(listOf(250, -500, 500)))
        assertEquals(0, WaterCalculator.total(emptyList()))
    }

    @Test
    fun `average is zero for an empty history`() {
        assertEquals(0, WaterCalculator.averageMl(emptyList()))
        assertEquals(2000, WaterCalculator.averageMl(listOf(1500, 2500)))
    }

    @Test
    fun `adherence is the share of days that met the target`() {
        val totals = listOf(2500, 2600, 1000, 2500)
        assertEquals(0.75, WaterCalculator.adherenceFraction(totals, 2500), 0.001)
        assertEquals(0.0, WaterCalculator.adherenceFraction(totals, 0), 0.001)
        assertEquals(0.0, WaterCalculator.adherenceFraction(emptyList(), 2500), 0.001)
    }

    @Test
    fun `quick amounts match the specified buttons`() {
        assertEquals(listOf(250, 500, 750, 1000), WaterCalculator.QUICK_AMOUNTS_ML)
    }
}
