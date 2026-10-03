package com.paytimeshift.pts.data

import android.content.Context
import android.util.AtomicFile
import com.paytimeshift.pts.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Optional fields preserve all 0.1.0/0.1.1 data during install-over upgrades. */
class LocalStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "pts-data-v1.json"))
    fun load(): AppData = ioLock.withLock {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return AppData()
        return decode(file.openRead().bufferedReader().use { it.readText() })
    }
    fun save(data: AppData) = ioLock.withLock {
        val stream = file.startWrite()
        try { stream.write(encode(data).toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    companion object {
        private val ioLock = ReentrantLock()
        fun decode(text: String): AppData {
            val root=JSONObject(text)
            require(root.getInt("schemaVersion")==1) {"Unsupported local data version"}
            val jobs=root.getJSONArray("jobs").objects().map {j->Job(
                id=j.getString("id"),name=j.getString("name"),color=j.getLong("color"),currency=j.getString("currency"),
                rate=j.getString("rate"),fixedPay=j.getBoolean("fixedPay"),payCycle=j.getString("payCycle"),paydayAnchor=j.getString("paydayAnchor"),
                rules=rules(j.optJSONObject("rules")),archived=j.optBoolean("archived",false),
                defaultStart=j.optString("defaultStart","07:00"),defaultEnd=j.optString("defaultEnd","15:00"),
                defaultBreakMinutes=j.optInt("defaultBreakMinutes",0),paidBreak=j.optBoolean("paidBreak",false),
                reminderMinutes=j.optInt("reminderMinutes",-1),minGapHours=j.optDouble("minGapHours",-1.0), costs=costs(j.optJSONArray("costs")), monthlyPay=j.optBoolean("monthlyPay",false), salaryPeriods=j.optJSONArray("salaryPeriods")?.objects()?.map {SalaryPeriod(it.getString("from"),it.optString("until",""),it.getString("amount"),it.getString("currency"))} ?: emptyList(), extraShifts=j.optJSONArray("extraShifts")?.objects()?.map {t->ShiftTemplate(t.getString("start"),t.getString("end"),t.optInt("breakMinutes",0),t.optBoolean("paidBreak",false),t.optBoolean("active",false))} ?: emptyList())}
            val shifts=root.getJSONArray("shifts").objects().map {s->Shift(
                id=s.getString("id"),jobId=s.getString("jobId"),date=s.getString("date"),start=s.getString("start"),end=s.getString("end"),
                breakMinutes=s.getInt("breakMinutes"),note=s.getString("note"),rate=s.getString("rate"),currency=s.getString("currency"),fixedPay=s.getBoolean("fixedPay"),
                rules=rules(s.optJSONObject("rules")),bonus=s.optString("bonus","0"),kind=s.optString("kind","Work"),paidBreak=s.optBoolean("paidBreak",false),monthlyPay=s.optBoolean("monthlyPay",false))}
            require(jobs.map {it.id}.distinct().size==jobs.size && shifts.map {it.id}.distinct().size==shifts.size)
            jobs.forEach {j->require(j.rate.toBigDecimal().signum()>=0);java.util.Currency.getInstance(j.currency);java.time.LocalDate.parse(j.paydayAnchor);java.time.LocalTime.parse(j.defaultStart);java.time.LocalTime.parse(j.defaultEnd);validateRules(j.rules)
                require(!(j.monthlyPay && j.fixedPay) && j.salaryPeriods.size<=1000)
                val periods=j.salaryPeriods.sortedBy {it.from}
                periods.forEachIndexed {i,v->val start=java.time.LocalDate.parse(v.from);require(v.amount.length<=32 && v.amount.toBigDecimal().signum()>=0);java.util.Currency.getInstance(v.currency)
                    require(v.until.isBlank() || java.time.LocalDate.parse(v.until)>=start)
                    if(i>0) require(periods[i-1].until.isNotBlank() && java.time.LocalDate.parse(periods[i-1].until)<start)}
                require(j.costs.map {it.id}.distinct().size==j.costs.size && j.costs.size<=100)
                j.costs.forEach {c->require(c.id.isNotBlank() && c.category in costCategories && c.name.length<=120 && c.amount.length<=32 && c.amount.toBigDecimal().signum()>=0 && c.frequency in costFrequencies)}
                require(j.extraShifts.size<=2);j.extraShifts.forEach {it.validate()};require(j.defaultBreakMinutes>=0 && j.reminderMinutes in -1..10080 && j.minGapHours.isFinite() && (j.minGapHours==-1.0 || j.minGapHours in 0.0..168.0))}
            shifts.forEach {s->require(s.kind in dayKinds && !(s.monthlyPay && s.fixedPay));require(jobs.any {it.id==s.jobId});java.util.Currency.getInstance(s.currency);require(s.rate.toBigDecimal().signum()>=0 && s.bonus.toBigDecimal().signum()>=0);require(s.breakMinutes>=0 && s.breakMinutes<java.time.Duration.between(s.begins,s.finishes).toMinutes());validateRules(s.rules)}
            val p=root.getJSONObject("preferences")
            val gap=p.optDouble("gapHours",8.0)
            require(gap.isFinite() && gap>=0 && gap<=168)
            val holidays=strings(root.optJSONArray("holidays"));holidays.forEach {java.time.LocalDate.parse(it)}
            val adjustments=root.optJSONArray("adjustments")?.objects()?.map {MonthlyAdjustment(it.getString("id"),it.getString("jobId"),it.getString("month"),it.getString("type"),it.getString("amount"),it.getString("currency"),it.getString("reason"),it.optString("note",""))} ?: emptyList()
            require(adjustments.size<=10000 && adjustments.map {it.id}.distinct().size==adjustments.size)
            adjustments.forEach {a->require(a.id.isNotBlank() && jobs.any {it.id==a.jobId} && a.type in listOf("Bonus","Deduction") && a.amount.length<=32 && a.amount.toBigDecimal().signum()>0 && a.reason.isNotBlank() && a.reason.length<=120 && a.note.length<=1000);java.time.YearMonth.parse(a.month);java.util.Currency.getInstance(a.currency)}
            val hidden=strings(p.optJSONArray("hiddenHintIds"));require(hidden.size<=200 && hidden.all {it.length<=100})
            return AppData(jobs,shifts,Preferences(p.getString("currency"),p.getBoolean("time24"),p.getBoolean("mondayFirst"),p.getString("appearance"),gap,p.optString("language","en"),p.optInt("reminderMinutes",0),p.optBoolean("showHints",true),hidden),holidays,adjustments)
        }
        fun encode(data: AppData): String {
            val root=JSONObject().put("schemaVersion",1).put("holidays",JSONArray(data.holidays))
            root.put("jobs",JSONArray().apply {data.jobs.forEach {j->put(JSONObject().put("id",j.id).put("name",j.name).put("color",j.color).put("currency",j.currency).put("rate",j.rate).put("fixedPay",j.fixedPay).put("monthlyPay",j.monthlyPay).put("salaryPeriods",JSONArray().apply {j.salaryPeriods.forEach {v->put(JSONObject().put("from",v.from).put("until",v.until).put("amount",v.amount).put("currency",v.currency))}}).put("payCycle",j.payCycle).put("paydayAnchor",j.paydayAnchor).put("rules",ruleJson(j.rules)).put("archived",j.archived).put("defaultStart",j.defaultStart).put("defaultEnd",j.defaultEnd).put("defaultBreakMinutes",j.defaultBreakMinutes).put("paidBreak",j.paidBreak).put("reminderMinutes",j.reminderMinutes).put("minGapHours",j.minGapHours).put("extraShifts",JSONArray().apply {j.extraShifts.forEach {t->put(JSONObject().put("start",t.start).put("end",t.end).put("breakMinutes",t.breakMinutes).put("paidBreak",t.paidBreak).put("active",t.active))}}).put("costs",JSONArray().apply {j.costs.forEach {c->put(JSONObject().put("id",c.id).put("category",c.category).put("name",c.name).put("amount",c.amount).put("frequency",c.frequency).put("enabled",c.enabled))}}))}})
            root.put("shifts",JSONArray().apply {data.shifts.forEach {s->put(JSONObject().put("id",s.id).put("jobId",s.jobId).put("date",s.date).put("start",s.start).put("end",s.end).put("breakMinutes",s.breakMinutes).put("note",s.note).put("rate",s.rate).put("currency",s.currency).put("fixedPay",s.fixedPay).put("monthlyPay",s.monthlyPay).put("rules",ruleJson(s.rules)).put("bonus",s.bonus).put("kind",s.kind).put("paidBreak",s.paidBreak))}})
            root.put("adjustments",JSONArray().apply {data.adjustments.forEach {a->put(JSONObject().put("id",a.id).put("jobId",a.jobId).put("month",a.month).put("type",a.type).put("amount",a.amount).put("currency",a.currency).put("reason",a.reason).put("note",a.note))}})
            val p=data.preferences
            root.put("preferences",JSONObject().put("currency",p.currency).put("time24",p.time24).put("mondayFirst",p.mondayFirst).put("appearance",p.appearance).put("gapHours",p.gapHours).put("language",p.language).put("reminderMinutes",p.reminderMinutes).put("showHints",p.showHints).put("hiddenHintIds",JSONArray(p.hiddenHintIds)))
            return root.toString()
        }
        private fun costs(a: JSONArray?): List<JobCost> = if(a==null) emptyList() else a.objects().map {c->
            JobCost(id=c.getString("id"),category=c.getString("category"),name=c.optString("name",""),amount=c.getString("amount"),frequency=c.getString("frequency"),enabled=c.optBoolean("enabled",true))}
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

