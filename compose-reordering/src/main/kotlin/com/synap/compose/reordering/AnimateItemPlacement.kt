package com.synap.compose.reordering

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier

@ExperimentalFoundationApi
fun Modifier.animateItemPlacement(scope: LazyItemScope, reorderingState: ReorderingState): Modifier =
    if (reorderingState.draggingIndex == -1) {
        with(scope) {
            animateItemPlacement()
        }
    } else {
        this
    }
