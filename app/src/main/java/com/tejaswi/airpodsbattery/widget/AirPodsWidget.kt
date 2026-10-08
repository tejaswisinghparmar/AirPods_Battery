package com.tejaswi.airpodsbattery.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejaswi.airpodsbattery.AppPrefs
import com.tejaswi.airpodsbattery.MainActivity
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AirPodsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val battery = AppPrefs.lastBattery(context)
        provideContent {
            WidgetContent(battery)
        }
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

@Composable
private fun WidgetContent(battery: AirPodsBattery?) {
    val bg = ColorProvider(Color(0xFF111116))
    val primary = ColorProvider(Color(0xFFF5F5F7))
    val secondary = ColorProvider(Color(0xFFA7A7B2))
    val accent = ColorProvider(Color(0xFFA78BFA))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF111116))
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text("AirPods 3", style = TextStyle(color = primary, fontSize = 17.sp))
            Spacer(GlanceModifier.width(8.dp))
            Text("●", style = TextStyle(color = accent, fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetBattery("L", battery?.left, battery?.leftCharging, primary, secondary)
            Spacer(GlanceModifier.width(12.dp))
            WidgetBattery("R", battery?.right, battery?.rightCharging, primary, secondary)
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text("Case", style = TextStyle(color = secondary, fontSize = 13.sp))
            Spacer(GlanceModifier.width(8.dp))
            Text(
                battery?.case?.let { "$it%" } ?: "—",
                style = TextStyle(color = primary, fontSize = 16.sp)
            )
            if (battery?.caseCharging == true) {
                Spacer(GlanceModifier.width(5.dp))
                Text("⚡", style = TextStyle(color = accent, fontSize = 12.sp))
            }
        }
    }
}

@Composable
private fun WidgetBattery(
    label: String,
    value: Int?,
    charging: Boolean?,
    primary: ColorProvider,
    secondary: ColorProvider
) {
    Column {
        Text(label, style = TextStyle(color = secondary, fontSize = 12.sp))
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(value?.let { "$it%" } ?: "—", style = TextStyle(color = primary, fontSize = 20.sp))
            if (charging == true) {
                Spacer(GlanceModifier.width(4.dp))
                Text("⚡", style = TextStyle(color = primary, fontSize = 11.sp))
            }
        }
    }
}
