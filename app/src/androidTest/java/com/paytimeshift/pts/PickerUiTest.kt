package com.paytimeshift.pts

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.TimePicker
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.ui.languageNames
import com.paytimeshift.pts.ui.localeForLanguage
import com.paytimeshift.pts.ui.localizedPickerContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real platform dialogs: retain the Activity token and dispatch actual button clicks. */
class PickerUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun datesAndTimesOpenSaveAndCancelInEveryAppLanguage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        languageNames.keys.forEach { language ->
            var date = "2026-10-15"
            var time = "07:00"
            lateinit var calendar: DatePickerDialog
            lateinit var clock: TimePickerDialog
            ui.activityRule.scenario.onActivity { activity ->
                val context = localizedPickerContext(activity, localeForLanguage(language))
                assertSame(activity.getSystemService(Context.WINDOW_SERVICE),
                    context.getSystemService(Context.WINDOW_SERVICE))
                assertEquals(localeForLanguage(language), context.resources.configuration.locales[0])
                calendar = DatePickerDialog(context,
                    { _, y, m, d -> date = java.time.LocalDate.of(y, m + 1, d).toString() },
                    2026, 9, 15)
                calendar.show()
                assertTrue(calendar.isShowing)
                calendar.datePicker.updateDate(2026, 10, 2)
                calendar.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            }
            // AlertDialog button listeners dispatch through a Handler; let the main queue run.
            instrumentation.waitForIdleSync()
            assertEquals("Saved date in $language", "2026-11-02", date)
            ui.activityRule.scenario.onActivity { activity ->
                calendar.dismiss()
                val context = localizedPickerContext(activity, localeForLanguage(language))
                calendar = DatePickerDialog(context, { _, _, _, _ -> date = "cancelled" }, 2026, 9, 15)
                calendar.show()
                calendar.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertEquals("Cancelled date in $language", "2026-11-02", date)
            ui.activityRule.scenario.onActivity { activity ->
                calendar.dismiss()
                val context = localizedPickerContext(activity, localeForLanguage(language))
                clock = TimePickerDialog(context,
                    { _, h, m -> time = java.time.LocalTime.of(h, m).toString() }, 7, 0, true)
                clock.show()
                assertTrue(clock.isShowing)
                val picker = clock.findViewById<TimePicker>(
                    activity.resources.getIdentifier("timePicker", "id", "android"))
                assertNotNull(picker)
                picker.hour = 1
                picker.minute = 30
                clock.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertEquals("Saved time in $language", "01:30", time)
            ui.activityRule.scenario.onActivity { activity ->
                clock.dismiss()
                val context = localizedPickerContext(activity, localeForLanguage(language))
                clock = TimePickerDialog(context, { _, _, _ -> time = "cancelled" }, 7, 0, true)
                clock.show()
                clock.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertEquals("Cancelled time in $language", "01:30", time)
            ui.activityRule.scenario.onActivity { calendar.dismiss(); clock.dismiss() }
        }
    }
}
