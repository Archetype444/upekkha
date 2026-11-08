package com.wanderwildwood.nikka.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/**
 * The backup: everything the app holds as one plain JSON file, written and read through the
 * system's file picker, so it goes wherever the person puts it and nowhere else. The field names
 * are data, not interface, and stay in English.
 *
 * Meditation's ticks are not written: they are the sittings, which are.
 *
 * Version 2 (0.2.0) adds each habit's cue and rest days and each session's kind and pattern.
 * A version 1 file still reads: its sessions are all sittings, its habits have no cue or rest.
 */
object Backup {

    data class HabitWithTicks(val habit: Habit, val ticks: Set<LocalDate>, val rests: Set<LocalDate> = emptySet())
    data class Contents(val habits: List<HabitWithTicks>, val sessions: List<Session>)

    private const val FORMAT = "nikka-backup"

    fun write(store: Store): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", 2)
        root.put("written", Instant.now().toString())
        root.put("habits", JSONArray().apply {
            for (h in store.habits()) {
                put(JSONObject().apply {
                    put("name", if (h.builtin) "" else h.name)
                    put("meditation", h.builtin)
                    put("times_a_week", h.perWeek)
                    put("position", h.position)
                    put("cue", h.cue)
                    put("ticked", JSONArray().apply {
                        if (!h.builtin) store.ticks(h.id).sorted().forEach { put(it.toString()) }
                    })
                    put("rested", JSONArray().apply { store.rests(h.id).sorted().forEach { put(it.toString()) } })
                })
            }
        })
        root.put("sittings", JSONArray().apply {
            for (s in store.sessions().reversed()) {
                put(JSONObject().apply {
                    put("start", Instant.ofEpochMilli(s.start).toString())
                    put("planned_seconds", s.plannedSec)
                    put("sat_seconds", s.satSec)
                    put("note", s.note)
                    put("kind", s.kind)
                    if (s.pattern.isNotEmpty()) put("pattern", s.pattern)
                })
            }
        })
        return root.toString(2)
    }

    /** Reads a backup, or throws if it is not one of ours. */
    fun read(text: String): Contents {
        require(text.toByteArray().size <= 32 * 1024 * 1024) { "Backup exceeds 32 MB" }
        val root = JSONObject(text)
        require(root.optInt("version", 1) in 1..2) { "Unsupported backup version" }
        require(root.optString("format") == FORMAT) { "not a Practice backup" }
        val habits = root.getJSONArray("habits").let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                val ticked = o.optJSONArray("ticked") ?: JSONArray()
                val rested = o.optJSONArray("rested") ?: JSONArray()
                HabitWithTicks(
                    Habit(0, o.optString("name"), o.optInt("times_a_week", 7), o.optBoolean("meditation"), o.optInt("position", i), o.optString("cue")),
                    (0 until ticked.length()).map { LocalDate.parse(ticked.getString(it)) }.toSet(),
                    (0 until rested.length()).map { LocalDate.parse(rested.getString(it)) }.toSet(),
                )
            }
        }
        val sessions = root.getJSONArray("sittings").let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Session(
                    0, Instant.parse(o.getString("start")).toEpochMilli(), o.getInt("planned_seconds"), o.getInt("sat_seconds"),
                    o.optString("note"), o.optString("kind", Session.SIT).ifEmpty { Session.SIT }, o.optString("pattern"),
                )
            }
        }
        require(habits.size in 1..1000 && habits.count { it.habit.builtin } == 1)
        require(sessions.size <= 100000)
        for (h in habits) {
            require(h.habit.name.length <= 200 && h.habit.cue.length <= 500 && h.habit.perWeek in 1..7)
            require((h.ticks + h.rests).all { it.year in 1970..2100 })
        }
        for (s in sessions) {
            require(s.start in 0..4102444800000L && s.plannedSec in 0..36000 && s.satSec in 0..36000)
            require(s.note.length <= 10000 && s.kind in listOf(Session.SIT, Session.BREATHE) && s.pattern.length <= 200)
        }
        return Contents(habits, sessions)
    }
}
