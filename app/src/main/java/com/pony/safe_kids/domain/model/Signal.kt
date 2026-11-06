package com.pony.safe_kids.domain.model

sealed interface Signal<out T> {
    data class Supported<out T>(val value: T) : Signal<T>

    data object Unsupported : Signal<Nothing>
}
