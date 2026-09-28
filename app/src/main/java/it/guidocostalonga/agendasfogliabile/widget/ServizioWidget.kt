package it.guidocostalonga.agendasfogliabile.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import it.guidocostalonga.agendasfogliabile.R
import it.guidocostalonga.agendasfogliabile.data.Fonti
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.data.Preferenze
import it.guidocostalonga.agendasfogliabile.util.testo
import java.time.LocalDate
import java.time.ZoneId

/** Fornisce al riquadro l'elenco degli impegni di oggi. */
class ServizioWidget : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Elenco(applicationContext)
}

private class Elenco(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var impegni: List<Impegno> = emptyList()
    private var giorno: LocalDate = LocalDate.now()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        giorno = LocalDate.now()
        impegni = try {
            val preferenze = Preferenze(context)
            Fonti.attiva(context, preferenze).impegni(giorno, giorno, preferenze.calendariScelti)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = impegni.size

    override fun getViewAt(position: Int): RemoteViews {
        val viste = RemoteViews(context.packageName, R.layout.widget_riga)
        val impegno = impegni.getOrNull(position) ?: return viste
        val zona = ZoneId.systemDefault()
        viste.setTextColor(R.id.pallino, impegno.colore or 0xFF000000.toInt())
        viste.setTextViewText(
            R.id.ora,
            if (impegno.iniziaIl(giorno, zona)) impegno.oraInizio(zona).testo() else "Tutto il giorno",
        )
        viste.setTextViewText(R.id.titolo, impegno.titolo)
        viste.setOnClickFillInIntent(R.id.riga, Intent())
        return viste
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
