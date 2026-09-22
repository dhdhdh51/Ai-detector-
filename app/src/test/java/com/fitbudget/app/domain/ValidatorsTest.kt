package com.fitbudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {

    @Test
    fun `height must be between 100 and 250 cm`() {
        assertTrue(Validators.height(100.0).isValid)
        assertTrue(Validators.height(172.0).isValid)
        assertTrue(Validators.height(250.0).isValid)
        assertFalse(Validators.height(99.9).isValid)
        assertFalse(Validators.height(251.0).isValid)
        assertFalse(Validators.height(0.0).isValid)
        assertFalse(Validators.height(-172.0).isValid)
        assertFalse(Validators.height(null).isValid)
    }

    @Test
    fun `weight must be between 30 and 300 kg`() {
        assertTrue(Validators.weight(30.0).isValid)
        assertTrue(Validators.weight(85.0).isValid)
        assertTrue(Validators.weight(300.0).isValid)
        assertFalse(Validators.weight(29.0).isValid)
        assertFalse(Validators.weight(301.0).isValid)
        assertFalse(Validators.weight(null).isValid)
    }

    @Test
    fun `target weight above current weight is rejected for a loss goal`() {
        assertFalse(Validators.targetWeight(90.0, 85.0).isValid)
        assertTrue(Validators.targetWeight(75.0, 85.0).isValid)
        assertTrue(Validators.targetWeight(85.0, 85.0).isValid)
    }

    @Test
    fun `an unrealistically large gap is rejected`() {
        assertFalse(Validators.targetWeight(60.0, 290.0).isValid)
    }

    @Test
    fun `target weight still honours the absolute bounds`() {
        assertFalse(Validators.targetWeight(20.0, 85.0).isValid)
        assertFalse(Validators.targetWeight(null, 85.0).isValid)
    }

    @Test
    fun `budget must be between 1 and 10000 rupees`() {
        assertTrue(Validators.budget(1.0).isValid)
        assertTrue(Validators.budget(100.0).isValid)
        assertTrue(Validators.budget(10_000.0).isValid)
        assertFalse(Validators.budget(0.0).isValid)
        assertFalse(Validators.budget(-100.0).isValid)
        assertFalse(Validators.budget(10_001.0).isValid)
        assertFalse(Validators.budget(null).isValid)
    }

    @Test
    fun `water target must be between 500 ml and 10 litres`() {
        assertTrue(Validators.waterTarget(500).isValid)
        assertTrue(Validators.waterTarget(2500).isValid)
        assertTrue(Validators.waterTarget(10_000).isValid)
        assertFalse(Validators.waterTarget(499).isValid)
        assertFalse(Validators.waterTarget(10_001).isValid)
        assertFalse(Validators.waterTarget(0).isValid)
    }

    @Test
    fun `steps must be between 0 and 100000`() {
        assertTrue(Validators.steps(0).isValid)
        assertTrue(Validators.steps(8_000).isValid)
        assertTrue(Validators.steps(100_000).isValid)
        assertFalse(Validators.steps(-1).isValid)
        assertFalse(Validators.steps(100_001).isValid)
    }

    @Test
    fun `date of birth must be in the past and within a sane age range`() {
        val today = LocalDate.of(2025, 9, 22)
        assertTrue(Validators.dateOfBirth(LocalDate.of(2006, 11, 22), today).isValid)
        assertFalse(Validators.dateOfBirth(LocalDate.of(2030, 1, 1), today).isValid)
        assertFalse(Validators.dateOfBirth(LocalDate.of(2020, 1, 1), today).isValid)
        assertFalse(Validators.dateOfBirth(LocalDate.of(1900, 1, 1), today).isValid)
        assertFalse(Validators.dateOfBirth(null, today).isValid)
    }

    @Test
    fun `invalid results always carry a friendly message`() {
        assertNotNull(Validators.height(10.0).message)
        assertNotNull(Validators.budget(-1.0).message)
        assertNotNull(Validators.steps(200_000).message)
        assertNull(Validators.height(172.0).message)
    }

    @Test
    fun `name must be present and reasonably short`() {
        assertTrue(Validators.name("Rahul").isValid)
        assertFalse(Validators.name("").isValid)
        assertFalse(Validators.name("   ").isValid)
        assertFalse(Validators.name("a".repeat(41)).isValid)
    }

    @Test
    fun `quantity is limited to a sensible serving range`() {
        assertTrue(Validators.quantity(1.0).isValid)
        assertTrue(Validators.quantity(0.25).isValid)
        assertFalse(Validators.quantity(0.1).isValid)
        assertFalse(Validators.quantity(25.0).isValid)
    }

    @Test
    fun `reminder interval is limited to 15 minutes through 12 hours`() {
        assertTrue(Validators.reminderInterval(120).isValid)
        assertFalse(Validators.reminderInterval(5).isValid)
        assertFalse(Validators.reminderInterval(1_000).isValid)
    }

    @Test
    fun `reminder time rejects impossible clock values`() {
        assertTrue(Validators.reminderTime(8, 0).isValid)
        assertFalse(Validators.reminderTime(24, 0).isValid)
        assertFalse(Validators.reminderTime(8, 60).isValid)
        assertFalse(Validators.reminderTime(null, 0).isValid)
    }

    @Test
    fun `parsing tolerates rupee signs commas and spaces`() {
        assertEquals(100.0, Validators.parseDecimal("₹100")!!, 0.001)
        assertEquals(1500.0, Validators.parseDecimal("1,500")!!, 0.001)
        assertEquals(82.5, Validators.parseDecimal(" 82.5 ")!!, 0.001)
        assertNull(Validators.parseDecimal("abc"))
        assertNull(Validators.parseDecimal(""))
    }

    @Test
    fun `parseInt falls back to truncating a decimal`() {
        assertEquals(2500, Validators.parseInt("2500"))
        assertEquals(2500, Validators.parseInt("2,500"))
        assertEquals(2500, Validators.parseInt("2500.9"))
        assertNull(Validators.parseInt("nope"))
    }
}
