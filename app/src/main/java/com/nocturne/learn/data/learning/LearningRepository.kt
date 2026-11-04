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
import kotlinx.serialization.json.jsonObject

/**
 * 学习进度是一个小 JSON 文件，里面可以有多条航线（多门学习目标）。
 * 没有数据库，卸载即清空；导出时直接带走整个文件即可。
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

    private val _archive = MutableStateFlow(load())
    val archive: StateFlow<LearningArchive> = _archive.asStateFlow()

    private val _state = MutableStateFlow(_archive.value.active() ?: LearningState())
    /** 当前航线的状态；所有教学工具都只改这一条。 */
    val state: StateFlow<LearningState> = _state.asStateFlow()

    private val _subjects = MutableStateFlow(_archive.value.subjects)
    val subjects: StateFlow<List<LearningState>> = _subjects.asStateFlow()

    private val _activeSubjectId = MutableStateFlow(_archive.value.active()?.id)
    val activeSubjectId: StateFlow<String?> = _activeSubjectId.asStateFlow()

    fun update(transform: (LearningState) -> LearningState): LearningState = synchronized(lock) {
        val (archive, active) = ensureActive(_archive.value)
        val next = transform(active)
        val subjects = archive.subjects.map { if (it.id == active.id) next else it }
        publish(archive.copy(subjects = subjects))
        next
    }

    /** 新建一条学习航线；同名目标会直接切换过去，避免重复。 */
    fun createSubject(
        goal: String,
        dailyMinutes: Int = 30,
        makeActive: Boolean = true,
    ): LearningState = synchronized(lock) {
        val cleanGoal = goal.trim().ifBlank { "未命名航线" }
        val archive = _archive.value
        val existing = archive.subjects.firstOrNull {
            it.goal.trim().equals(cleanGoal, ignoreCase = true)
        }
        if (existing != null) {
            publish(archive.copy(activeSubjectId = existing.id))
            return@synchronized existing
        }
        val subject = LearningState(
            id = UUID.randomUUID().toString(),
            goal = cleanGoal,
            dailyMinutes = dailyMinutes.coerceIn(5, 600),
        )
        publish(
            archive.copy(
                subjects = archive.subjects + subject,
                activeSubjectId = if (makeActive) subject.id else archive.activeSubjectId,
            ),
        )
        subject
    }

    fun switchSubject(subjectId: String) = synchronized(lock) {
        val archive = _archive.value
        if (archive.subjects.any { it.id == subjectId }) {
            publish(archive.copy(activeSubjectId = subjectId))
        }
    }

    fun updateActivePlan(goal: String, dailyMinutes: Int) = synchronized(lock) {
        val (archive, active) = ensureActive(_archive.value)
        val next = active.copy(
            goal = goal.trim().ifBlank { active.goal },
            dailyMinutes = dailyMinutes.coerceIn(5, 600),
        )
        publish(
            archive.copy(
                subjects = archive.subjects.map { if (it.id == active.id) next else it },
                activeSubjectId = active.id,
            ),
        )
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
                goal = current.goal.ifBlank { goal.trim() },
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
                notes = (it.notes + summary.take(240)).filter { n -> n.isNotBlank() }.takeLast(40),
            )
        }
    }

    fun reset() {
        synchronized(lock) {
            file.delete()
            publish(LearningArchive(), persist = false)
        }
    }

    fun exportJson(): String = json.encodeToString(_archive.value)

    private fun ensureActive(archive: LearningArchive): Pair<LearningArchive, LearningState> {
        val active = archive.active()
        if (active != null) return archive to active
        val subject = LearningState(id = UUID.randomUUID().toString())
        return archive.copy(subjects = archive.subjects + subject, activeSubjectId = subject.id) to subject
    }

    private fun publish(archive: LearningArchive, persist: Boolean = true) {
        val active = archive.active()
        val fixed = archive.copy(activeSubjectId = active?.id)
        if (persist) write(fixed)
        _archive.value = fixed
        _subjects.value = fixed.subjects
        _activeSubjectId.value = active?.id
        _state.value = active ?: LearningState()
    }

    private fun load(): LearningArchive {
        if (!file.exists()) return LearningArchive()
        val text = runCatching { file.readText() }.getOrNull().orEmpty()
        if (text.isBlank()) return LearningArchive()

        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        val archive = if (root?.containsKey("subjects") == true) {
            runCatching { json.decodeFromString<LearningArchive>(text) }.getOrNull()
        } else {
            val old = runCatching { json.decodeFromString<LearningState>(text) }.getOrNull()
            when {
                old == null -> null
                old.goal.isBlank() && old.knowledgePoints.isEmpty() && old.sessions == 0 -> null
                else -> {
                    val id = old.id.ifBlank { UUID.randomUUID().toString() }
                    LearningArchive(
                        subjects = listOf(old.copy(id = id)),
                        activeSubjectId = id,
                    )
                }
            }
        } ?: return LearningArchive()

        val normalized = archive.subjects.map { subject ->
            if (subject.id.isBlank()) subject.copy(id = UUID.randomUUID().toString()) else subject
        }
        if (normalized.isEmpty()) return LearningArchive()
        val activeId = archive.activeSubjectId
            ?.takeIf { id -> normalized.any { it.id == id } }
            ?: normalized.first().id
        return LearningArchive(subjects = normalized, activeSubjectId = activeId)
    }

    private fun write(archive: LearningArchive) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(archive))
            if (!tmp.renameTo(file)) {
                file.writeText(json.encodeToString(archive))
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
