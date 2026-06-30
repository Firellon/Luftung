package dev.macfi.luftung.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.macfi.luftung.refresh.RefreshCoordinator

class VentilationWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            VentilationWidgetContent(WidgetStateStore(context).read())
        }
    }
}

class VentilationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VentilationWidget()
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        RefreshCoordinator(context).refresh()
    }
}

@Composable
private fun VentilationWidgetContent(state: WidgetDisplayState) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(state.backgroundColor()))
            .padding(12.dp)
            .clickable(actionRunCallback<RefreshWidgetAction>()),
    ) {
        Column {
            Text(
                text = state.title,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF12372A)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = state.indoorLine,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF274D40)),
                    fontSize = 13.sp,
                ),
            )
            Text(
                text = state.outdoorLine,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF274D40)),
                    fontSize = 12.sp,
                ),
            )
            Text(
                text = state.reason,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF274D40)),
                    fontSize = 12.sp,
                ),
            )
            Text(
                text = "Updated ${state.lastUpdated}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF48685C)),
                    fontSize = 11.sp,
                ),
            )
        }
    }
}

private fun WidgetDisplayState.backgroundColor(): Color {
    return when (status) {
        WidgetStatus.HELPFUL -> Color(0xFFE8F5E9)
        WidgetStatus.MIXED -> Color(0xFFFFF8E1)
        WidgetStatus.NOT_HELPFUL -> Color(0xFFFFEBEE)
        WidgetStatus.COLD_OR_DRY -> Color(0xFFE3F2FD)
        WidgetStatus.NEEDS_INPUT -> Color(0xFFE3F2FD)
        WidgetStatus.ERROR -> Color(0xFFF2F2F2)
    }
}
