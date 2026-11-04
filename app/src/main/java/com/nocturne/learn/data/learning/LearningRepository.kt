package com.nocturne.learn.data.learning

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 学习进度是一个小 JSON 文件。没有数据库，卸载即清空；后续做导出时直接带走这个文件即可。
 */
class LearningRepository(
    context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    },
) {

    private val file = File(context.filesDir, "learning_state.json")
    private val lock = Any()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LearningState> = _state.asStateFlow()

    fun update(transform: (LearningState) -> LearningState): LearningState = synchronized(lock) {
        val next = transform(_state.value)
        write(next)
        _state.value = next
        next
    }

    fun markSessionStarted(goal: String) {
        update { current ->
            val today = todayEpochDay()
            val newStreak = when {
                current.lastStudyEpochDay == today -> current.streak
                current.lastStudyEpochDay == today - 1 -> current.streak + 1
                current.streak == 0 -> 1
                else -> 1
            }
            current.copy(
                goal = current.goal.ifBlank { goal },
                sessions = current.sessions + 1,
                streak = newStreak,
                lastStudyEpochDay = today,
            )
        }
    }

    fun upsertKnowledgePoint(
        title: String,
        mastery: Float,
        note: String = "",
        evidence: String = "",
    ): KnowledgePoint {
        val cleanTitle = title.trim()
        var result: KnowledgePoint? = null
        update { current ->
            val existing = current.findPoint(cleanTitle)
            val point = if (existing != null) {
                existing.copy(
                    mastery = mastery.coerceIn(0f, 1f),
                    note = note.ifBlank { existing.note },
                    status = statusFor(mastery),
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                KnowledgePoint(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    note = note,
                    mastery = mastery.coerceIn(0f, 1f),
                    status = statusFor(mastery),
                    updatedAt = System.currentTimeMillis(),
                )
            }
            result = point
            val points = current.knowledgePoints.filterNot {
                it.title.equals(cleanTitle, ignoreCase = true)
            } + point
            val newNote = if (evidence.isBlank()) current.notes else {
                (current.notes + "${point.title}：${evidence.take(120)}").takeLast(40)
            }
            current.copy(knowledgePoints = points, notes = newNote)
        }
        return result!!
    }

    fun savePlan(goal: String, level: String, plan: String, points: List<Pair<String, String>>) {
        update { current ->
            val merged = current.knowledgePoints.toMutableList()
            points.forEach { (title, note) ->
                val cleanTitle = title.trim()
                if (cleanTitle.isBlank()) return@forEach
                val existing = merged.firstOrNull {
                    it.title.equals(cleanTitle, ignoreCase = true)
                }
                if (existing == null) {
                    merged += KnowledgePoint(
                        id = UUID.randomUUID().toString(),
                        title = cleanTitle,
                        note = note,
                        updatedAt = System.currentTimeMillis(),
                    )
                } else if (note.isNotBlank() && existing.note.isBlank()) {
                    val index = merged.indexOf(existing)
                    merged[index] = existing.copy(note = note)
                }
            }
            current.copy(
                goal = goal.ifBlank { current.goal },
                level = level.ifBlank { current.level },
                plan = plan.ifBlank { current.plan },
                knowledgePoints = merged,
            )
        }
    }

    fun scheduleReview(kpTitle: String, days: Int = 1) {
        update { current ->
            val point = current.findPoint(kpTitle) ?: return@update current
            val interval = days.coerceIn(1, 365)
            val dueAt = System.currentTimeMillis() + interval * 24L * 60L * 60L * 1000L
            val existing = current.reviews.firstOrNull { it.kpId == point.id }
            val review = if (existing != null) {
                existing.copy(dueAt = dueAt, intervalDays = interval, reps = existing.reps + 1)
            } else {
                ReviewItem(
                    id = UUID.randomUUID().toString(),
                    kpId = point.id,
                    dueAt = dueAt,
                    intervalDays = interval,
                    reps = 1,
                )
            }
            current.copy(reviews = current.reviews.filterNot { it.kpId == point.id } + review)
        }
    }

    fun addNote(text: String) {
        if (text.isBlank()) return
        update { it.copy(notes = (it.notes + text.take(240)).takeLast(40)) }
    }

    fun completeSession(summary: String) {
        update {
            it.copy(
                lastStudyEpochDay = todayEpochDay(),
                plan = it.plan,
                notes = (it.notes + summary.take(240)).filter { n -> n.isNotBlank() }.takeLast(40),
            )
        }
    }

    fun reset() {
        synchronized(lock) {
            file.delete()
            _state.value = LearningState()
        }
    }

    fun exportJson(): String = json.encodeToString(_state.value)

    private fun load(): LearningState {
        if (!file.exists()) return LearningState()
        return runCatching { json.decodeFromString<LearningState>(file.readText()) }
            .getOrDefault(LearningState())
    }

    private fun write(state: LearningState) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(state))
            if (!tmp.renameTo(file)) {
                file.writeText(json.encodeToString(state))
                tmp.delete()
            }
        }
    }

    private fun statusFor(mastery: Float): String = when {
        mastery >= 0.8f -> "mastered"
        mastery > 0f -> "learning"
        else -> "new"
    }

    private fun todayEpochDay(): Long =
        LocalDate.now(ZoneId.systemDefault()).toEpochDay()
}
