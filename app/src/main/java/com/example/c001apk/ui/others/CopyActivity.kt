package com.example.c001apk.ui.others

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.c001apk.databinding.ActivityCopyBinding
import com.example.c001apk.logic.database.HomeMenuDatabase
import com.example.c001apk.logic.model.HomeMenu
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.ui.home.HomeMenuAdapter
import com.example.c001apk.ui.home.ItemTouchHelperCallback
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class CopyActivity : BaseActivity<ActivityCopyBinding>() {

    private lateinit var mAdapter: HomeMenuAdapter
    private lateinit var mLayoutManager: FlexboxLayoutManager
    private var menuList: MutableList<HomeMenu> = mutableListOf()  // Using MutableList instead of ArrayList for Kotlin idiomatic
    private val homeMenuDao by lazy {
        HomeMenuDatabase.getDatabase(this).homeMenuDao()
    }

    private fun getAllLinkAndText(str: String?): String {
        if (TextUtils.isEmpty(str)) return ""
        val pattern = Pattern.compile("<a class=\"feed-link-url\"\\s+href=\"([^<>\"]*)\"[^<]*[^>]*>")
        return pattern.matcher(str).replaceAll(" $1 ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra("text")?.let {
            val linkText = getAllLinkAndText(it)
            binding.textView.text = Html.fromHtml(
                linkText.replace("\n", "<br />"),
                Html.FROM_HTML_MODE_COMPACT
            )
        }

        intent.getStringExtra("type")?.let { type ->
            if (type == "homeMenu") {
                binding.done.visibility = View.VISIBLE
                
                launch {
                    menuList = homeMenuDao.loadAll() as MutableList<HomeMenu>
                    initView()
                }
            }
        }

        binding.done.setOnClickListener {
            launch {
                saveMenuItems()
            }
        }
    }

    private fun initView() {
        mLayoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }
        mAdapter = HomeMenuAdapter(menuList)
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = mLayoutManager
            ItemTouchHelper(ItemTouchHelperCallback(mAdapter)).attachToRecyclerView(this)
        }
    }

    private suspend fun saveMenuItems() {
        withContext(Dispatchers.IO) {
            homeMenuDao.deleteAll()
            homeMenuDao.insertAll(menuList.mapIndexed { index, homeMenu ->
                HomeMenu(index, homeMenu.title, homeMenu.isEnable)
            })
        }
        
        withContext(Dispatchers.Main) {
            restartApp()
        }
    }

    private fun restartApp() {
        Intent.makeRestartActivityTask(componentName).also {
            startActivity(it)
        }
    }
}
