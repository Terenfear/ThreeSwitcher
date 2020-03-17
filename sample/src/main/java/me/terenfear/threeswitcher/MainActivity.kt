package me.terenfear.threeswitcher

import android.graphics.Typeface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        threeSwitcher.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))

        text.text = "From initial view state: ${threeSwitcher.state}"
        threeSwitcher.onStateChangedListener = { v, s ->
            text.text = "From change listener: $s"
        }

        curStBtn.setOnClickListener {
            text.text = "From current view state: ${threeSwitcher.state}"
        }

        leftBtn.setOnClickListener {
            threeSwitcher.setState(ThreeSwitcher.State.LEFT)
        }
        rightBtn.setOnClickListener {
            threeSwitcher.setState(ThreeSwitcher.State.RIGHT)
        }

        leftNEBtn.setOnClickListener {
            threeSwitcher.setStateNoEvent(ThreeSwitcher.State.LEFT)
        }
        rightNEBtn.setOnClickListener {
            threeSwitcher.setStateNoEvent(ThreeSwitcher.State.RIGHT)
        }

        leftImBtn.setOnClickListener {
            threeSwitcher.setStateImmediately(ThreeSwitcher.State.LEFT)
        }
        rightImBtn.setOnClickListener {
            threeSwitcher.setStateImmediately(ThreeSwitcher.State.RIGHT)
        }

        leftNEImBtn.setOnClickListener {
            threeSwitcher.setStateNoEventImmediately(ThreeSwitcher.State.LEFT)
        }
        rightNEImBtn.setOnClickListener {
            threeSwitcher.setStateNoEventImmediately(ThreeSwitcher.State.RIGHT)
        }
    }
}
