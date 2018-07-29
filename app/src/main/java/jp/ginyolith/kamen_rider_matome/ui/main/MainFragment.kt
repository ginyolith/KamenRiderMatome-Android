package jp.ginyolith.kamen_rider_matome.ui.main

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import jp.ginyolith.kamen_rider_matome.R
import jp.ginyolith.kamen_rider_matome.data.Article
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import jp.ginyolith.kamen_rider_matome.databinding.MainFragmentBinding
import jp.ginyolith.kamen_rider_matome.databinding.RowMatomeListBinding

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
            val articles = HttpAccess().getArticles("https://kamen-rider-matome.firebaseapp.com/test_rss.txt")
            activity?.runOnUiThread {
                binding.matomeList.adapter = ArticleListAdapter(articles, context)

                // 縦軸のリストと設定する
                binding.matomeList.layoutManager = LinearLayoutManager(context)

                // Listに区切り線を入れる
                val dividerItemDecoration = DividerItemDecoration(
                        context,
                        LinearLayoutManager(context).orientation)
                binding.matomeList.addItemDecoration(dividerItemDecoration)
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
            dataList[position].run {
                holder.binding.article = this
                Glide.with(context).load(thumbnailUrl).into(holder.binding.imageRowMatome)
            }
        }

        class BindingHolder(var binding : RowMatomeListBinding) : RecyclerView.ViewHolder(binding.root)
    }

}
