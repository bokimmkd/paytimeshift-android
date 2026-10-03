package com.paytimeshift.pts

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.platform.app.InstrumentationRegistry
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.platform.colleagueScheduleShareIntent
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Exercise the actual content provider attachment, not just a filename string. */
class ScheduleShareTest {
    @Test fun textAttachmentHasReadableUtf8PayloadAndImportsWithRecipientPrices() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val date=LocalDate.of(2026,10,1)
        val source=Job(id="source",name="Работа",rate="180",currency="MKD")
        val shift=source.templateShift(date,source.shiftTemplates().first()).copy(note="PRIVATE",bonus="123")
        val intent=colleagueScheduleShareIntent(context,source,AppData(jobs=listOf(source),shifts=listOf(shift)),date,date)
        assertEquals(Intent.ACTION_SEND,intent.action)
        assertEquals("text/plain",intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        @Suppress("DEPRECATION")
        val uri=intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content",uri.scheme)
        assertEquals(uri,intent.clipData!!.getItemAt(0).uri)
        assertEquals(intent.type,context.contentResolver.getType(uri))
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)!!.use {cursor->
            assertTrue(cursor.moveToFirst())
            assertEquals("PTS-schedule-2026-10-01-2026-10-01.txt",cursor.getString(0))
        }
        val text=context.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use {it.readText()}
        assertTrue(text.contains("Работа"))
        assertFalse(text.contains("PRIVATE"))
        assertFalse(text.contains("bonus"))
        assertFalse(text.contains("rate"))
        val recipient=Job(id="recipient",name="My job",rate="12",currency="EUR")
        val imported=decodeColleagueSchedule(text,recipient,emptyList()).single()
        assertEquals(recipient.id,imported.jobId)
        assertEquals("12",imported.rate)
        assertEquals("EUR",imported.currency)
        assertEquals(shift.date,imported.date)
        assertEquals(shift.start,imported.start)
        assertEquals(shift.end,imported.end)
        assertTrue(decodeColleagueSchedule(text,recipient,listOf(imported)).isEmpty())
    }
}
