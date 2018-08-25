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
import jp.ginyolith.kamen_rider_matome.data.RSSDatabase
import jp.ginyolith.kamen_rider_matome.data.article.Article
import jp.ginyolith.kamen_rider_matome.data.blog.ArticlesDataSource
import jp.ginyolith.kamen_rider_matome.data.blog.ArticlesRepository
import jp.ginyolith.kamen_rider_matome.data.blog.BlogsRepository
import jp.ginyolith.kamen_rider_matome.databinding.MainFragmentBinding
import jp.ginyolith.kamen_rider_matome.databinding.RowMatomeListBinding

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding
    private lateinit var blogsRepository: BlogsRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        blogsRepository = BlogsRepository.getInstance(requireContext())

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

        val refreshUiList : (List<Article>) -> Unit = {
            activity?.runOnUiThread {
                binding.matomeList.adapter = ArticleListAdapter(it, context)

                // 縦軸のリストと設定する
                binding.matomeList.layoutManager = LinearLayoutManager(context)

                // Listに区切り線を入れる
                binding.matomeList.setBorder(true)
            }
        }

        Thread(Runnable {
            val database = RSSDatabase.getInstance(requireContext())
            val articleDao = database.articleDao()
            val blogDao = database.blogDao()
            val blogs = blogDao.selectAll()
            val articles = articleDao.selectAll().apply { forEach { article ->
                article.blog = blogs.firstOrNull { it._id == article.blogId }!!
            } }

            if (articles.isEmpty()) {
                refreshArticleList()
            }


        }).start()
    }

    /**
     * リモートからRSS情報を読み込み、ローカルに格納。UIに反映する
     */
    private fun refreshArticleList(callback : () -> Unit = {}) {
        val refreshList = { articles : List<Article> ->
            activity?.runOnUiThread {
                binding.matomeList.adapter = ArticleListAdapter(articles, context)

                // 縦軸のリストと設定する
                binding.matomeList.layoutManager = LinearLayoutManager(context)

                // Listに区切り線を入れる
                binding.matomeList.setBorder(true)

                callback()
            }
        }

        ArticlesRepository.getInstance(requireContext()).getArticles(object : ArticlesDataSource.Callback{
            override fun onNoNewArticle() {
                activity?.toast("新着の記事はありません。")
                activity?.runOnUiThread{callback()}

            }

            override fun onAllSuccess(articles: List<Article>) {
                refreshList(articles)
            }

            override fun onSomeFaild(articles: List<Article>, errors: List<ArticlesDataSource.Error>) {
                refreshList(articles)
            }

            override fun onAllFaild(errors: List<ArticlesDataSource.Error>) {

            }

        })
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
