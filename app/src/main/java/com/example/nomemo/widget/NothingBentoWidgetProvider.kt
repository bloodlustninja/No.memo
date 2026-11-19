package com.example.nomemo.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Nothing OS 5.0 Physical "Glyph Matrix" Homescreen Widget Receiver.
 *
 * Implements GlanceAppWidgetReceiver to bind NothingBentoGlanceWidget to
 * the Android homescreen widget manager with full backwards compatibility.
 */
class NothingBentoWidgetProvider : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NothingBentoGlanceWidget()

    companion object {
        const val ACTION_VOICE_CAPTURE = "com.example.nomemo.ACTION_VOICE_CAPTURE"

        /**
         * Triggers a reactive update across all placed Glance bento widgets.
         */
        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NothingBentoGlanceWidget().updateAll(context.applicationContext)
                } catch (_: Exception) {}
            }
        }
    }
}
