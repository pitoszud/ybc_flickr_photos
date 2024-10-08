package com.velocip.ybs.photos.presentation.screens.photos_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocip.ybs.data.repo.PhotoSearchRepo
import com.velocip.ybs.model.PhotoItemUi
import com.velocip.ybs.model.PhotosUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val photoRepo: PhotoSearchRepo
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _query = MutableStateFlow("")

    init {
        searchPhotos("Yorkshire")
    }


    private val photos: StateFlow<List<PhotoItemUi>> = _query
        .flatMapLatest { query ->
            photoRepo.getPhotos()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    val photosUiState: StateFlow<PhotosUiState> =
        combine(_isLoading, _errorMessage, photos) { isLoading, errorMessage, photos ->
        PhotosUiState(
            isLoading = isLoading,
            errorMessage = errorMessage,
            photos = photos
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        PhotosUiState(false, null, emptyList())
    )



    fun searchPhotos(query: String) {
        _isLoading.value = true
        _query.value = query
        viewModelScope.launch {
            photoRepo.searchPhotos(query = query.trim()).fold(
                onSuccess = {
                    _isLoading.value = false
                    _errorMessage.value = null
                },
                onFailure = {
                    _isLoading.value = false
                    _errorMessage.value = it.message
                }
            )
        }
    }

}