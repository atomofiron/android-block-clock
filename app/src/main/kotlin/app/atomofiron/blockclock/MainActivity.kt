package app.atomofiron.blockclock

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import app.atomofiron.blockclock.settings.SettingsScreen
import app.atomofiron.blockclock.ui.theme.AppTheme
import app.atomofiron.blockclock.update.UpdateService
import app.atomofiron.blockclock.update.UpdateStore
import app.atomofiron.blockclock.util.collect
import app.atomofiron.blockclock.util.get

class MainActivity : AppCompatActivity() {

    private var isEnterAnimationCompleted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        UpdateService.self.onActivityCreate(this)
        UpdateStore.self.alerts.collect(lifecycleScope) {
            Toast.makeText(this, resources[it.text], Toast.LENGTH_LONG).show()
        }

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

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) UpdateService.self.completeUpdate()
    }
}
