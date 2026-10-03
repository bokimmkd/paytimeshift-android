package com.paytimeshift.pts

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.paytimeshift.pts.ui.languageNames
import com.paytimeshift.pts.ui.localeForLanguage
import com.paytimeshift.pts.ui.localizedPickerContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real platform dialogs: showing them must retain the Activity's window token. */
class PickerUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun datesAndTimesOpenSaveAndCancelInEveryAppLanguage() {
        languageNames.keys.forEach { language ->
            ui.activityRule.scenario.onActivity { activity ->
                val context = localizedPickerContext(activity, localeForLanguage(language))
                assertSame(activity.getSystemService(Context.WINDOW_SERVICE),
                    context.getSystemService(Context.WINDOW_SERVICE))
                assertEquals(localeForLanguage(language), context.resources.configuration.locales[0])
                var date = "2026-10-15"
                val calendar = DatePickerDialog(context,
                    { _, year, month, day -> date = "%04d-%02d-%02d".format(year, month + 1, day) },
                    2026, 9, 15)
                try {
                    calendar.show()
                    assertTrue(calendar.isShowing)
                    calendar.datePicker.updateDate(2026, 10, 2)
                    calendar.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
                    assertEquals("2026-11-02", date)
                } finally { calendar.dismiss() }
                val cancelDate = DatePickerDialog(context,
                    { _, _, _, _ -> fail("Cancelled date must not change data") }, 2026, 9, 15)
                try {
                    cancelDate.show()
                    cancelDate.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
                } finally { cancelDate.dismiss() }
                var time = "07:00"
                val clock = TimePickerDialog(context,
                    { _, hour, minute -> time = "%02d:%02d".format(hour, minute) }, 7, 0, true)
                try {
                    clock.show()
                    assertTrue(clock.isShowing)
                    val picker = clock.findViewById<TimePicker>(
                        activity.resources.getIdentifier("timePicker", "id", "android"))
                    assertNotNull(picker)
                    picker.hour = 1
                    picker.minute = 30
                    clock.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
                    assertEquals("01:30", time)
                } finally { clock.dismiss() }
                val cancelTime = TimePickerDialog(context,
                    { _, _, _ -> fail("Cancelled time must not change data") }, 7, 0, true)
                try {
                    cancelTime.show()
                    cancelTime.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
                } finally { cancelTime.dismiss() }
            }
        }
    }
}
