package com.nocturne.learn.data.learning

import kotlinx.serialization.Serializable

@Serializable
data class KnowledgePoint(
    val id: String,
    val title: String,
    val note: String = "",
    /** 0f ~ 1f */
    val mastery: Float = 0f,
    val status: String = "new",
    val updatedAt: Long = 0L,
    /** 教材航线里的章节名；普通航线为空。 */
    val chapter: String = "",
    /** 第几页 / 第几张幻灯片，方便回到原文。 */
    val sourceRef: String = "",
    /** 从教材里摘出的原文片段，讲课时用来对齐教材。 */
    val sourceExcerpt: String = "",
    /** 在整本书里的顺序。 */
    val order: Int = 0,
)

@Serializable
data class ReviewItem(
    val id: String,
    val kpId: String,
    val dueAt: Long,
    val intervalDays: Int = 1,
    val reps: Int = 0,
)

/** 一条航线 = 一个学习目标。知识点、复习、坚持天数都只属于这条航线。 */
@Serializable
data class LearningState(
    val id: String = "",
    val goal: String = "",
    val dailyMinutes: Int = 30,
    val plan: String = "",
    val level: String = "",
    val knowledgePoints: List<KnowledgePoint> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
    val notes: List<String> = emptyList(),
    val sessions: Int = 0,
    val streak: Int = 0,
    val lastStudyEpochDay: Long = 0L,
    val totalMinutes: Int = 0,
) {

    fun masteredCount(): Int = knowledgePoints.count { it.mastery >= 0.8f }

    fun learningCount(): Int = knowledgePoints.count { it.mastery < 0.8f && it.mastery > 0f }

    fun dueReviews(now: Long = System.currentTimeMillis()): List<ReviewItem> =
        reviews.filter { it.dueAt <= now }

    fun dueCount(now: Long = System.currentTimeMillis()): Int = dueReviews(now).size

    fun findPoint(title: String): KnowledgePoint? {
        val key = title.trim().lowercase()
        return knowledgePoints.firstOrNull { it.title.trim().lowercase() == key }
    }

    fun knowledgePointById(id: String): KnowledgePoint? = knowledgePoints.firstOrNull { it.id == id }

    fun nextTarget(): KnowledgePoint? {
        val dueIds = dueReviews().map { it.kpId }.toSet()
        return knowledgePoints.firstOrNull { it.id in dueIds }
            ?: knowledgePoints.minByOrNull { it.mastery }
    }
}

/** 本机保存的全部航线。旧版本只有一个 LearningState，读取时会自动包成一条航线。 */
@Serializable
data class LearningArchive(
    val subjects: List<LearningState> = emptyList(),
    val activeSubjectId: String? = null,
) {
    fun active(): LearningState? =
        subjects.firstOrNull { it.id == activeSubjectId } ?: subjects.firstOrNull()
}
