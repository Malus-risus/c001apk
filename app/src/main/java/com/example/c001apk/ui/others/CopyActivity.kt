package com.example.c001apk.ui.others

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.c001apk.databinding.ActivityCopyBinding
import com.example.c001apk.logic.database.HomeMenuDatabase
import com.example.c001apk.logic.model.HomeMenu
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.ui.home.HomeMenuAdapter
import com.example.c001apk.ui.home.ItemTouchHelperCallback
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern


class CopyActivity : BaseActivity<ActivityCopyBinding>() {

    private lateinit var mAdapter: HomeMenuAdapter
    private val menuList: MutableList<HomeMenu> = mutableListOf()
    private val homeMenuDao by lazy {
        HomeMenuDatabase.getDatabase(this).homeMenuDao()
    }

    private fun getAllLinkAndText(str: String?): String {
        return if (TextUtils.isEmpty(str)) "" else
            Pattern.compile("<a class=\"feed-link-url\"\\s+href=\"([^<>\"]*)\"[^<]*[^>]*>")
                .matcher(str).replaceAll(" $1 ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        processIncomingText()
        handleHomeMenuIntent()
        setupDoneButton()
    }

    private fun processIncomingText() {
        intent.getStringExtra("text")?.let {
            val linkText = getAllLinkAndText(it)
            binding.textView.text = Html.fromHtml(
                linkText.replace("\n", " <br/>"),
                Html.FROM_HTML_MODE_COMPACT
            ).toString()
        }
    }

    private fun handleHomeMenuIntent() {
        intent.getStringExtra("type")?.let { type ->
            if (type == "homeMenu") {
                binding.done.visibility = View.VISIBLE
                loadHomeMenu()
            }
        }
    }

    private fun setupDoneButton() {
        binding.done.setOnClickListener {
            saveHomeMenu()
        }
    }

    private fun loadHomeMenu() {
        lifecycleScope.launch(Dispatchers.IO) {
            menuList.addAll(homeMenuDao.loadAll())
            withContext(Dispatchers.Main) {
                initView()
            }
        }
    }

    private fun saveHomeMenu() {
        lifecycleScope.launch(Dispatchers.IO) {
            homeMenuDao.deleteAll()
            homeMenuDao.insertAll(menuList.mapIndexed { index, menu ->
                HomeMenu(index, menu.title, menu.isEnable)
            })
            withContext(Dispatchers.Main) {
                restartApp()
            }
        }
    }

    private fun initView() {
        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexboxLayoutManager.FlexDirection.ROW
            flexWrap = FlexboxLayoutManager.FlexWrap.WRAP
        }
        mAdapter = HomeMenuAdapter(menuList)
        binding.recyclerView.apply {
            adapter = mAdapter
            this.layoutManager = layoutManager
            ItemTouchHelper(ItemTouchHelperCallback(mAdapter)).attachToRecyclerView(this)
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

}
