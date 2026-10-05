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
            Shift(id="tomorrow",jobId="b",date=today.plusDays(1).toString(),start="18:00",end="21:00",rate="10",currency="EUR"))
        return AppData(jobs=jobs,shifts=shifts,preferences=Preferences(language=language,appearance=appearance))
    }
    private fun texts(view: View): List<TextView> = if(view is TextView) listOf(view) else
        if(view is ViewGroup) (0 until view.childCount).flatMap {texts(view.getChildAt(it))} else emptyList()
    @Test fun realRemoteViewsFitCompactExpandedAndTallSizesInBothThemesAndLanguages() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        for(language in listOf("en","mk")) for(theme in listOf("Light","Dark")) for(height in listOf(128,220,320)) {
            instrumentation.runOnMainSync {
                val parent=FrameLayout(context)
                val view=widgetViews(context,data(language,theme),height,today).apply(context,parent)
                val width=(if(height==128) 280 else 320)
                val density=context.resources.displayMetrics.density
                val w=(width*density).toInt();val h=(height*density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
                view.layout(0,0,w,h)
                val labels=texts(view).map {it.text.toString()}
                assertTrue(labels.contains(if(language=="en") "Today" else "Денес") || labels.any {it.startsWith(if(language=="en") "Today ·" else "Денес ·")})
                assertTrue(labels.contains(if(language=="en") "Tomorrow" else "Утре") || labels.any {it.startsWith(if(language=="en") "Tomorrow ·" else "Утре ·")})
                assertTrue(labels.contains("07:00–15:00"));assertTrue(labels.contains("18:00–21:00"))
                assertTrue(labels.any {it.startsWith(if(height==320) "+1" else "+2")})
                if(height>=220) assertTrue(labels.contains(if(language=="mk") "7.5 ч" else "7.5h"))
                for(id in listOf(R.id.widget_today,R.id.widget_tomorrow)) {
                    val panel=view.findViewById<ViewGroup>(id)
                    texts(panel).filter {it.visibility==View.VISIBLE && it.text.isNotBlank()}.forEach { text ->
                        val bounds=Rect(0,0,text.width,text.height);panel.offsetDescendantRectToMyCoords(text,bounds)
                        assertTrue("$language $theme $height clips ${text.text}",bounds.bottom<=panel.height)
                    }
                }
                val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val file=File(context.getExternalFilesDir("screenshots"),"widget-$language-$theme-$height.png")
                    file.parentFile!!.mkdirs();file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                } finally {bitmap.recycle()}
            }
        }
    }
    @Test fun emptyDaysOvernightAndTwelveHourTimesRenderWithoutDuplicateRows() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        instrumentation.runOnMainSync {
            val parent=FrameLayout(context)
            val night=data().shifts.first {it.id=="three"}
            val onlyNight=data().copy(shifts=listOf(night),preferences=Preferences(time24=false))
            val view=widgetViews(context,onlyNight,220,today).apply(context,parent)
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
