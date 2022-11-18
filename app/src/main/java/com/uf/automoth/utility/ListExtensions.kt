package com.uf.automoth.utility

fun <E> List<E>.indexOfOrNull(element: E): Int? = indexOfOrNull(List<E>::indexOf, element)
fun <E> List<E>.indexOfFirstOrNull(predicate: (E) -> Boolean): Int? =
    indexOfOrNull(List<E>::indexOfFirst, predicate)

fun <E, T> List<E>.indexOfOrNull(f: List<E>.(T) -> Int, arg: T): Int? {
    val index = this.f(arg)
    return if (index == -1) null else index
}
