package com.example.examplesse.screens.profiles

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.examplesse.data.Event
import com.example.examplesse.data.ProfileSSEDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfilesUiState(
    val data: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: Throwable? = null
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profilesStreamingDataSource: ProfileSSEDataSource,
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
            .flow
            .onEach { event ->
                Log.d("ProfilesViewModel", "event $event")
                showLoadingFirstIfData(event)
                _profiles.update { state ->
                    mapProfileEventToUi(event, state)
                }
            }.launchIn(viewModelScope)

        _isStreaming.value = true
    }

    private suspend fun showLoadingFirstIfData(event: Event<List<String>>) {
        // just for the sake of better UX, show extra loading before new data
        if (event is Event.Data) {
            _profiles.update { it.copy(isLoading = true) }
            delay(1000)
        }
    }

    private fun stopProfileStreaming() {
        profileStreamingJob?.cancel()
        profileStreamingJob = null

        _isStreaming.value = false
    }

    private fun mapProfileEventToUi(
        event: Event<List<String>>,
        prevUiState: ProfilesUiState
    ) = when (event) {
        is Event.Data ->
            prevUiState.copy(
                error = null,
                data = event.data,
                isLoading = false
            )
        is Event.Error ->
            prevUiState.copy(
                error = event.throwable,
                isLoading = false
            )
    }

}