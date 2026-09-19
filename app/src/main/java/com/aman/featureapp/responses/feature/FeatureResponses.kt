package com.aman.featureapp.responses.feature

data class ChatMessageItem(
    val role: String,
    val content: String,
    val timestamp: String? = null
)

data class FeatureItem(
    val id: String? = null,
    val uid: String,
    val owner_id: String? = null,
    val name: String,
    val description: String,
    val status: String, // "building", "clarifying", "ready", "error"
    val is_public: Boolean = false,
    val is_favourite: Boolean = false,
    val chat_session: List<ChatMessageItem>? = null,
    val folder_path: String? = null,
    val error_msg: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val clear_storage_requested: Boolean? = false
)

data class FeatureStatusResponse(
    val uid: String,
    val status: String,
    val pending_question: String? = null,
    val error_msg: String? = null
)
