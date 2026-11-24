package com.pronotes.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.pronotes.app.data.Repository

class NotebookWidget : AppWidgetProvider() {
    companion object {
        private const val PINNED = "com.pronotes.app.NOTEBOOK_WIDGET_PINNED"

        fun pin(context: Context, noteId: String) {
            val manager = AppWidgetManager.getInstance(context)
            val callback = Intent(context, NotebookWidget::class.java).apply {
                action = PINNED
                data = Uri.Builder().scheme("pronotes-widget").authority("notebook").appendPath(noteId).build()
            }
            val mutable = if (android.os.Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, 0, callback, mutable or PendingIntent.FLAG_UPDATE_CURRENT)
            manager.requestPinAppWidget(ComponentName(context, NotebookWidget::class.java), null, pending)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PINNED) {
            super.onReceive(context, intent)
            return
        }
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val note = intent.data?.lastPathSegment ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        context.getSharedPreferences("notebookWidgets", Context.MODE_PRIVATE).edit().putString(id.toString(), note).apply()
        onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(id))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences("notebookWidgets", Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { editor.remove(it.toString()) }
        editor.apply()
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("notebookWidgets", Context.MODE_PRIVATE)
        val notebooks = Repository.get(context).library.value.notebooks
        for (widgetId in appWidgetIds) {
            val target = prefs.getString(widgetId.toString(), null)
            val book = notebooks.firstOrNull { it.id == target && it.deletedAt == null }
            fun activity(action: String): PendingIntent {
                val intent = Intent(context, MainActivity::class.java).setAction(action).apply {
                    putExtra("notebookId", book?.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                return PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            val views = RemoteViews(context.packageName, R.layout.widget_notebook).apply {
                setTextViewText(R.id.widget_title, book?.title ?: if (target == null) "Resume notebook" else "Open library")
                setOnClickPendingIntent(R.id.widget_root, activity(if (book != null) "open_notebook" else if (target == null) "resume_notebook" else "open_library"))
                setOnClickPendingIntent(R.id.widget_library, activity("open_library"))
                setOnClickPendingIntent(R.id.widget_subtitle, activity("quick_note"))
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
