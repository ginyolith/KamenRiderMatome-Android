package jp.ginyolith.kamen_rider_matome

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import jp.ginyolith.kamen_rider_matome.databinding.MainActivityBinding
import jp.ginyolith.kamen_rider_matome.ui.main.MainFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding : MainActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }

        setSupportActionBar(binding.toolbar)
    }

}
