package com.paytimeshift.pts.data

import android.content.Context
import android.util.AtomicFile
import com.paytimeshift.pts.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Optional fields preserve all 0.1.0/0.1.1 data during install-over upgrades. */
class LocalStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "pts-data-v1.json"))
    fun load(): AppData {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return AppData()
        return decode(file.openRead().bufferedReader().use { it.readText() })
    }
    fun save(data: AppData) {
        val stream = file.startWrite()
        try { stream.write(encode(data).toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    companion object {
        fun decode(text: String): AppData {
            val root=JSONObject(text)
            require(root.getInt("schemaVersion")==1) {"Unsupported local data version"}
            val jobs=root.getJSONArray("jobs").objects().map {j->Job(
                id=j.getString("id"),name=j.getString("name"),color=j.getLong("color"),currency=j.getString("currency"),
                rate=j.getString("rate"),fixedPay=j.getBoolean("fixedPay"),payCycle=j.getString("payCycle"),paydayAnchor=j.getString("paydayAnchor"),
                rules=rules(j.optJSONObject("rules")),archived=j.optBoolean("archived",false),
                defaultStart=j.optString("defaultStart","07:00"),defaultEnd=j.optString("defaultEnd","15:00"),
                defaultBreakMinutes=j.optInt("defaultBreakMinutes",0),paidBreak=j.optBoolean("paidBreak",false),
                reminderMinutes=j.optInt("reminderMinutes",-1),minGapHours=j.optDouble("minGapHours",-1.0))}
            val shifts=root.getJSONArray("shifts").objects().map {s->Shift(
                id=s.getString("id"),jobId=s.getString("jobId"),date=s.getString("date"),start=s.getString("start"),end=s.getString("end"),
                breakMinutes=s.getInt("breakMinutes"),note=s.getString("note"),rate=s.getString("rate"),currency=s.getString("currency"),fixedPay=s.getBoolean("fixedPay"),
                rules=rules(s.optJSONObject("rules")),bonus=s.optString("bonus","0"),kind=s.optString("kind","Work"),paidBreak=s.optBoolean("paidBreak",false))}
            require(jobs.map {it.id}.distinct().size==jobs.size && shifts.map {it.id}.distinct().size==shifts.size)
            jobs.forEach {j->require(j.rate.toBigDecimal().signum()>=0);java.util.Currency.getInstance(j.currency);java.time.LocalDate.parse(j.paydayAnchor);java.time.LocalTime.parse(j.defaultStart);java.time.LocalTime.parse(j.defaultEnd);validateRules(j.rules)
                require(j.defaultBreakMinutes>=0 && j.reminderMinutes in -1..10080 && j.minGapHours.isFinite() && (j.minGapHours==-1.0 || j.minGapHours in 0.0..168.0))}
            shifts.forEach {s->require(jobs.any {it.id==s.jobId});java.util.Currency.getInstance(s.currency);require(s.rate.toBigDecimal().signum()>=0 && s.bonus.toBigDecimal().signum()>=0);require(s.breakMinutes>=0 && s.breakMinutes<java.time.Duration.between(s.begins,s.finishes).toMinutes());validateRules(s.rules)}
            val p=root.getJSONObject("preferences")
            val gap=p.optDouble("gapHours",8.0)
            require(gap.isFinite() && gap>=0 && gap<=168)
            val holidays=strings(root.optJSONArray("holidays"));holidays.forEach {java.time.LocalDate.parse(it)}
            return AppData(jobs,shifts,Preferences(p.getString("currency"),p.getBoolean("time24"),p.getBoolean("mondayFirst"),p.getString("appearance"),gap,p.optString("language","mk"),p.optInt("reminderMinutes",0)),holidays)
        }
        fun encode(data: AppData): String {
            val root=JSONObject().put("schemaVersion",1).put("holidays",JSONArray(data.holidays))
            root.put("jobs",JSONArray().apply {data.jobs.forEach {j->put(JSONObject().put("id",j.id).put("name",j.name).put("color",j.color).put("currency",j.currency).put("rate",j.rate).put("fixedPay",j.fixedPay).put("payCycle",j.payCycle).put("paydayAnchor",j.paydayAnchor).put("rules",ruleJson(j.rules)).put("archived",j.archived).put("defaultStart",j.defaultStart).put("defaultEnd",j.defaultEnd).put("defaultBreakMinutes",j.defaultBreakMinutes).put("paidBreak",j.paidBreak).put("reminderMinutes",j.reminderMinutes).put("minGapHours",j.minGapHours))}})
            root.put("shifts",JSONArray().apply {data.shifts.forEach {s->put(JSONObject().put("id",s.id).put("jobId",s.jobId).put("date",s.date).put("start",s.start).put("end",s.end).put("breakMinutes",s.breakMinutes).put("note",s.note).put("rate",s.rate).put("currency",s.currency).put("fixedPay",s.fixedPay).put("rules",ruleJson(s.rules)).put("bonus",s.bonus).put("kind",s.kind).put("paidBreak",s.paidBreak))}})
            val p=data.preferences
            root.put("preferences",JSONObject().put("currency",p.currency).put("time24",p.time24).put("mondayFirst",p.mondayFirst).put("appearance",p.appearance).put("gapHours",p.gapHours).put("language",p.language).put("reminderMinutes",p.reminderMinutes))
            return root.toString()
        }
        private fun rules(j: JSONObject?)=if(j==null) PayRules() else PayRules(j.optDouble("overtimeAfterHours",8.0),j.optDouble("overtimePercent",0.0),j.optDouble("nightPercent",0.0),j.optDouble("sundayPercent",0.0),j.optString("nightStart","22:00"),j.optString("nightEnd","06:00"),
            j.optBoolean("useHourlyRates",false),j.optString("overtimeRate",""),j.optString("saturdayRate",""),j.optString("sundayRate",""),j.optString("holidayRate",""),strings(j.optJSONArray("holidayDates")))
        private fun ruleJson(r: PayRules)=JSONObject().put("overtimeAfterHours",r.overtimeAfterHours).put("overtimePercent",r.overtimePercent).put("nightPercent",r.nightPercent).put("sundayPercent",r.sundayPercent).put("nightStart",r.nightStart).put("nightEnd",r.nightEnd)
            .put("useHourlyRates",r.useHourlyRates).put("overtimeRate",r.overtimeRate).put("saturdayRate",r.saturdayRate).put("sundayRate",r.sundayRate).put("holidayRate",r.holidayRate).put("holidayDates",JSONArray(r.holidayDates))
        private fun validateRules(r: PayRules) {
            require(listOf(r.overtimeAfterHours,r.overtimePercent,r.nightPercent,r.sundayPercent).all {it.isFinite() && it>=0})
            listOf(r.overtimeRate,r.saturdayRate,r.sundayRate,r.holidayRate).filter {it.isNotBlank()}.forEach {require(it.toBigDecimal().signum()>=0)}
            r.holidayDates.forEach {java.time.LocalDate.parse(it)}
            java.time.LocalTime.parse(r.nightStart);java.time.LocalTime.parse(r.nightEnd)
        }
        private fun strings(a:JSONArray?)=if(a==null) emptyList() else (0 until a.length()).map {a.getString(it)}
        private fun JSONArray.objects()=(0 until length()).map {getJSONObject(it)}
    }
}
