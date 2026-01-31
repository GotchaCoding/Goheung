package com.example.goheung.presentation.adapter

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("imageUrl")
    fun bindImageUrl(view: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return
        Glide.with(view.context)
            .load(imageUrl)
            .circleCrop()
            .into(view)
    }

    @JvmStatic
    @BindingAdapter("visibility")
    fun bindVisibility(view: View, isVisible: Boolean) {
        view.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
