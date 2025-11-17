package io.github.lumkit.pline.ui.screen.home

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.github.lumkit.pline.base.BaseViewModel
import io.github.lumkit.pline.db.AppDatabase
import io.github.lumkit.pline.db.entity.Work
import io.github.lumkit.pline.db.repo.WorkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    database: AppDatabase
): BaseViewModel() {

    private val repository = WorkRepository(database)

    val aliveWorks: StateFlow<List<Work>> = repository.observeActive().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    suspend fun deleteWork(work: Work) {
        repository.markDeleted(work.id, true)
    }
}
