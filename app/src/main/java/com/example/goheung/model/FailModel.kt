package com.example.goheung.model

import com.example.goheung.constants.Constants

data class FailModel(
    val message: String = "",
    override val id: Long = Constants.KEY_FAIL_MODEL_ID,
    override val viewType: Int = Constants.VIEW_TYPE_FAIL
) : ItemModel(id, viewType)
