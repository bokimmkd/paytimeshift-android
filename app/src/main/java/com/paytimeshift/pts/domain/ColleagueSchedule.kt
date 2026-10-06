package com.paytimeshift.pts.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Deliberately excludes job pay rules, earnings, costs, bonus and private notes. */
fun encodeColleagueSchedule(job: Job,rows: List<Shift>,from: LocalDate,until: LocalDate): String {
    require(until>=from)
    return JSONObject().put("format","PTS-schedule").put("version",1).put("job",job.name)
        .put("from",from.toString()).put("until",until.toString()).put("shifts",JSONArray().apply {
            rows.filter {it.jobId==job.id && LocalDate.parse(it.date) in from..until}.sortedBy {it.begins}.forEach {s->
                put(JSONObject().put("date",s.date).put("start",s.start).put("end",s.end).put("breakMinutes",s.breakMinutes).put("paidBreak",s.paidBreak).put("kind",s.kind))
            }
        }).toString(2)
}
fun decodeColleagueSchedule(text: String,job: Job,existing: List<Shift>): List<Shift> {
    require(text.length<=4*1024*1024)
    val root=JSONObject(text);require(root.getString("format")=="PTS-schedule" && root.getInt("version")==1)
    val list=root.getJSONArray("shifts");require(list.length()<=1100)
    val rows=(0 until list.length()).map {i->
        val s=list.getJSONObject(i)
        val t=ShiftTemplate(s.getString("start"),s.getString("end"),s.getInt("breakMinutes"),s.optBoolean("paidBreak",false),true)
        t.validate();val kind=s.optString("kind","Work");require(kind in dayKinds)
        job.templateShift(LocalDate.parse(s.getString("date")),t).copy(kind=kind)
    }
    return newScheduleRows(rows,existing)
}

