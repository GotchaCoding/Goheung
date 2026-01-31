package com.example.goheung.model

import com.example.goheung.constants.Constants

data class TitleModel(
    val title: String,
    override val viewType: Int = Constants.VIEW_TYPE_TITLE,
    override val id: Long = Constants.KEY_TITLE_MODEL_ID
) : ItemModel(id, viewType)
