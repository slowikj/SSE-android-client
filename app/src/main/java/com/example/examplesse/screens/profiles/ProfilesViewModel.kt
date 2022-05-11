package com.example.examplesse.screens.profiles

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examplesse.data.ProfilesEvent
import com.example.examplesse.data.ProfilesStreamingDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfilesUiState(
    val data: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: Throwable? = null
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profilesStreamingDataSource: ProfilesStreamingDataSource
) : ViewModel() {

    private val _profiles = MutableStateFlow(ProfilesUiState(isLoading = true))

    val profiles: StateFlow<ProfilesUiState> = _profiles.asStateFlow()

    private var profileStreamingJob: Job? = null

    private val _isStreaming = MutableStateFlow(true)

    val isStreaming = _isStreaming.asStateFlow()

    init {
        startProfileStreaming()
    }

    fun toggleProfileStreaming() {
        if (!isStreaming.value) {
            startProfileStreaming()
        } else {
            stopProfileStreaming()
        }
    }

    private fun startProfileStreaming() {
        profileStreamingJob = profilesStreamingDataSource
            .profilesFlow
            .onEach { event ->
                Log.d("ProfilesViewModel", "event $event")
                _profiles.update { state ->
                    mapProfileEventToUi(event, state)
                }
            }.launchIn(viewModelScope)

        _isStreaming.value = true
    }

    private fun stopProfileStreaming() {
        profileStreamingJob?.cancel()
        profileStreamingJob = null

        _isStreaming.value = false
    }

    private fun mapProfileEventToUi(
        event: ProfilesEvent,
        prevUiState: ProfilesUiState
    ) = when (event) {
        is ProfilesEvent.Data ->
            prevUiState.copy(
                error = null,
                data = event.profiles,
                isLoading = false
            )
        is ProfilesEvent.Loading ->
            prevUiState.copy(
                error = null,
                isLoading = true
            )
        is ProfilesEvent.Error ->
            prevUiState.copy(
                error = event.throwable,
                isLoading = false
            )
    }

}