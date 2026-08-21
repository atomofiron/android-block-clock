package app.atomofiron.blockclock

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import app.atomofiron.blockclock.settings.SettingsScreen
import app.atomofiron.blockclock.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    private var isEnterAnimationCompleted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                SettingsScreen(isEnterAnimationCompleted.value)
            }
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        isEnterAnimationCompleted.value = true
    }

    override fun onStop() {
        super.onStop()
        isEnterAnimationCompleted.value = false
    }
}
