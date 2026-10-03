package com.paytimeshift.pts.platform

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.paytimeshift.pts.domain.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.time.LocalTime

suspend fun recognizeRoster(context:Context,uri:Uri): String {
    val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    suspend fun read(image:InputImage):String = suspendCancellableCoroutine {c->
        recognizer.process(image).addOnSuccessListener {if(c.isActive) c.resume(it.text)}.addOnFailureListener {if(c.isActive) c.resumeWithException(it)}
    }
    try {
        if(context.contentResolver.getType(uri)=="application/pdf") {
            val fd=context.contentResolver.openFileDescriptor(uri,"r") ?: error("No file")
            PdfRenderer(fd).use {pdf->
                require(pdf.pageCount in 1..20) {"Choose a PDF with at most 20 pages"}
                val texts=mutableListOf<String>()
                for(i in 0 until pdf.pageCount) pdf.openPage(i).use {page->
                    val scale=1800f / maxOf(page.width,page.height)
                    val bitmap=Bitmap.createBitmap((page.width*scale).toInt().coerceAtLeast(1),(page.height*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
                    try {bitmap.eraseColor(android.graphics.Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);texts.add(read(InputImage.fromBitmap(bitmap,0)))} finally {bitmap.recycle()}
                }
                return texts.joinToString("\n")
            }
        }
        return read(InputImage.fromFilePath(context,uri))
    } finally {recognizer.close()}
}
fun parseRoster(text:String,job:Job,existing:List<Shift>): List<Shift> {
    val output=mutableListOf<Shift>()
    val datePattern=Regex("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[./]\\d{1,2}[./]\\d{4})\\b")
    val timePattern=Regex("\\b(\\d{1,2}:[0-5]\\d)\\s*[-–—]\\s*(\\d{1,2}:[0-5]\\d)\\b")
    for(line in text.lines()) try {
        val dateText=datePattern.find(line)?.value ?: continue
        val time=timePattern.find(line) ?: continue
        val date=if(dateText.contains('-')) LocalDate.parse(dateText) else dateText.split('.','/').let {LocalDate.of(it[2].toInt(),it[1].toInt(),it[0].toInt())}
        fun timeValue(v:String)=v.split(':').let {LocalTime.of(it[0].toInt(),it[1].toInt()).toString()}
        val start=timeValue(time.groupValues[1]);val end=timeValue(time.groupValues[2]);if(start==end) continue
        val shift=Shift(jobId=job.id,date=date.toString(),start=start,end=end,breakMinutes=job.defaultBreakMinutes,rate=job.rate,currency=job.currency,fixedPay=job.fixedPay,rules=job.hourlyRules(),paidBreak=job.paidBreak,monthlyPay=job.monthlySalaryOn(date)!=null)
        if(shift.breakMinutes>=java.time.Duration.between(shift.begins,shift.finishes).toMinutes()) continue
        if((existing+output).none {it.jobId==shift.jobId && it.date==shift.date && it.start==shift.start && it.end==shift.end}) output.add(shift)
    } catch(_:Exception) {continue}
    return output
}

