package it.guidocostalonga.agendasfogliabile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import it.guidocostalonga.agendasfogliabile.data.ArchivioCalendario
import it.guidocostalonga.agendasfogliabile.data.Preferenze
import it.guidocostalonga.agendasfogliabile.ui.AgendaApp
import it.guidocostalonga.agendasfogliabile.ui.TemaAgenda

class MainActivity : ComponentActivity() {

    // Aumenta a ogni ritorno nell'app: ricarica i dati e ricontrolla i permessi.
    private val ripresa = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val archivio = ArchivioCalendario(this)
        val preferenze = Preferenze(this)
        setContent {
            TemaAgenda {
                AgendaApp(archivio, preferenze, ripresa.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ripresa.intValue++
    }
}
