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

/** Multi-page A4 output. Text wraps rather than truncating job names or translations. */
fun createWorkReportPdf(context: Context,data: AppData,month: YearMonth,yearly: Boolean): File {
    val file=File(File(context.cacheDir,"shared").apply {mkdirs()},"PTS-${if(yearly) month.year.toString() else month.toString()}-work-report.pdf")
    val doc=PdfDocument();val language=data.preferences.language
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {typeface=Typeface.DEFAULT;textSize=11f;color=Color.rgb(7,28,67)}
    var page: PdfDocument.Page?=null;var y=0f;var number=0
    fun finish() {page?.let {p->paint.textSize=9f;paint.typeface=Typeface.DEFAULT;paint.color=Color.GRAY;p.canvas.drawText("PTS · Pay Time Shift  |  $number",36f,814f,paint);doc.finishPage(p)};page=null}
    fun start() {finish();number++;page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,number).create());page!!.canvas.drawColor(Color.WHITE)
        paint.textSize=18f;paint.typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);paint.color=Color.rgb(0,133,124)
        page!!.canvas.drawText("PTS · Pay Time Shift",36f,43f,paint);y=70f}
    fun write(value: String,heading: Boolean) {
        if(page==null || y>778f) start()
        paint.textSize=if(heading) 13f else 11f;paint.typeface=Typeface.create(Typeface.DEFAULT,if(heading) Typeface.BOLD else Typeface.NORMAL)
        paint.color=if(heading) Color.rgb(0,133,124) else Color.rgb(7,28,67)
        var rest=value
        do {
            if(y>778f) {start();paint.textSize=if(heading) 13f else 11f;paint.typeface=Typeface.create(Typeface.DEFAULT,if(heading) Typeface.BOLD else Typeface.NORMAL);paint.color=Color.rgb(7,28,67)}
            var count=paint.breakText(rest,true,523f,null).coerceAtLeast(1)
            if(count<rest.length) {val space=rest.lastIndexOf(' ',count-1);if(space>0) count=space}
            page!!.canvas.drawText(rest.take(count),36f,y,paint);y+=if(heading) 20f else 16f
            rest=rest.drop(count).trimStart()
        } while(rest.isNotEmpty())
        if(heading) y+=3f
    }
    try {
        reportLines(data,month,yearly).forEach {line->
            val label=if(line.literal) line.label else translate(line.label,language)
            write(label+if(line.value.isNotEmpty()) "  ·  ${line.value}" else "",line.heading)
        }
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
