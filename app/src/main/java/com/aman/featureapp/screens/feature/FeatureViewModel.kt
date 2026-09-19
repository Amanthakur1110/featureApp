package com.aman.featureapp.screens.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aman.featureapp.api.ApiServer
import com.aman.featureapp.api.ErrorParser
import com.aman.featureapp.requests.feature.UpdateFeatureRequest
import com.aman.featureapp.responses.feature.FeatureItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeatureUiState(
    val isLoading: Boolean = false,
    val features: List<FeatureItem> = emptyList(),
    val searchQuery: String = "",
    val filterType: FeatureFilter = FeatureFilter.ALL,
    val errorMessage: String? = null
)

enum class FeatureFilter {
    ALL,
    FAVOURITES,
    PUBLIC
}

class FeatureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    init {
        loadFeatures()
    }

    fun loadFeatures() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ApiServer.featureApi.listFeatures()
                if (response.success && response.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        features = response.data
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message ?: "Failed to load features"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ErrorParser.parse(e)
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onFilterSelected(filter: FeatureFilter) {
        _uiState.value = _uiState.value.copy(filterType = filter)
    }

    fun toggleFavourite(feature: FeatureItem) {
        viewModelScope.launch {
            val newFav = !feature.is_favourite
            // Optimistic update
            val currentList = _uiState.value.features.map {
                if (it.uid == feature.uid) it.copy(is_favourite = newFav) else it
            }
            _uiState.value = _uiState.value.copy(features = currentList)

            try {
                ApiServer.featureApi.updateFeature(
                    uid = feature.uid,
                    request = UpdateFeatureRequest(is_favourite = newFav)
                )
            } catch (e: Exception) {
                // Revert on failure
                loadFeatures()
            }
        }
    }

    fun togglePublic(feature: FeatureItem) {
        viewModelScope.launch {
            val newPublic = !feature.is_public
            // Optimistic update
            val currentList = _uiState.value.features.map {
                if (it.uid == feature.uid) it.copy(is_public = newPublic) else it
            }
            _uiState.value = _uiState.value.copy(features = currentList)

            try {
                ApiServer.featureApi.updateFeature(
                    uid = feature.uid,
                    request = UpdateFeatureRequest(is_public = newPublic)
                )
            } catch (e: Exception) {
                // Revert on failure
                loadFeatures()
            }
        }
    }

    fun deleteFeature(feature: FeatureItem) {
        viewModelScope.launch {
            val currentList = _uiState.value.features.filter { it.uid != feature.uid }
            _uiState.value = _uiState.value.copy(features = currentList)

            try {
                ApiServer.featureApi.deleteFeature(feature.uid)
            } catch (e: Exception) {
                loadFeatures()
            }
        }
    }
}
