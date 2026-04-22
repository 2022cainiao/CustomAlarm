package com.customalarm.app.ui.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.customalarm.app.AppContainer
import com.customalarm.app.domain.RoutineGroupDraft
import kotlinx.coroutines.launch

data class RoutineGroupEditorUiState(
    val id: Long = 0L,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val name: String = "",
    val enabled: Boolean = true,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

class RoutineGroupEditorViewModel(
    private val container: AppContainer,
    private val groupId: Long?
) : ViewModel() {
    var uiState by mutableStateOf(RoutineGroupEditorUiState())
        private set

    init {
        viewModelScope.launch {
            val existing = groupId?.let { container.routineGroupRepository.getRoutineGroup(it) }
            uiState = if (existing == null) {
                uiState.copy(isLoading = false)
            } else {
                RoutineGroupEditorUiState(
                    id = existing.id,
                    isLoading = false,
                    name = existing.name,
                    enabled = existing.enabled
                )
            }
        }
    }

    fun updateName(value: String) {
        uiState = uiState.copy(name = value, errorMessage = null)
    }

    fun updateEnabled(enabled: Boolean) {
        uiState = uiState.copy(enabled = enabled)
    }

    fun save() {
        if (uiState.name.isBlank()) {
            uiState = uiState.copy(errorMessage = "Enter a routine group name.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true, errorMessage = null)
            container.alarmCoordinator.saveRoutineGroup(
                RoutineGroupDraft(
                    id = uiState.id,
                    name = uiState.name,
                    enabled = uiState.enabled
                )
            )
            uiState = uiState.copy(isSaving = false, saved = true)
        }
    }

    companion object {
        fun factory(container: AppContainer, groupId: Long?): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoutineGroupEditorViewModel(container, groupId) as T
                }
            }
        }
    }
}
