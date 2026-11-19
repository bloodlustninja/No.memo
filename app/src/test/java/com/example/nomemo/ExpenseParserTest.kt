package com.example.nomemo

import com.example.nomemo.ledger.ExpenseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseParserTest {

    @Test
    fun testFormatsParsing() {
        val noteText = """
            Fuel 450
            Coffee -180
            ₹1,200 groceries
            +5000 split
            Dinner 350 #food #party
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(5, analysis.entries.size)
        // Fuel (450) + groceries (1200) + split (5000) + Dinner (350) - Coffee (180):
        // 7000 - 180 = 6820
        assertEquals(6820.0, analysis.totalExpense, 0.001)
        assertEquals("₹", analysis.currencySymbol)
    }

    @Test
    fun testNegativeAmountsDeductCorrectly() {
        val noteText = """
            Fuel 450
            Coffee -180
            Groceries 1000
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(3, analysis.entries.size)
        // 450 - 180 + 1000 = 1270
        assertEquals(1270.0, analysis.totalExpense, 0.001)
    }

    @Test
    fun testNegativeAmountsWithDifferentMinusFormats() {
        val noteText = """
            Dinner 500
            -180
            Discount -50
            Coffee - 50
            Lunch -₹100
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(5, analysis.entries.size)
        // 500 - 180 - 50 - 50 - 100 = 120
        assertEquals(120.0, analysis.totalExpense, 0.001)
    }

    @Test
    fun testInlineArithmeticCalculation() {
        val noteText = """
            500 - 180
            Dinner 1000 - 200
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(2, analysis.entries.size)
        // (500 - 180) = 320, (1000 - 200) = 800 -> 320 + 800 = 1120
        assertEquals(1120.0, analysis.totalExpense, 0.001)
    }

    @Test
    fun testAllNegativeEntries() {
        val noteText = """
            -180 coffee
            -50 tea
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(2, analysis.entries.size)
        // All negative expenses: total magnitude = 230
        assertEquals(230.0, analysis.totalExpense, 0.001)
    }

    @Test
    fun testNegativeWithCapBudget() {
        val noteText = """
            /cap 500
            Shopping 400
            Return -100
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(300.0, analysis.totalExpense, 0.001)
        assertEquals(200.0, analysis.remainingBudget ?: 0.0, 0.001)
        assertFalse(analysis.isCapExceededOrNearLimit)
    }

    @Test
    fun testDirectivesCapAndSplit() {
        val noteText = """
            /cap 5000
            /split 4
            Groceries 1200
            Wifi 800
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(5000.0, analysis.capAmount ?: 0.0, 0.001)
        assertEquals(4, analysis.splitCount ?: 0)
        assertEquals(2000.0, analysis.totalExpense, 0.001)
        assertEquals(500.0, analysis.perPersonSplit ?: 0.0, 0.001)
        assertEquals(3000.0, analysis.remainingBudget ?: 0.0, 0.001)
        assertFalse(analysis.isCapExceededOrNearLimit) // 2000 / 5000 = 40% < 85%
    }

    @Test
    fun testCapThresholdNearLimit() {
        val noteText = """
            /cap 1000
            Shopping 900
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode) // Activated due to /cap
        assertEquals(900.0, analysis.totalExpense, 0.001)
        assertTrue(analysis.isCapExceededOrNearLimit) // 900 / 1000 = 90% >= 85%
    }

    @Test
    fun testNormalNotesRemainUntouched() {
        // Less than 2 expense lines and no directive or tag
        val singleLineNote = """
            Call mom at 5
            Meeting tomorrow
        """.trimIndent()

        val analysis = ExpenseParser.parse(singleLineNote)
        assertFalse(analysis.isLedgerMode)
    }

    @Test
    fun testExplicitExpenseTagActivatesSingleLine() {
        val noteText = """
            #expense
            Hotel 2500
        """.trimIndent()

        val analysis = ExpenseParser.parse(noteText)
        assertTrue(analysis.isLedgerMode)
        assertEquals(2500.0, analysis.totalExpense, 0.001)
    }

    @Test
    fun testResetLedger() {
        val noteText = """
            Meeting notes:
            Fuel 450
            Coffee -180
            Don't forget to submit report
            /cap 5000
        """.trimIndent()

        val reset = ExpenseParser.resetLedger(noteText)
        assertTrue(reset.contains("Meeting notes:"))
        assertTrue(reset.contains("Don't forget to submit report"))
        assertFalse(reset.contains("Fuel 450"))
        assertFalse(reset.contains("/cap 5000"))
    }
}
