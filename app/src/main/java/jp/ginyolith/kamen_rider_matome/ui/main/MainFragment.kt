package jp.ginyolith.kamen_rider_matome.ui.main

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.ginyolith.kamen_rider_matome.*
import jp.ginyolith.kamen_rider_matome.data.Article
import jp.ginyolith.kamen_rider_matome.data.Blog
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import jp.ginyolith.kamen_rider_matome.data.RSSDatabase
import jp.ginyolith.kamen_rider_matome.databinding.MainFragmentBinding
import jp.ginyolith.kamen_rider_matome.databinding.RowMatomeListBinding
import okhttp3.Call
import java.io.IOException
import java.util.concurrent.CountDownLatch

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)

        binding.swipeRefreshMain.setOnRefreshListener {
            Thread(Runnable {
                refreshArticleList {
                    // 処理終了時、更新をやめる
                    if (binding.swipeRefreshMain.isRefreshing) {
                        binding.swipeRefreshMain.isRefreshing = false
                    }
                }
            }).start()
        }

        return binding.root
    }

    fun scrollToTop() {
        binding.matomeList.scrollToPosition(0)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Thread(Runnable {
            val database = RSSDatabase.getInstance(requireContext())
            val articleDao = database.articleDao()
            val blogDao = database.blogDao()
            val blogs = blogDao.selectAll()
            val articles = articleDao.selectAll().apply { forEach { article ->
                article.blog = blogs.firstOrNull { it._id == article.blogId }!!
            } }

            activity?.runOnUiThread {
                binding.matomeList.adapter = ArticleListAdapter(articles, context)

                // 縦軸のリストと設定する
                binding.matomeList.layoutManager = LinearLayoutManager(context)

                // Listに区切り線を入れる
                binding.matomeList.setBorder(true)
            }
        }).start()
    }

    /**
     * リモートからRSS情報を読み込み、ローカルに格納。UIに反映する
     */
    private fun refreshArticleList(callback : () -> Unit) {
        val database = RSSDatabase.getInstance(requireContext())
        val articleDao = database.articleDao()
        val blogDao = database.blogDao()
        val initedBlogs = blogDao.selectAll()
        val access = HttpAccess()

        database.runInTransaction {
            // 最新のブログ情報を取得
            // ブログの最終更新日を更新
            val latestBlogs = getLatestBlogInfo(access).filterNotNull()
            blogDao.insertOrUpdate(*latestBlogs.toTypedArray())

            // 初期化済みのブログの記事を取得してdbにinsert
            val insertArticles = ArrayList<Article>()
            insertArticles.addAll(getInitedArticleList(initedBlogs,access))

            // 初期化
            insertArticles.addAll(getNotInitedArticles(latestBlogs, initedBlogs, access))

            if (insertArticles.isEmpty()) {
                activity?.toast("新着の記事はありません。")
            } else {
                articleDao.insert(*insertArticles.toTypedArray())
            }
        }

        val blogs = blogDao.selectAll()
        val articles = articleDao.selectAll().apply { forEach { article ->
            article.blog = blogs.firstOrNull { it._id == article.blogId }!!
        } }

        activity?.runOnUiThread {
            binding.matomeList.adapter = ArticleListAdapter(articles, context)

            // 縦軸のリストと設定する
            binding.matomeList.layoutManager = LinearLayoutManager(context)

            // Listに区切り線を入れる
            binding.matomeList.setBorder(true)

            callback()
        }
    }

    fun getInitedArticleList(initedBlogs : List<Blog>, access : HttpAccess): ArrayList<Article> {
        val latch = CountDownLatch(initedBlogs.size)
        val initedBlogArticles = ArrayList<Article>()
        val onFailure : (Call?, IOException?) -> Unit = { _, e ->
            activity?.toast("リクエストに失敗しました。")
            latch.countDown()
        }
        // 初期化済み
        initedBlogs.forEach { blog ->
            access.getArticles(blog.enum.getFeedUrl(), onFailure) { _, _, list ->
                requireNotNull(list).apply {
                    forEach { it.blog = blog }
                }.filter {
                    // TODO test and modelに処理移譲
                    it.pubDate > it.blog.lastUpdateDate
                }.toList().let {
                    initedBlogArticles.addAll(it)
                }
                latch.countDown()
            }
        }
        latch.await()
        return initedBlogArticles
    }

    fun getLatestBlogInfo(access : HttpAccess): ArrayList<Blog?> {
        val latch = CountDownLatch(Blog.Enum.values().size)
        val latestBlogs = ArrayList<Blog?>()
        val onFailure : (Call?, IOException?) -> Unit = { _, e ->
            activity?.toast("リクエストに失敗しました。")
            latch.countDown()
        }

        Blog.Enum.values().map {
            access.getBlogInfoFromRSSFeed(it.getFeedUrl(), onFailure) { _, _, blog ->
                latestBlogs.add(blog)
                latch.countDown()
            }
        }
        latch.await()

        return latestBlogs
    }

    fun getNotInitedArticles(latestBlogs : List<Blog>, initedBlogs: List<Blog>, access : HttpAccess): ArrayList<Article> {
        val notInitBlogds = latestBlogs.filterNot {
            initedBlogs.map(Blog::_id).contains(it?._id)
        }.toList()
        val latch = CountDownLatch(notInitBlogds.size)
        val articleList = ArrayList<Article>()
        val onFailure : (Call?, IOException?) -> Unit = { _, e ->
            activity?.toast("リクエストに失敗しました。")
            latch.countDown()
        }
        notInitBlogds.forEach { blog ->
            access.getArticles(requireNotNull(blog).enum.getFeedUrl(), onFailure) { _, _, list ->
                requireNotNull(list).run {
                    forEach { it.blog = requireNotNull(blog) }
                }

                articleList.addAll(requireNotNull(list))
                latch.countDown()
            }
        }

        latch.await()
        return articleList
    }
}

    class ArticleListAdapter(private val dataList : List<Article>, private val context : Context?)
        : RecyclerView.Adapter<ArticleListAdapter.BindingHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
            val inflater : LayoutInflater = LayoutInflater.from(parent.context)
            val binding = RowMatomeListBinding.inflate(inflater, parent, false)
            return BindingHolder(binding)
        }

        override fun getItemCount(): Int = dataList.size

        override fun onBindViewHolder(holder: BindingHolder, position: Int) {
            dataList[position].let {article ->
                // bindingに変数設定
                holder.binding.article = article

                GlideApp.with(context)
                        .load(article.thumbnailUrl)
                        .circleCrop()
                        .into(holder.binding.imageRowMatome)

                // Activity遷移
                holder.binding.layoutRowMatome.setOnClickListener {
                    val intent = Intent(context, WebViewActivity::class.java)
                    intent.putExtra("article", article)
                    context?.startActivity(intent)
                }
            }
        }

        class BindingHolder(var binding : RowMatomeListBinding) : RecyclerView.ViewHolder(binding.root)
    }
