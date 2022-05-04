package com.duckbox.domain.survey

data class Question (
    val type: QuestionType,
    val question: String,
    val candidates: List<String>?
    )

enum class QuestionType {
    MULTI, // [객관식]
    LIKERT, // [선형배율]
}
