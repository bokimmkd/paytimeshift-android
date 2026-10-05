package com.paytimeshift.pts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.platform.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val today=LocalDate.of(2026,10,5)
    private fun data(language: String = "en", appearance: String = "Light"): AppData {
        val jobs=listOf(Job(id="a",name="Job 1"),Job(id="b",name="Wolt",color=0xFFFFA000))
        val shifts=listOf(
            Shift(id="one",jobId="a",date=today.toString(),start="07:00",end="15:00",breakMinutes=30,rate="10",currency="EUR"),
            Shift(id="two",jobId="b",date=today.toString(),start="18:00",end="21:00",rate="10",currency="EUR"),
            Shift(id="three",jobId="a",date=today.toString(),start="22:00",end="06:00",rate="10",currency="EUR"),
            Shift(id="tomorrow-a",jobId="a",date=today.plusDays(1).toString(),start="07:00",end="15:00",rate="10",currency="EUR"),
            Shift(id="tomorrow",jobId="b",date=today.plusDays(1).toString(),start="18:00",end="21:00",rate="10",currency="EUR"))
        return AppData(jobs=jobs,shifts=shifts,preferences=Preferences(language=language,appearance=appearance))
    }
    private fun texts(view: View): List<TextView> = if(view is TextView) listOf(view) else
        if(view is ViewGroup) (0 until view.childCount).flatMap {texts(view.getChildAt(it))} else emptyList()
    private fun renderAndCheck(context: android.content.Context, fixture: AppData, width: Int, height: Int,
        name: String, expectedToday: Int = 2, expectedTomorrow: Int = 2, hiddenToday: Int = 1) {
        val parent=FrameLayout(context)
        val view=widgetViews(context,fixture,height,today,width).apply(context,parent)
        val density=context.resources.displayMetrics.density
        val w=(width*density).toInt();val h=(height*density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
        view.layout(0,0,w,h)
        val lang=fixture.preferences.language
        for(index in 0..1) {
            val panelId=if(index==0) R.id.widget_today else R.id.widget_tomorrow
            val rowsId=if(index==0) R.id.widget_today_rows else R.id.widget_tomorrow_rows
            val headingId=if(index==0) R.id.widget_today_label else R.id.widget_tomorrow_label
            val moreId=if(index==0) R.id.widget_today_more else R.id.widget_tomorrow_more
            val expected=if(index==0) expectedToday else expectedTomorrow
            val heading=if(index==0) (if(lang=="mk") "ДЕНЕС" else "TODAY") else (if(lang=="mk") "УТРЕ" else "TOMORROW")
            val panel=view.findViewById<ViewGroup>(panelId)
            val rows=view.findViewById<ViewGroup>(rowsId)
            assertEquals(expected,rows.childCount)
            assertEquals(heading,view.findViewById<TextView>(headingId).text.toString())
            val more=view.findViewById<TextView>(moreId)
            assertEquals(if(panelId==R.id.widget_today && hiddenToday>0) View.VISIBLE else View.GONE,more.visibility)
            if(more.visibility==View.VISIBLE) assertEquals("+$hiddenToday",more.text.toString())
            texts(panel).filter {it.visibility==View.VISIBLE && it.text.isNotBlank()}.forEach { text ->
                val bounds=Rect(0,0,text.width,text.height);panel.offsetDescendantRectToMyCoords(text,bounds)
                assertTrue("$name clips ${text.text}: $bounds vs ${panel.width}×${panel.height}",
                    bounds.top>=0 && bounds.bottom<=panel.height && bounds.left>=0 && bounds.right<=panel.width)
                assertTrue("$name gives zero height to ${text.text}",text.height>0)
                if(text.id==R.id.widget_time && fixture.preferences.time24)
                    assertEquals("$name truncates ${text.text}",0,text.layout?.getEllipsisCount(0) ?: 0)
            }
        }
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            val file=File(context.getExternalFilesDir("screenshots"),"$name.png")
            file.parentFile!!.mkdirs();file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        } finally {bitmap.recycle()}
    }
    @Test fun twoEventsForBothDaysFitTheActualLauncherSizesInBothThemesAndLanguages() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        for(language in listOf("en","mk")) for(theme in listOf("Light","Dark"))
            for((width,height) in listOf(240 to 128,280 to 128,260 to 220,350 to 250,350 to 350)) {
                instrumentation.runOnMainSync {
                    renderAndCheck(context,data(language,theme),width,height,"widget-$language-$theme-$width-$height")
                }
            }
    }
    @Test fun ownerNonWorkingDaysAndBothJobsAreVisibleWithoutAMoreBadge() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val fixture=data("mk").copy(shifts=data().shifts.filter {it.id!="three"}.map {
            if(it.date==today.toString()) it.copy(kind="Non-working day") else it
        })
        instrumentation.runOnMainSync {
            renderAndCheck(context,fixture,350,250,"widget-owner-two-events",hiddenToday=0)
            val reference=data().copy(shifts=listOf(data().shifts[0],data().shifts.last()))
            renderAndCheck(context,reference,350,350,"widget-approved-reference",1,1,0)
        }
    }
    @Test fun largerFontStillShowsBothEventsAtTheMinimumAndDefaultSizes() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val config=android.content.res.Configuration(context.resources.configuration).apply {fontScale=1.3f}
        val larger=context.createConfigurationContext(config)
        instrumentation.runOnMainSync {
            renderAndCheck(larger,data("mk"),280,128,"widget-large-font-compact")
            renderAndCheck(larger,data("mk"),350,250,"widget-large-font-default")
        }
    }
    @Test fun emptyDaysOvernightAndTwelveHourTimesRenderWithoutDuplicateRows() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        instrumentation.runOnMainSync {
            val parent=FrameLayout(context)
            val night=data().shifts.first {it.id=="three"}
            val onlyNight=data().copy(shifts=listOf(night),preferences=Preferences(time24=false))
            val view=widgetViews(context,onlyNight,250,today).apply(context,parent)
            val labels=texts(view).map {it.text.toString()}
            assertTrue(labels.contains("10:00 PM–6:00 AM +1"))
            assertEquals(1,labels.count {it=="No shifts added"})
            val empty=widgetViews(context,onlyNight.copy(shifts=emptyList()),128,today).apply(context,parent)
            assertEquals(2,texts(empty).count {it.text.toString()=="No shifts added"})
        }
    }
    @Test fun widgetTargetsKeepTheCorrectDayAndShiftAndUseDistinctPendingIntentIdentities() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val shift=data().shifts.first()
        val first=widgetIntent(context,today,shift);val next=widgetIntent(context,today.plusDays(1))
        assertFalse(first.filterEquals(next))
        assertEquals(WidgetTarget(today.toString(),shift.id),WidgetTarget.fromIntent(first))
        assertEquals(WidgetTarget(today.plusDays(1).toString()),WidgetTarget.fromIntent(next))
        assertNull(WidgetTarget.fromIntent(ShortcutAction.Calendar.intent(context)))
        first.putExtra("widgetDate","bad-date");assertNull(WidgetTarget.fromIntent(first))
    }
    @Test fun warmWidgetDayTapOpensTheSelectedDateInCalendar() {
        val date=LocalDate.now().plusDays(1)
        val activity=ui.activity;val original=android.content.Intent(activity.intent)
        try {
            ui.activityRule.scenario.onActivity {it.startActivity(widgetIntent(it,date))}
            val prefix=date.format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.ENGLISH))
            ui.waitUntil(10000) {ui.onAllNodes(hasText(prefix,substring=true)).fetchSemanticsNodes(atLeastOneRootRequired=false).isNotEmpty()}
            ui.onNodeWithText("Plan shifts and mark holidays").assertIsDisplayed()
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {activity.intent=original}
        }
    }
}
