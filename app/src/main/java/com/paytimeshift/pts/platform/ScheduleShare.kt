package com.paytimeshift.pts.platform

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.ui.translate
import java.io.File
import java.time.YearMonth

fun shareSchedule(context:Context,data:AppData,month:YearMonth,pdf:Boolean) {
    val locale=com.paytimeshift.pts.ui.localeForLanguage(data.preferences.language)
    val rows=data.shifts.filter {YearMonth.from(it.begins)==month}.sortedBy {it.begins}
    val lines=rows.map {s->"${s.date}  ${if(s.kind=="Work") "${s.start}–${s.end}" else translate(s.kind,data.preferences.language)}  ${data.jobs.find {it.id==s.jobId}?.name ?: ""}"}
    val title="PTS · ${month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",locale))}"
    val pages=lines.chunked(23).ifEmpty {listOf(emptyList())}
    val folder=File(context.cacheDir,"shared").apply {mkdirs()}
    val file=File(folder,"PTS-$month.${if(pdf) "pdf" else "png"}")
    fun draw(canvas:Canvas,page:List<String>,width:Int) {
        canvas.drawColor(Color.WHITE)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(7,28,67);textSize=24f;typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)}
        canvas.drawText(title,24f,42f,paint)
        paint.textSize=13f;paint.typeface=Typeface.DEFAULT
        if(page.isEmpty()) canvas.drawText(translate("No shifts added",data.preferences.language),24f,85f,paint)
        page.forEachIndexed {i,line->
            var shown=line
            while(paint.measureText(shown)>width-48 && shown.length>1) shown=shown.dropLast(1)
            if(shown!=line) shown=shown.dropLast(1)+"…"
            canvas.drawText(shown,24f,85f+i*28f,paint)
        }
    }
    if(pdf) {val doc=PdfDocument();try {pages.forEachIndexed {i,page->val p=doc.startPage(PdfDocument.PageInfo.Builder(595,842,i+1).create());draw(p.canvas,page,595);doc.finishPage(p)};file.outputStream().use {doc.writeTo(it)}} finally {doc.close()}}
    else {
        val image=Bitmap.createBitmap(800,(130+lines.size*28).coerceAtLeast(300),Bitmap.Config.ARGB_8888)
        try {draw(Canvas(image),lines,800);file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}} finally {image.recycle()}
    }
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type=if(pdf) "application/pdf" else "image/png";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);clipData=android.content.ClipData.newRawUri("PTS",uri)},translate("Share schedule",data.preferences.language)))
}

fun shareColleagueSchedule(context: Context,job: Job,data: AppData,from: java.time.LocalDate,until: java.time.LocalDate) {
    val file=File(File(context.cacheDir,"shared").apply {mkdirs()},"PTS-$from-$until.pts-schedule")
    file.writeText(encodeColleagueSchedule(job,data.shifts,from,until))
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type="application/json";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData=android.content.ClipData.newRawUri("PTS",uri)
    },translate("Export schedule for colleague",data.preferences.language)))
}
