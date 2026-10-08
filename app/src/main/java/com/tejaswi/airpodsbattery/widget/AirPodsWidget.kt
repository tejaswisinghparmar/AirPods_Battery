package com.tejaswi.airpodsbattery.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tejaswi.airpodsbattery.AppPrefs
import com.tejaswi.airpodsbattery.MainActivity
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AirPodsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent(AppPrefs.lastBattery(context)) }
    }

    companion object {
        fun refresh(context: Context) {
            CoroutineScope(Dispatchers.Default).launch {
                AirPodsWidget().updateAll(context.applicationContext)
            }
        }
    }
}

class AirPodsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AirPodsWidget()
}

private val WidgetBg = ColorProvider(Color(0xFF111116))
private val WidgetText = ColorProvider(Color(0xFFF5F5F7))
private val WidgetMuted = ColorProvider(Color(0xFFA7A7B2))

@androidx.compose.runtime.Composable
private fun WidgetContent(battery: AirPodsBattery?) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(WidgetBg).clickable(actionStartActivity<MainActivity>()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(battery?.deviceName?.takeIf { it.isNotBlank() } ?: "AirPods 3", style = TextStyle(color = WidgetText, fontSize = 14.sp))
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 5.dp), horizontalAlignment = Alignment.Horizontal.Start) {
            Text("L ${battery?.left?.let { "$it%" } ?: "--"}", style = TextStyle(color = WidgetText, fontSize = 13.sp))
            Text("   R ${battery?.right?.let { "$it%" } ?: "--"}", style = TextStyle(color = WidgetText, fontSize = 13.sp))
            Text("   C ${battery?.case?.let { "$it%" } ?: "--"}", style = TextStyle(color = WidgetMuted, fontSize = 13.sp))
        }
    }
}
