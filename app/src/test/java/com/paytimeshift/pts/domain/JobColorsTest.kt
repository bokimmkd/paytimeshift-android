package com.paytimeshift.pts.domain

import org.junit.Assert.*
import org.junit.Test

class JobColorsTest {
    @Test fun usedColorsAreReservedAndEditingKeepsItsOwnColor() {
        val jobs=listOf(Job(id="a",name="Factory"),Job(id="b",name="Wolt",color=0xFFFFA000L),
            Job(id="old",name="Archived",color=0xFFA040C5L,archived=true))
        assertFalse(jobColorAvailable(jobs,jobs[0].color))
        assertTrue(jobColorAvailable(jobs,jobs[0].color,"a"))
        assertFalse(jobColorAvailable(jobs,jobs[0].color,"b"))
        assertFalse(jobColorAvailable(jobs,jobs[2].color,"a"))
        assertEquals(0xFF00857CL,newJobColor(jobs))
    }
    @Test fun fullPaletteStillAllowsAnotherJobWithoutReusingAnyColor() {
        var jobs=emptyList<Job>()
        repeat(30) {index-> jobs=jobs+Job(id="$index",name="Job $index",color=newJobColor(jobs)) }
        assertEquals(30,jobs.map {it.color}.distinct().size)
        assertTrue(jobColorAvailable(jobs,newJobColor(jobs)))
        val imported=jobs+Job(id="custom",name="Custom",color=0xFF123456L)
        assertTrue(jobColorChoices(imported).contains(0xFF123456L))
        assertFalse(jobColorAvailable(imported,0xFF123456L))
    }
}
