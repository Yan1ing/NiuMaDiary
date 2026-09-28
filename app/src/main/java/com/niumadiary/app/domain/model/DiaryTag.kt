package com.niumadiary.app.domain.model

data class DiaryTag(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val sortOrder: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "tag id cannot be blank" }
        require(name.isNotBlank()) { "tag name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "tag name is too long" }
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 24
    }
}
