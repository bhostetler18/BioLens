package com.uf.biolens.utility

import android.view.View

fun View.setPadding(
    left: Int? = null,
    right: Int? = null,
    top: Int? = null,
    bottom: Int? = null
) {
    setPadding(
        left ?: paddingLeft,
        top ?: paddingTop,
        right ?: paddingRight,
        bottom ?: paddingBottom
    )
}
