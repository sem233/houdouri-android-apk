package com.houdouri.app.domain

import com.houdouri.app.data.ShiftEntity
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun exportToCsv(shifts: List<ShiftEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("id,startTime,endTime,timezoneId,note")
        for (s in shifts) {
            val escapedNote = (s.note ?: "").replace("\"", "\"\"")
            val endStr = s.endTime?.toString() ?: ""
            sb.appendLine("${s.id},${s.startTime},$endStr,${s.timezoneId},\"$escapedNote\"")
        }
        return sb.toString()
    }

    fun importFromCsv(csvContent: String): List<ShiftEntity> {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size <= 1) return emptyList()

        val result = mutableListOf<ShiftEntity>()
        val regex = Regex("""^(\d+),(\d+),(\d*),([^,]+),(.*)$""")
        for (i in 1 until lines.size) {
            val match = regex.matchEntire(lines[i]) ?: continue
            val id = match.groupValues[1].toLongOrNull() ?: 0L
            val start = match.groupValues[2].toLongOrNull() ?: continue
            val end = match.groupValues[3].toLongOrNull()
            val tz = match.groupValues[4].ifBlank { "Africa/Cairo" }
            var note = match.groupValues[5].trim()
            if (note.startsWith("\"") && note.endsWith("\"") && note.length >= 2) {
                note = note.substring(1, note.length - 1).replace("\"\"", "\"")
            }
            result.add(
                ShiftEntity(
                    id = id,
                    startTime = start,
                    endTime = end,
                    timezoneId = tz,
                    note = note.ifBlank { null }
                )
            )
        }
        return result
    }

    fun exportToJson(shifts: List<ShiftEntity>): String {
        val array = JSONArray()
        for (s in shifts) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("startTime", s.startTime)
                if (s.endTime != null) put("endTime", s.endTime) else put("endTime", JSONObject.NULL)
                put("timezoneId", s.timezoneId)
                put("note", s.note ?: "")
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importFromJson(jsonContent: String): List<ShiftEntity> {
        val array = JSONArray(jsonContent)
        val list = mutableListOf<ShiftEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val endTime = if (obj.isNull("endTime")) null else obj.optLong("endTime")
            val note = obj.optString("note", "").ifBlank { null }
            list.add(
                ShiftEntity(
                    id = obj.optLong("id", 0L),
                    startTime = obj.getLong("startTime"),
                    endTime = endTime,
                    timezoneId = obj.optString("timezoneId", "Africa/Cairo"),
                    note = note
                )
            )
        }
        return list
    }
}
