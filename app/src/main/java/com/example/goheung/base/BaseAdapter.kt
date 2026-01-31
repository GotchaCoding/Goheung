package com.example.goheung.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.goheung.R
import com.example.goheung.constants.Constants
import com.example.goheung.databinding.ItemFailBinding
import com.example.goheung.model.FailModel
import com.example.goheung.model.ItemModel

abstract class BaseAdapter : ListAdapter<ItemModel, RecyclerView.ViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            Constants.VIEW_TYPE_FAIL -> {
                val binding: ItemFailBinding =
                    DataBindingUtil.inflate(inflater, R.layout.item_fail, parent, false)
                FailViewHolder(binding)
            }
            Constants.VIEW_TYPE_LOADING -> {
                val itemView = inflater.inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(itemView)
            }
            Constants.VIEW_TYPE_EMPTY -> {
                val itemView = inflater.inflate(R.layout.item_empty, parent, false)
                EmptyViewHolder(itemView)
            }
            else -> {
                throw Exception("Unknown ViewType: $viewType")
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].viewType
    }

    class FailViewHolder(val binding: ItemFailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setItem(item: FailModel) {
            binding.item = item
        }
    }

    class LoadingViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)

    class EmptyViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<ItemModel>() {
            override fun areItemsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
