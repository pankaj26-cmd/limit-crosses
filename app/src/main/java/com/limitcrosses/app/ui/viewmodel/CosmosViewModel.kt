package com.limitcrosses.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import com.limitcrosses.app.core.model.CosmosCategory
import com.limitcrosses.app.core.model.CosmosTopic
import com.limitcrosses.app.domain.repository.CosmosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CosmosViewModel(
    private val cosmosRepository: CosmosRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CosmosCategory.ALL)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _topics = MutableStateFlow<List<CosmosTopic>>(emptyList())
    val topics = _topics.asStateFlow()

    init {
        loadCategory(CosmosCategory.ALL)
    }

    fun selectCategory(category: CosmosCategory) {
        _selectedCategory.value = category
        loadCategory(category)
    }

    private fun loadCategory(category: CosmosCategory) {
        _topics.value = cosmosRepository.getTopics(category)
    }

    fun openSourceArticle(context: Context, url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }
}

