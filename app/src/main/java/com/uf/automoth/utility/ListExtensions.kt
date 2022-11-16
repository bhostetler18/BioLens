package com.uf.automoth.utility

fun <T> List<T>.indexOfFirstOrNull(
    predicate: (T) -> Boolean
): Int? {
    val index = indexOfFirst(predicate)
    return if (index == -1) null else index
}