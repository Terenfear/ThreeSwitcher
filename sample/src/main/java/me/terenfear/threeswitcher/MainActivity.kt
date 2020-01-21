package me.terenfear.threeswitcher

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        threeSwitcher.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

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
        centerBtn.setOnClickListener {
            threeSwitcher.setState(ThreeSwitcher.State.CENTER)
        }
        rightBtn.setOnClickListener {
            threeSwitcher.setState(ThreeSwitcher.State.RIGHT)
        }

        leftNEBtn.setOnClickListener {
            threeSwitcher.setStateNoEvent(ThreeSwitcher.State.LEFT)
        }
        centerNEBtn.setOnClickListener {
            threeSwitcher.setStateNoEvent(ThreeSwitcher.State.CENTER)
        }
        rightNEBtn.setOnClickListener {
            threeSwitcher.setStateNoEvent(ThreeSwitcher.State.RIGHT)
        }

        leftImBtn.setOnClickListener {
            threeSwitcher.setStateImmediately(ThreeSwitcher.State.LEFT)
        }
        centerImBtn.setOnClickListener {
            threeSwitcher.setStateImmediately(ThreeSwitcher.State.CENTER)
        }
        rightImBtn.setOnClickListener {
            threeSwitcher.setStateImmediately(ThreeSwitcher.State.RIGHT)
        }

        leftNEImBtn.setOnClickListener {
            threeSwitcher.setStateNoEventImmediately(ThreeSwitcher.State.LEFT)
        }
        centerNEImBtn.setOnClickListener {
            threeSwitcher.setStateNoEventImmediately(ThreeSwitcher.State.CENTER)
        }
        rightNEImBtn.setOnClickListener {
            threeSwitcher.setStateNoEventImmediately(ThreeSwitcher.State.RIGHT)
        }
    }
}
