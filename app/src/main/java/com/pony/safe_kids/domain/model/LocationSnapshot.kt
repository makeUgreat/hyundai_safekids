package com.pony.safe_kids.domain.model

sealed interface LocationSnapshot<out L> {
    data object Pending : LocationSnapshot<Nothing>

    data class Unknown<L>(val locations: Set<L>) : LocationSnapshot<L>

    data class Ready<L>(val values: Map<L, Boolean>) : LocationSnapshot<L>
}
