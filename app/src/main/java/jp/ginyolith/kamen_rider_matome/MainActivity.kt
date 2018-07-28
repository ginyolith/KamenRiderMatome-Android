package jp.ginyolith.kamen_rider_matome

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import jp.ginyolith.kamen_rider_matome.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }
    }

}
