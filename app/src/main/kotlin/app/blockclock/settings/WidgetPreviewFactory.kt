package app.blockclock.settings

import android.graphics.Bitmap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.blockclock.ui.values.Padding
import app.blockclock.widget.DateOnlyWidget
import app.blockclock.widget.OneLevelWidget
import app.blockclock.widget.Structure
import app.blockclock.widget.ThreeLevelWidget
import app.blockclock.widget.TimeOnlyWidget
import app.blockclock.widget.TwoLevelWidget
import app.blockclock.widget.WidgetSettings
import app.blockclock.widget.resolve
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/*
adb shell run-as app.atomofiron.blockclock.debug cp files/preview_one_level.png files/preview_two_level.png files/preview_three_level.png files/preview_time_only.png files/preview_date_only.png /sdcard/previews/
adb pull /sdcard/previews
*/

private val Names = listOf("preview_one_level", "preview_two_level", "preview_three_level", "preview_time_only", "preview_date_only")

@Composable
fun ColumnScope.WidgetPreviewFactory(settings: WidgetSettings) {
    val graphicsLayers = listOf(
        rememberGraphicsLayer(),
        rememberGraphicsLayer(),
        rememberGraphicsLayer(),
        rememberGraphicsLayer(),
        rememberGraphicsLayer(),
    )
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Padding.Half)
            .padding(vertical = Padding.Common),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val gap = settings.gapDp.dp
        GlanceWidgetPreview(
            modifier = Modifier
                .padding(horizontal = Padding.Half)
                .drawWithContent {
                    graphicsLayers[0].record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayers[0])
                },
            widget = OneLevelWidget(settings),
            size = Structure.OneLevel.resolve(DpSize(previewWidth(), 100.dp), gap).second,
        )
        GlanceWidgetPreview(
            modifier = Modifier
                .padding(horizontal = Padding.Half)
                .drawWithContent {
                    graphicsLayers[1].record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayers[1])
                },
            widget = TwoLevelWidget(settings),
            size = Structure.TwoLevel.resolve(DpSize(182.dp, 120.dp), gap).second,
        )
        GlanceWidgetPreview(
            modifier = Modifier
                .padding(horizontal = Padding.Half)
                .drawWithContent {
                    graphicsLayers[2].record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayers[2])
                },
            widget = ThreeLevelWidget(settings),
            size = Structure.ThreeLevel.resolve(DpSize(182.dp, 200.dp), gap).second,
        )
        GlanceWidgetPreview(
            modifier = Modifier
                .padding(horizontal = Padding.Half)
                .drawWithContent {
                    graphicsLayers[3].record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayers[3])
                },
            widget = TimeOnlyWidget(settings),
            size = Structure.TimeOnly.resolve(DpSize(182.dp, 91.dp), gap).second,
        )
        GlanceWidgetPreview(
            modifier = Modifier
                .padding(horizontal = Padding.Half)
                .drawWithContent {
                    graphicsLayers[4].record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayers[4])
                },
            widget = DateOnlyWidget(settings),
            size = Structure.DateOnly.resolve(DpSize(182.dp, 91.dp), gap).second,
        )
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        shape = ShapeDefaults.Medium,
        onClick = {
            scope.launch {
                try {
                    graphicsLayers.forEachIndexed { i, layer ->
                        val bitmap = layer.toImageBitmap().asAndroidBitmap()
                        val file = File(context.filesDir, "previews/${Names[i]}.png")
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    ) {
        Text("Save to PNGs")
    }
}
