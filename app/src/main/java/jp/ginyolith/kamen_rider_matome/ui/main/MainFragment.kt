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
import com.bumptech.glide.Glide
import jp.ginyolith.kamen_rider_matome.R
import jp.ginyolith.kamen_rider_matome.WebViewActivity
import jp.ginyolith.kamen_rider_matome.data.Article
import jp.ginyolith.kamen_rider_matome.data.Blog
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import jp.ginyolith.kamen_rider_matome.data.RSSDatabase
import jp.ginyolith.kamen_rider_matome.databinding.MainFragmentBinding
import jp.ginyolith.kamen_rider_matome.databinding.RowMatomeListBinding
import jp.ginyolith.kamen_rider_matome.setBorder

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        return binding.root
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

        Thread(Runnable {
            val database = RSSDatabase.getInstance(requireContext())
            val articleDao = database.articleDao()
            val blogDao = database.blogDao()
            val initedBlogs = blogDao.selectAll()
            val access = HttpAccess()

            database.runInTransaction {
                // 初期化済み
                initedBlogs.flatMap {blog ->
                    access.getArticles(blog.enum.getFeedUrl()).apply { forEach { it.blog = blog } }
                } .filter {
                    // TODO test and modelに処理移譲
                    it.pubDate > it.blog.lastUpdateDate
                }.toList().let {
                    articleDao.insert(*it.toTypedArray())
                }

                // 初期化
                Blog.Enum.values().filterNot {
                    initedBlogs.map(Blog::enum).contains(it)
                } .map {
                    val blog = access.getBlogInfoFromRSSFeed(it.getFeedUrl())
                    blogDao.insert(blog)
                    blog
                } .flatMap {blog -> Blog
                    access.getArticles(blog.enum.getFeedUrl()).apply { forEach { it.blog = blog } }
                } .toList().let {
                    articleDao.insert(*it.toTypedArray())
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
            }
        }).start()
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
                Glide.with(context).load(article.thumbnailUrl).into(holder.binding.imageRowMatome)

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

}
