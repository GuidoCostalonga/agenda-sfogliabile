package it.guidocostalonga.agendasfogliabile.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import it.guidocostalonga.agendasfogliabile.MainActivity
import it.guidocostalonga.agendasfogliabile.R
import it.guidocostalonga.agendasfogliabile.util.dataConGiorno
import java.time.LocalDate
import java.time.ZoneId

/** Il riquadro per la schermata iniziale con gli impegni di oggi. */
class AgendaWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { disegna(context, manager, it) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.lista)
        programmaMezzanotte(context)
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(intentoAggiornamento(context))
    }

    private fun disegna(context: Context, manager: AppWidgetManager, id: Int) {
        val viste = RemoteViews(context.packageName, R.layout.widget_agenda)
        viste.setTextViewText(R.id.intestazione, LocalDate.now().dataConGiorno())

        val servizio = Intent(context, ServizioWidget::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        servizio.data = Uri.parse(servizio.toUri(Intent.URI_INTENT_SCHEME))
        @Suppress("DEPRECATION")
        viste.setRemoteAdapter(R.id.lista, servizio)
        viste.setEmptyView(R.id.lista, R.id.vuoto)

        val apri = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        viste.setOnClickPendingIntent(R.id.intestazione, apri)
        viste.setOnClickPendingIntent(R.id.vuoto, apri)

        val modificabile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val modello = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or modificabile,
        )
        viste.setPendingIntentTemplate(R.id.lista, modello)

        manager.updateAppWidget(id, viste)
    }

    companion object {
        /** Chiede di ridisegnare tutti i riquadri presenti sulla schermata iniziale. */
        fun aggiorna(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, AgendaWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }

        private fun intentoAggiornamento(context: Context): PendingIntent {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))
            return PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, AgendaWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** Allo scoccare della mezzanotte il riquadro passa al nuovo giorno. */
        private fun programmaMezzanotte(context: Context) {
            val sveglia = context.getSystemService(AlarmManager::class.java) ?: return
            val quando = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .plusMinutes(1)
                .toInstant()
                .toEpochMilli()
            sveglia.set(AlarmManager.RTC, quando, intentoAggiornamento(context))
        }
    }
}
