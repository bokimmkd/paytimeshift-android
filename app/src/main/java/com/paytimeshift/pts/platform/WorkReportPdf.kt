package com.paytimeshift.pts.platform

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import androidx.core.content.FileProvider
import com.paytimeshift.pts.domain.*
import com.paytimeshift.pts.ui.translate
import java.io.File
import java.time.YearMonth
import java.time.LocalDate

/** Branded A4 tables, repeated headers and full wrapped cell content. */
fun createWorkReportPdf(context: Context,data: AppData,month: YearMonth,yearly: Boolean): File {
    val file=File(File(context.cacheDir,"shared").apply {mkdirs()},"PTS-${if(yearly) month.year.toString() else month.toString()}-work-report.pdf")
    val doc=PdfDocument();val language=data.preferences.language
    val navy=Color.rgb(7,28,67);val teal=Color.rgb(0,133,124)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG);var page:PdfDocument.Page?=null;var y=0f;var number=0
    var tableHeaders=listOf<String>();var widths=listOf<Float>()
    fun text(value:String,x:Float,baseline:Float,size:Float=10f,bold:Boolean=false,color:Int=navy) {
        paint.textSize=size;paint.color=color;paint.typeface=Typeface.create(Typeface.DEFAULT,if(bold) Typeface.BOLD else Typeface.NORMAL)
        page!!.canvas.drawText(value,x,baseline,paint)
    }
    fun wrap(value:String,width:Float):List<String> {
        paint.textSize=9f;paint.typeface=Typeface.DEFAULT
        var rest=value;val lines=mutableListOf<String>()
        do {var n=paint.breakText(rest,true,width-12f,null).coerceAtLeast(1);if(n<rest.length){val space=rest.lastIndexOf(' ',n-1);if(space>0)n=space};lines+=rest.take(n);rest=rest.drop(n).trimStart()} while(rest.isNotEmpty())
        return lines
    }
    fun finish() {page?.let {text("PTS · Pay Time Shift",36f,815f,9f,color=teal);text(number.toString(),550f,815f,9f);doc.finishPage(it)};page=null}
    fun header() {if(tableHeaders.isEmpty())return;paint.color=teal;page!!.canvas.drawRect(36f,y,559f,y+28f,paint);var x=36f
        tableHeaders.forEachIndexed {i,v->wrap(v,widths[i]).take(2).forEachIndexed {line,t->text(t,x+6,y+11+line*10,9f,true,Color.WHITE)};x+=widths[i]};y+=28f}
    fun start() {finish();number++;page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,number).create());page!!.canvas.drawColor(Color.WHITE)
        paint.color=navy;page!!.canvas.drawRect(0f,0f,595f,82f,paint)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=2.5f;paint.color=Color.WHITE;page!!.canvas.drawCircle(54f,36f,17f,paint);page!!.canvas.drawLine(54f,36f,54f,24f,paint);page!!.canvas.drawLine(54f,36f,64f,36f,paint);paint.style=Paint.Style.FILL
        text("PTS",83f,40f,24f,true,Color.WHITE);text("Pay Time Shift",142f,39f,12f,color=Color.WHITE)
        text(if(yearly) month.year.toString() else month.toString(),36f,65f,11f,color=Color.WHITE);y=101f;header()}
    fun row(cells:List<String>) {
        val lines=cells.mapIndexed {i,v->wrap(v,widths[i])};val height=(lines.maxOf {it.size}*12f+12f).coerceAtLeast(28f)
        if(page==null || y+height>788f) start()
        paint.color=Color.rgb(239,247,247);page!!.canvas.drawRect(36f,y,559f,y+height,paint)
        var x=36f;lines.forEachIndexed {i,cell->cell.forEachIndexed {line,v->text(v,x+6,y+16+line*12)};x+=widths[i]}
        paint.color=Color.rgb(213,228,229);paint.strokeWidth=.6f;page!!.canvas.drawLine(36f,y+height,559f,y+height,paint);y+=height
    }
    fun section(title:String,headers:List<String>,sizes:List<Float>) {
        tableHeaders=emptyList();if(page==null || y>710f) start();y+=14f;text(title,36f,y,13f,true,teal);y+=12f
        tableHeaders=headers; widths=sizes;header()
    }
    try {
        start()
        val report=if(yearly) yearlyAnalytics(data,month.year) else monthlyAnalytics(data,month)
        section(translate(if(yearly) "Annual Work & Earnings Report" else "Monthly Work & Earnings Report",language),listOf(translate("Summary",language),translate("Amount",language)),listOf(290f,233f))
        reportLines(data,month,yearly).forEach {line->
            val label=if(line.literal)line.label else translate(line.label,language)
            if(line.heading) section(label,listOf(translate("Summary",language),translate("Amount",language)),listOf(290f,233f)) else row(listOf(label,line.value))
        }
        section(translate("Total shifts",language),listOf("Date","Job","Shift","Break","Paid hours","Earnings").map {translate(it,language)},listOf(72f,114f,91f,54f,65f,127f))
        data.shifts.filter {LocalDate.parse(it.date) in report.from..report.until}.sortedBy {it.begins}.forEach {shift->
            row(listOf(shift.date,data.jobs.find {it.id==shift.jobId}?.name ?: "",if(shift.kind=="Work") "${shift.start}–${shift.end}" else translate(shift.kind,language),shift.breakMinutes.toString(),hours(shift.paidMinutes),money(shift.earnings(),shift.currency)))
        }
        report.currencies.forEach {currency->currency.jobs.filter {it.details.isNotEmpty()}.forEach {job->
            section(job.name+" · "+translate("Cost items",language),listOf("Cost item","Frequency","Amount","Count","Total").map {translate(it,language)},listOf(170f,94f,91f,47f,121f))
            job.details.forEach {d->row(listOf(d.cost.name.ifBlank {translate(d.cost.category,language)},translate(d.cost.frequency,language),money(d.cost.amount.toBigDecimal(),currency.currency),d.count.toString(),money(d.total,currency.currency)))}
        }}
        finish();file.outputStream().use {doc.writeTo(it)}
    } finally {doc.close()}
    return file
}
fun openWorkReport(context: Context,file: File,action: String,language: String) {
    if(action=="Print") {
        (context.getSystemService(Context.PRINT_SERVICE) as PrintManager).print(file.nameWithoutExtension,object: PrintDocumentAdapter() {
            override fun onLayout(old: PrintAttributes?,new: PrintAttributes?,signal: CancellationSignal,callback: LayoutResultCallback,extras: android.os.Bundle?) {
                if(signal.isCanceled) callback.onLayoutCancelled() else callback.onLayoutFinished(PrintDocumentInfo.Builder(file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true)
            }
            override fun onWrite(pages: Array<out PageRange>,destination: ParcelFileDescriptor,signal: CancellationSignal,callback: WriteResultCallback) {
                try {if(signal.isCanceled) {callback.onWriteCancelled();return};file.inputStream().use {input->java.io.FileOutputStream(destination.fileDescriptor).use {input.copyTo(it)}};callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))}
                catch(e: Exception) {callback.onWriteFailed(translate("File could not be saved.",language))}
            }
        },PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());return
    }
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    val intent=if(action=="View") Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/pdf") else Intent(Intent.ACTION_SEND).apply {
        type=if(action=="Email") "message/rfc822" else "application/pdf"
        putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_SUBJECT,"PTS · Pay Time Shift — ${file.nameWithoutExtension}")
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.clipData=android.content.ClipData.newRawUri("PTS",uri)
    context.startActivity(Intent.createChooser(intent,translate(action,language)))
}
