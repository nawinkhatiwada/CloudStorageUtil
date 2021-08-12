package com.evolveasia.cloudutils

internal inline fun <R> R?.orElse(block: () -> R): R {
    return this ?: block()
}


