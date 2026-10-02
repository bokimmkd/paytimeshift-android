package com.paytimeshift.pts.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.math.BigDecimal

class ModelsTest {
    private fun shift(date:String="2026-10-01",start:String="07:00",end:String="15:00",pause:Int=0,rate:String="6",currency:String="EUR",fixed:Boolean=false) = Shift(jobId="factory",date=date,start=start,end=end,breakMinutes=pause,rate=rate,currency=currency,fixedPay=fixed)
    @Test fun overnightAndBreak() {
        val s=shift(start="23:00",end="07:00",pause=30)
        assertEquals(450L,s.paidMinutes)
        assertEquals(LocalDate.of(2026,10,2),s.finishes.toLocalDate())
        assertEquals(0,s.earnings().compareTo(BigDecimal("45")))
    }
    @Test fun fixedShiftDoesNotMultiplyHours() {assertEquals(0,shift(rate="55",fixed=true,pause=30).earnings().compareTo(BigDecimal("55")))}
    @Test fun currenciesAreNeverCombined() {
        val result=totals(listOf(shift(),shift(currency="MKD",rate="100")))
        assertEquals(setOf("EUR","MKD"),result.keys)
        assertEquals(0,result.getValue("EUR").compareTo(BigDecimal("48")))
        assertEquals(0,result.getValue("MKD").compareTo(BigDecimal("800")))
    }
    @Test fun currencyPrecision() {assertEquals("JPY 48",money(BigDecimal("48.2"),"JPY"));assertEquals("BHD 48.235",money(BigDecimal("48.2345"),"BHD"))}
    @Test fun overlapsAcrossMidnightAndNestedShifts() {
        val rows=listOf(shift(start="22:00",end="08:00"),shift(date="2026-10-02",start="01:00",end="02:00"),shift(date="2026-10-02",start="06:00",end="10:00"))
        assertEquals(2,warnings(rows,0).size)
        assertTrue(warnings(rows,0).all {it.startsWith("Overlapping")})
    }
    @Test fun shortGapThreshold() {val rows=listOf(shift(),shift(start="18:00",end="23:00"));assertEquals(1,warnings(rows,8).size);assertTrue(warnings(rows,3).isEmpty())}
    @Test fun monthlyPaydayClampsAndReturnsOriginalDayNextMonth() {
        val j=Job(name="Factory",paydayAnchor="2026-01-31")
        assertEquals(LocalDate.of(2026,2,28),nextPayday(j,LocalDate.of(2026,2,1)))
        assertEquals(LocalDate.of(2026,3,31),nextPayday(j,LocalDate.of(2026,3,1)))
    }
    @Test fun biweeklyUsesAnchorWithoutDrift() {
        val j=Job(name="Taxi",payCycle="Biweekly",paydayAnchor="2026-10-07")
        assertEquals(LocalDate.of(2026,10,21),nextPayday(j,LocalDate.of(2026,10,8)))
        assertEquals(LocalDate.of(2026,10,7),nextPayday(j,LocalDate.of(2026,10,7)))
    }
    @Test fun savedRateIndependentFromJobEdits() {val original=Job(name="Factory",rate="6");val s=shift(rate=original.rate);val edited=original.copy(rate="10");assertEquals("10",edited.rate);assertEquals(0,s.earnings().compareTo(BigDecimal("48")))}
    @Test fun currencyCatalogHasRegionalAndGlobalChoices() {val codes=currencyCatalog().map {it.currencyCode};assertTrue(codes.containsAll(listOf("MKD","EUR","USD","GBP","CHF","RSD","ALL","TRY","JPY","BHD","INR","CNY","AUD","CAD","BRL","ZAR")))}
}
