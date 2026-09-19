package com.aman.featureapp.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.featureapp.api.ApiServer
import com.aman.featureapp.api.ErrorParser
import com.aman.featureapp.requests.feature.ClarifyFeatureRequest
import com.aman.featureapp.requests.feature.CreateFeatureRequest
import com.aman.featureapp.requests.feature.EditFeatureRequest
import com.aman.featureapp.responses.feature.FeatureItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val time: String = ""
)

sealed class FeatureBuildStatus {
    object Idle : FeatureBuildStatus()
    data class Building(
        val featureName: String,
        val uid: String,
        val startTimeMs: Long = System.currentTimeMillis()
    ) : FeatureBuildStatus()
    data class Clarifying(val featureName: String, val uid: String, val question: String) : FeatureBuildStatus()
    data class Ready(val featureName: String, val uid: String, val item: FeatureItem?) : FeatureBuildStatus()
    data class Failed(val featureName: String, val uid: String, val errorMessage: String) : FeatureBuildStatus()
}

enum class ChatStep {
    ASKING_NAME,
    ASKING_DESCRIPTION,
    BUILDING_ACTIVE,
    /** User tapped "Edit" on a feature — waiting for them to describe changes */
    EDITING_DESCRIPTION
}

data class ActiveFeatureSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uid: String? = null,
    val featureName: String = "New Feature",
    val chatMessages: List<UiChatMessage> = emptyList(),
    val currentStep: ChatStep = ChatStep.ASKING_NAME,
    val pendingFeatureName: String = "",
    val editingFeatureUid: String? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val latestStatus: FeatureBuildStatus = FeatureBuildStatus.Idle,
    val startTimeMs: Long = System.currentTimeMillis()
)

