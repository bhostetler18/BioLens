package com.uf.biolens.utility

fun <E> MutableSet<E>.toggle(element: E) {
    if (contains(element)) {
        remove(element)
    } else {
        add(element)
    }
}
