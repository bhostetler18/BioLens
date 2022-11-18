package com.uf.automoth.data.export

import com.uf.automoth.data.Image

interface SessionCSVFormatter {
    fun getHeader(): List<String>
    fun getRow(image: Image): List<String>
}

data class BasicSessionFormatterColumn(
    val name: String,
    val getter: (Image) -> String
)

abstract class BasicSessionCSVFormatter : SessionCSVFormatter {
    private var columns = ArrayList<BasicSessionFormatterColumn>()

    fun addColumn(name: String, getter: (Image) -> String) {
        columns.add(BasicSessionFormatterColumn(name, getter))
    }

    fun addConstantColumn(name: String, value: String) {
        columns.add(BasicSessionFormatterColumn(name) { value })
    }

    override fun getHeader(): List<String> {
        return columns.map {
            it.name
        }
    }

    override fun getRow(image: Image): List<String> {
        return columns.map {
            it.getter(image)
        }
    }
}
