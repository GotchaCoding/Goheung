package com.example.goheung.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.goheung.model.ItemModel

open class BaseViewModel : ViewModel() {
    protected val _items = MutableLiveData<List<ItemModel>>()
    val items: LiveData<List<ItemModel>> = _items
}
