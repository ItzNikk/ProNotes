package com.pronotes.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NotebookWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.notebook_widget)
        for ((index, target) in listOf(R.id.widget_library, R.id.widget_recent).withIndex()) {
            val intent = Intent(context, MainActivity::class.java).setAction(if (index == 0) "open_library" else "resume_notebook")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            views.setOnClickPendingIntent(target, PendingIntent.getActivity(context, index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
