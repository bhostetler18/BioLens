package com.uf.biolens.ui.imageGrid

import androidx.lifecycle.MutableLiveData
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Image
import com.uf.biolens.utility.toggle

interface ImageSelector {
    fun isSelected(image: Image): Boolean
    fun toggle(image: Image)
    fun setSelected(image: Image, isSelected: Boolean)

    fun toggleEditing()
    val isEditing: Boolean
}

data class ImageDisplayData(var numImages: Int, var skipCount: Int, var numSelected: Int)

class BioLensImageSelector : ImageSelector {

    var isEditingLiveData = MutableLiveData(false)
    override val isEditing: Boolean get() = isEditingLiveData.value ?: false

    private var selectedIndices = HashSet<Long>()
    var numSelected = MutableLiveData<Int>(0)

    var exitOnNoSelection = true

    override fun toggleEditing() {
        setEditing(!isEditing)
    }

    private fun setEditing(isEditing: Boolean) {
        isEditingLiveData.value = isEditing
        if (!isEditing) {
            selectedIndices.clear()
            numSelected.value = 0
        }
    }

    override fun toggle(image: Image) = mutateSelection {
        selectedIndices.toggle(image.imageID)
    }

    override fun setSelected(image: Image, isSelected: Boolean): Unit = mutateSelection {
        if (isSelected) {
            selectedIndices.add(image.imageID)
        } else {
            selectedIndices.remove(image.imageID)
        }
    }

    override fun isSelected(image: Image): Boolean {
        return selectedIndices.contains(image.imageID)
    }

    fun deleteSelectedImages() = mutateSelection {
        selectedIndices.forEach {
            BioLensRepository.deleteImage(it)
        }
        selectedIndices.clear()
    }

    private fun <T> mutateSelection(block: BioLensImageSelector.() -> T): T {
        val ret = block()
        val count = selectedIndices.size
        if (exitOnNoSelection && count == 0) {
            setEditing(false)
        }
        numSelected.value = count
        return ret
    }
}