data class HomeUiState(
    val sessions: List<ActiveFeatureSession> = emptyList(),
    val activeSessionId: String = "",
    val chatMessages: List<UiChatMessage> = emptyList(),
    val currentStep: ChatStep = ChatStep.ASKING_NAME,
    val pendingFeatureName: String = "",
    /** UID of the feature being edited (only set during EDITING_DESCRIPTION step) */
    val editingFeatureUid: String? = null,
    val inputText: String = "",
    val isSending: Boolean = false,
    val latestStatus: FeatureBuildStatus = FeatureBuildStatus.Idle,
    val activeUid: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val pollingJobs = mutableMapOf<String, Job>()

    companion object {
        private fun createInitialSession(): ActiveFeatureSession {
            return ActiveFeatureSession(
                id = "initial_${System.currentTimeMillis()}",
                featureName = "New Feature",
                chatMessages = listOf(
                    UiChatMessage(
                        isUser = false,
                        text = "👋 Hi! I can turn your ideas into working mobile apps.\n\nWhat would you like to call your app?"
                    )
                ),
                currentStep = ChatStep.ASKING_NAME
            )
        }

        private fun createInitialState(): HomeUiState {
            val session = createInitialSession()
            return HomeUiState(
                sessions = listOf(session),
                activeSessionId = session.id,
                chatMessages = session.chatMessages,
                currentStep = session.currentStep,
                pendingFeatureName = session.pendingFeatureName,
                editingFeatureUid = session.editingFeatureUid,
                inputText = session.inputText,
                isSending = session.isSending,
                latestStatus = session.latestStatus,
                activeUid = session.uid
            )
        }
    }

    private fun syncActiveSession(
        sessions: List<ActiveFeatureSession>,
        activeId: String,
        errorMsg: String? = null
    ): HomeUiState {
        val active = sessions.find { it.id == activeId } ?: sessions.firstOrNull()
        return if (active != null) {
            HomeUiState(
                sessions = sessions,
                activeSessionId = active.id,
                chatMessages = active.chatMessages,
                currentStep = active.currentStep,
                pendingFeatureName = active.pendingFeatureName,
                editingFeatureUid = active.editingFeatureUid,
                inputText = active.inputText,
                isSending = active.isSending,
                latestStatus = active.latestStatus,
                activeUid = active.uid,
                errorMessage = errorMsg ?: _uiState.value.errorMessage
            )
        } else {
            createInitialState()
        }
    }

    private fun updateActiveSession(transform: (ActiveFeatureSession) -> ActiveFeatureSession) {
        val currentSessions = _uiState.value.sessions
        val activeId = _uiState.value.activeSessionId
        val updated = currentSessions.map {
            if (it.id == activeId) transform(it) else it
        }
        _uiState.value = syncActiveSession(updated, activeId)
    }

    private fun updateSessionByUid(uid: String, transform: (ActiveFeatureSession) -> ActiveFeatureSession) {
        val currentSessions = _uiState.value.sessions
        val activeId = _uiState.value.activeSessionId
        val updated = currentSessions.map {
            if (it.uid == uid || it.id == uid || it.editingFeatureUid == uid) transform(it) else it
        }
        _uiState.value = syncActiveSession(updated, activeId)
    }

    private fun appendChatMessageToSession(uid: String, message: UiChatMessage) {
        updateSessionByUid(uid) { session ->
            session.copy(chatMessages = session.chatMessages + message)
        }
    }

    fun onInputChanged(newText: String) {
        updateActiveSession { it.copy(inputText = newText) }
    }

    fun selectSession(sessionId: String) {
        if (_uiState.value.activeSessionId == sessionId) return
        _uiState.value = syncActiveSession(_uiState.value.sessions, sessionId)
    }

    fun closeSession(sessionId: String) {
        val currentSessions = _uiState.value.sessions
        val target = currentSessions.find { it.id == sessionId }
        target?.uid?.let { uid ->
            pollingJobs[uid]?.cancel()
            pollingJobs.remove(uid)
        }
        val remaining = currentSessions.filterNot { it.id == sessionId }
        if (remaining.isEmpty()) {
            val fresh = createInitialSession()
            _uiState.value = syncActiveSession(listOf(fresh), fresh.id)
        } else {
            val nextActiveId = if (_uiState.value.activeSessionId == sessionId) {
                remaining.last().id
            } else {
                _uiState.value.activeSessionId
            }
            _uiState.value = syncActiveSession(remaining, nextActiveId)
        }
    }

    fun onSendClicked() {
        val activeSession = _uiState.value.sessions.find { it.id == _uiState.value.activeSessionId } ?: return
        val text = activeSession.inputText.trim()
        if (text.isEmpty() || activeSession.isSending) return

        when (activeSession.currentStep) {
            ChatStep.ASKING_NAME -> {
                val updatedMessages = activeSession.chatMessages + listOf(
                    UiChatMessage(isUser = true, text = text),
                    UiChatMessage(
                        isUser = false,
                        text = "Got it: \"$text\" ✨\n\nNow describe what it should do. What happens when you open it? What can you do with it?"
                    )
                )
                updateActiveSession {
                    it.copy(
                        chatMessages = updatedMessages,
                        pendingFeatureName = text,
                        featureName = text,
                        inputText = "",
                        currentStep = ChatStep.ASKING_DESCRIPTION
                    )
                }
            }

            ChatStep.ASKING_DESCRIPTION -> {
                val featureName = activeSession.pendingFeatureName.ifBlank { "New Feature" }
                val updatedMessages = activeSession.chatMessages + listOf(
                    UiChatMessage(isUser = true, text = text),
                    UiChatMessage(
                        isUser = false,
                        text = "Got it! Building \"$featureName\"… This usually takes 30–60 seconds."
                    )
                )
                updateActiveSession {
                    it.copy(
                        chatMessages = updatedMessages,
                        inputText = "",
                        isSending = true,
                        currentStep = ChatStep.BUILDING_ACTIVE
                    )
                }
                createFeatureAndStartPolling(activeSession.id, featureName, text)
            }

            ChatStep.BUILDING_ACTIVE -> {
                val uid = activeSession.uid ?: return
                val updatedMessages = activeSession.chatMessages + listOf(
                    UiChatMessage(isUser = true, text = text)
                )
                updateActiveSession {
                    it.copy(
                        chatMessages = updatedMessages,
                        inputText = "",
                        isSending = true
                    )
                }
                submitClarification(activeSession.id, uid, text)
            }

            ChatStep.EDITING_DESCRIPTION -> {
                val uid = activeSession.editingFeatureUid ?: return
                val featureName = activeSession.pendingFeatureName
                val updatedMessages = activeSession.chatMessages + listOf(
                    UiChatMessage(isUser = true, text = text),
                    UiChatMessage(
                        isUser = false,
                        text = "Updating \"$featureName\" with your changes…"
                    )
                )
                updateActiveSession {
                    it.copy(
                        chatMessages = updatedMessages,
                        inputText = "",
                        isSending = true,
                        currentStep = ChatStep.BUILDING_ACTIVE,
                        editingFeatureUid = null
                    )
                }
                submitEdit(activeSession.id, uid, featureName, text)
            }
        }
    }

    /**
     * Called when user clicks "New Feature" button.
     * Creates a new blank session so the user can build another feature concurrently
     * while existing features continue building in the background.
     */
    fun startNewFeature() {
        val existingIdle = _uiState.value.sessions.firstOrNull {
            it.uid == null && it.currentStep == ChatStep.ASKING_NAME && it.latestStatus is FeatureBuildStatus.Idle
        }
        if (existingIdle != null) {
            selectSession(existingIdle.id)
            return
        }

        val newSession = ActiveFeatureSession(
            id = "session_${System.currentTimeMillis()}",
            featureName = "New Feature",
            chatMessages = listOf(
                UiChatMessage(
                    isUser = false,
                    text = "✨ Let's build another one!\n\nWhat would you like to call it?"
                )
            ),
            currentStep = ChatStep.ASKING_NAME
        )
        val updated = _uiState.value.sessions + newSession
        _uiState.value = syncActiveSession(updated, newSession.id)
    }

    /**
     * Called from FeatureScreen when the user taps "Edit".
     * Reuses or creates an active session for this feature.
     */
    fun startEditFeature(uid: String, name: String) {
        val currentSessions = _uiState.value.sessions
        val existing = currentSessions.find { it.uid == uid || it.id == uid || it.editingFeatureUid == uid }
        if (existing != null) {
            val updated = currentSessions.map {
                if (it.id == existing.id) {
                    it.copy(
                        featureName = name,
                        pendingFeatureName = name,
                        editingFeatureUid = uid,
                        currentStep = ChatStep.EDITING_DESCRIPTION,
                        inputText = "",
                        latestStatus = FeatureBuildStatus.Idle,
                        chatMessages = it.chatMessages + listOf(
                            UiChatMessage(
                                isUser = false,
                                text = "✏️ Editing \"$name\"\n\nDescribe what you'd like to change or improve. The AI will regenerate the feature from scratch using your new description."
                            )
                        )
                    )
                } else it
            }
            _uiState.value = syncActiveSession(updated, existing.id)
        } else {
            val editSession = ActiveFeatureSession(
                id = uid,
                uid = uid,
                featureName = name,
                chatMessages = listOf(
                    UiChatMessage(
                        isUser = false,
                        text = "✏️ Editing \"$name\"\n\nDescribe what you'd like to change or improve. The AI will regenerate the feature from scratch using your new description."
                    )
                ),
                currentStep = ChatStep.EDITING_DESCRIPTION,
                pendingFeatureName = name,
                editingFeatureUid = uid,
                inputText = "",
                latestStatus = FeatureBuildStatus.Idle
            )
            val updated = currentSessions + editSession
            _uiState.value = syncActiveSession(updated, editSession.id)
        }
    }

    /**
     * Called when a feature is deleted from the Features screen.
     * Cancels any active background polling for that UID, removes it from active sessions,
     * and clears it from the Home screen UI.
     */
    fun onFeatureDeleted(deletedUid: String) {
        pollingJobs[deletedUid]?.cancel()
        pollingJobs.remove(deletedUid)

        val currentSessions = _uiState.value.sessions
        val remaining = currentSessions.filterNot {
            it.uid == deletedUid || it.id == deletedUid || it.editingFeatureUid == deletedUid
        }

        if (remaining.isEmpty()) {
            val fresh = createInitialSession()
            _uiState.value = syncActiveSession(listOf(fresh), fresh.id)
        } else {
            val nextActiveId = if (_uiState.value.activeSessionId == deletedUid ||
                currentSessions.find { it.id == _uiState.value.activeSessionId }?.uid == deletedUid
            ) {
                remaining.last().id
            } else {
                _uiState.value.activeSessionId
            }
            _uiState.value = syncActiveSession(remaining, nextActiveId)
        }
    }

    private fun createFeatureAndStartPolling(sessionId: String, name: String, description: String) {
        viewModelScope.launch {
            try {
                val response = ApiServer.featureApi.createFeature(
                    CreateFeatureRequest(name = name, description = description)
                )
                if (response.success && response.data != null) {
                    val uid = response.data.uid
                    val now = System.currentTimeMillis()
                    val currentSessions = _uiState.value.sessions.map {
                        if (it.id == sessionId) {
                            it.copy(
                                id = uid,
                                uid = uid,
                                featureName = name,
                                isSending = false,
                                latestStatus = FeatureBuildStatus.Building(name, uid, now),
                                startTimeMs = now
                            )
                        } else it
                    }
                    val newActiveId = if (_uiState.value.activeSessionId == sessionId) uid else _uiState.value.activeSessionId
                    _uiState.value = syncActiveSession(currentSessions, newActiveId)
                    startPolling(uid, name)
                } else {
                    val rawErr = response.message ?: "Could not start creation."
                    val cleanErr = if (rawErr.contains("html", true) || rawErr.contains("css", true) || rawErr.length > 120) {
                        "Service is temporarily unavailable. Please try again."
                    } else {
                        rawErr
                    }
                    updateSessionByUid(sessionId) {
                        it.copy(
                            isSending = false,
                            latestStatus = FeatureBuildStatus.Failed(name, "", cleanErr),
                            chatMessages = it.chatMessages + UiChatMessage(
                                isUser = false,
                                text = "⚠️ $cleanErr"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                val rawErr = ErrorParser.parse(e)
                val cleanErr = if (rawErr.contains("html", true) || rawErr.contains("css", true) || rawErr.length > 120) {
                    "Could not connect to server. Please check your network."
                } else {
                    rawErr
                }
                updateSessionByUid(sessionId) {
                    it.copy(
                        isSending = false,
                        latestStatus = FeatureBuildStatus.Failed(name, "", cleanErr),
                        chatMessages = it.chatMessages + UiChatMessage(
                            isUser = false,
                            text = "⚠️ $cleanErr"
                        )
                    )
                }
            }
        }
    }

    private fun submitEdit(sessionId: String, uid: String, name: String, newDescription: String) {
        viewModelScope.launch {
            try {
                val response = ApiServer.featureApi.editFeature(
                    uid = uid,
                    request = EditFeatureRequest(description = newDescription)
                )
                if (response.success) {
                    val now = System.currentTimeMillis()
                    updateSessionByUid(uid) {
                        it.copy(
                            isSending = false,
                            latestStatus = FeatureBuildStatus.Building(name, uid, now),
                            startTimeMs = now
                        )
                    }
                    startPolling(uid, name)
                } else {
                    val err = response.message ?: "Failed to start edit"
                    updateSessionByUid(uid) {
                        it.copy(
                            isSending = false,
                            currentStep = ChatStep.EDITING_DESCRIPTION,
                            editingFeatureUid = uid,
                            chatMessages = it.chatMessages + UiChatMessage(
                                isUser = false,
                                text = "❌ Could not start edit: $err"
                            ),
                            latestStatus = FeatureBuildStatus.Failed(name, uid, err)
                        )
                    }
                }
            } catch (e: Exception) {
                val err = ErrorParser.parse(e)
                updateSessionByUid(uid) {
                    it.copy(
                        isSending = false,
                        currentStep = ChatStep.EDITING_DESCRIPTION,
                        editingFeatureUid = uid,
                        chatMessages = it.chatMessages + UiChatMessage(
                            isUser = false,
                            text = "❌ Could not start edit: $err"
                        ),
                        latestStatus = FeatureBuildStatus.Failed(name, uid, err)
                    )
                }
            }
        }
    }

    private fun submitClarification(sessionId: String, uid: String, answer: String) {
        viewModelScope.launch {
            try {
                val response = ApiServer.featureApi.clarifyFeature(
                    uid = uid,
                    request = ClarifyFeatureRequest(answer = answer)
                )
                if (response.success) {
                    val targetSession = _uiState.value.sessions.find { it.uid == uid || it.id == sessionId }
                    val name = targetSession?.featureName ?: "Feature"
                    val now = System.currentTimeMillis()
                    updateSessionByUid(uid) {
                        it.copy(
                            isSending = false,
                            latestStatus = FeatureBuildStatus.Building(name, uid, now),
                            startTimeMs = now
                        )
                    }
                    startPolling(uid, name)
                } else {
                    updateSessionByUid(uid) {
                        it.copy(
                            isSending = false,
                            latestStatus = FeatureBuildStatus.Failed(it.featureName, uid, response.message ?: "Failed to submit clarification")
                        )
                    }
                }
            } catch (e: Exception) {
                updateSessionByUid(uid) {
                    it.copy(
                        isSending = false,
                        latestStatus = FeatureBuildStatus.Failed(it.featureName, uid, ErrorParser.parse(e))
                    )
                }
            }
        }
    }

    private fun startPolling(uid: String, featureName: String) {
        pollingJobs[uid]?.cancel()
        val job = viewModelScope.launch {
            var consecutiveNetworkErrors = 0
            var elapsedSeconds = 0L
            var lastHeartbeatSeconds = 0L

            while (isActive) {
                // Adaptive poll delay
                val pollDelay = when {
                    elapsedSeconds >= 120 -> 10_000L
                    elapsedSeconds >= 60  -> 6_000L
                    elapsedSeconds >= 30  -> 4_000L
                    else                  -> 3_000L
                }
                delay(pollDelay)
                elapsedSeconds += pollDelay / 1000L

                // In-chat heartbeat every 40 seconds
                val secondsSinceHeartbeat = elapsedSeconds - lastHeartbeatSeconds
                if (secondsSinceHeartbeat >= 40 && elapsedSeconds > 0) {
                    lastHeartbeatSeconds = elapsedSeconds
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                    val session = _uiState.value.sessions.find { it.uid == uid }
                    val alreadyHasThisTime = session?.chatMessages?.any {
                        !it.isUser && it.text.contains("($timeStr")
                    } ?: false

                    if (!alreadyHasThisTime) {
                        appendChatMessageToSession(uid, UiChatMessage(
                            isUser = false,
                            text = when {
                                elapsedSeconds >= 120 -> "⏳ Almost there ($timeStr) — complex features take a bit longer!"
                                elapsedSeconds >= 60  -> "⏳ Still creating ($timeStr) — hang tight…"
                                else                  -> "⏳ Still creating ($timeStr)…"
                            }
                        ))
                    }
                }

                try {
                    val statusRes = ApiServer.featureApi.getFeatureStatus(uid)
                    consecutiveNetworkErrors = 0 // reset on any successful HTTP contact

                    if (!statusRes.success || statusRes.data == null) {
                        // Server returned an error (e.g. 404 or backend failure) — stop polling immediately
                        val err = statusRes.message ?: "Could not get creation status."
                        updateSessionByUid(uid) { s ->
                            s.copy(
                                latestStatus = FeatureBuildStatus.Failed(featureName, uid, err),
                                chatMessages = s.chatMessages + UiChatMessage(isUser = false, text = "⚠️ $err")
                            )
                        }
                        pollingJobs.remove(uid)
                        break
                    }

                    val statusData = statusRes.data
                    when (statusData.status) {
                        "ready" -> {
                            updateSessionByUid(uid) { s ->
                                s.copy(
                                    latestStatus = FeatureBuildStatus.Ready(featureName, uid, null),
                                    chatMessages = s.chatMessages + UiChatMessage(
                                        isUser = false,
                                        text = "🎉 \"$featureName\" is ready! Tap **Open Feature** above to launch it."
                                    )
                                )
                            }
                            pollingJobs.remove(uid)
                            break
                        }
                        "clarifying" -> {
                            val question = statusData.pending_question
                                ?: "AI needs additional info to proceed."
                            updateSessionByUid(uid) { s ->
                                val alreadyShown = s.chatMessages.any {
                                    !it.isUser && it.text.contains(question)
                                }
                                val updatedMessages = if (!alreadyShown) {
                                    s.chatMessages + UiChatMessage(
                                        isUser = false,
                                        text = "❓ Clarification needed:\n$question"
                                    )
                                } else {
                                    s.chatMessages
                                }
                                s.copy(
                                    chatMessages = updatedMessages,
                                    latestStatus = FeatureBuildStatus.Clarifying(featureName, uid, question),
                                    currentStep = ChatStep.BUILDING_ACTIVE
                                )
                            }
                            pollingJobs.remove(uid)
                            break
                        }
                        "error" -> {
                            // Backend reported error: STOP POLLING IMMEDIATELY
                            val rawErr = statusData.error_msg ?: "Creation encountered an issue."
                            val cleanErr = if (rawErr.contains("html", ignoreCase = true) ||
                                               rawErr.contains("css", ignoreCase = true) ||
                                               rawErr.contains("vite", ignoreCase = true) ||
                                               rawErr.contains("npm", ignoreCase = true) ||
                                               rawErr.contains("syntax", ignoreCase = true) ||
                                               rawErr.contains("compilation", ignoreCase = true) ||
                                               rawErr.length > 120) {
                                "Unable to create this feature. Please try describing what you want differently."
                            } else {
                                rawErr
                            }
                            updateSessionByUid(uid) { s ->
                                s.copy(
                                    latestStatus = FeatureBuildStatus.Failed(featureName, uid, cleanErr),
                                    chatMessages = s.chatMessages + UiChatMessage(
                                        isUser = false,
                                        text = "❌ $cleanErr\n\nYou can try again with a revised description."
                                    )
                                )
                            }
                            pollingJobs.remove(uid)
                            break
                        }
                        else -> {
                            // Status is "building" or "in_progress"
                            updateSessionByUid(uid) { s ->
                                val currentBuilding = s.latestStatus as? FeatureBuildStatus.Building
                                val startTime = currentBuilding?.startTimeMs ?: s.startTimeMs
                                s.copy(
                                    latestStatus = FeatureBuildStatus.Building(featureName, uid, startTime)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    consecutiveNetworkErrors++
                    if (consecutiveNetworkErrors >= 3) {
                        val errMsg = "Connection lost after $consecutiveNetworkErrors attempts. Please check your network and retry."
                        updateSessionByUid(uid) { s ->
                            s.copy(
                                latestStatus = FeatureBuildStatus.Failed(featureName, uid, errMsg),
                                chatMessages = s.chatMessages + UiChatMessage(
                                    isUser = false,
                                    text = "⚠️ Network error: $errMsg"
                                )
                            )
                        }
                        pollingJobs.remove(uid)
                        break
                    }
                    delay(3000)
                }
            }
        }
        pollingJobs[uid] = job
    }

    override fun onCleared() {
        super.onCleared()
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
    }
}

