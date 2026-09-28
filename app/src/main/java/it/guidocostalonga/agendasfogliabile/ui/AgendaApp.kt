package it.guidocostalonga.agendasfogliabile.ui

import android.Manifest
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.guidocostalonga.agendasfogliabile.data.ArchivioCalendario
import it.guidocostalonga.agendasfogliabile.data.Impegno
import it.guidocostalonga.agendasfogliabile.data.Preferenze
import it.guidocostalonga.agendasfogliabile.util.giorniTra
import it.guidocostalonga.agendasfogliabile.util.lunediDi
import it.guidocostalonga.agendasfogliabile.util.settimaneTra
import it.guidocostalonga.agendasfogliabile.widget.AgendaWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

sealed interface Schermata {
    data object Agenda : Schermata
    data object Ricerca : Schermata
    data object Calendari : Schermata
    data object Indice : Schermata
    data class Editor(val impegno: Impegno?, val giorno: LocalDate, val ora: Int?) : Schermata
}

@Composable
fun AgendaApp(archivio: ArchivioCalendario, preferenze: Preferenze, ripresa: Int) {
    val context = LocalContext.current
    var permesso by remember { mutableStateOf(archivio.puoLeggere()) }
    val richiesta = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permesso = archivio.puoLeggere()
        AgendaWidget.aggiorna(context)
    }
    LaunchedEffect(ripresa) { permesso = archivio.puoLeggere() }

    if (permesso) {
        AgendaConPermesso(archivio, preferenze, ripresa)
    } else {
        RichiestaPermesso(
            onConsenti = {
                richiesta.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            },
            onImpostazioni = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        )
    }
}

@Composable
private fun AgendaConPermesso(archivio: ArchivioCalendario, preferenze: Preferenze, ripresa: Int) {
    val context = LocalContext.current
    var modifiche by remember { mutableIntStateOf(0) }

    // Ogni volta che il calendario cambia (anche per una sincronizzazione con Google) l'agenda si ricarica.
    DisposableEffect(Unit) {
        val osservatore = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                modifiche++
            }
        }
        context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, osservatore)
        onDispose { context.contentResolver.unregisterContentObserver(osservatore) }
    }
    val versione = modifiche + ripresa
    LaunchedEffect(versione) {
        delay(1500)
        AgendaWidget.aggiorna(context)
    }

    var scelti by remember { mutableStateOf(preferenze.calendariScelti) }
    var settimana by rememberSaveable { mutableStateOf(preferenze.vistaSettimana) }
    var giornoScelto by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var schermata by remember { mutableStateOf<Schermata>(Schermata.Agenda) }

    BackHandler(enabled = schermata != Schermata.Agenda) { schermata = Schermata.Agenda }

    when (val attuale = schermata) {
        Schermata.Agenda -> VistaAgenda(
            archivio = archivio,
            scelti = scelti,
            versione = versione,
            settimana = settimana,
            giornoScelto = giornoScelto,
            onGiornoScelto = { giornoScelto = it },
            onSettimana = {
                settimana = it
                preferenze.vistaSettimana = it
            },
            onApri = { schermata = Schermata.Editor(it, it.giornoInizio(ZoneId.systemDefault()), null) },
            onNuovo = { giorno, ora -> schermata = Schermata.Editor(null, giorno, ora) },
            onRicerca = { schermata = Schermata.Ricerca },
            onCalendari = { schermata = Schermata.Calendari },
            onIndice = { schermata = Schermata.Indice },
        )
        Schermata.Ricerca -> Ricerca(
            archivio = archivio,
            scelti = scelti,
            onIndietro = { schermata = Schermata.Agenda },
            onGiorno = {
                giornoScelto = it
                schermata = Schermata.Agenda
            },
        )
        Schermata.Calendari -> SceltaCalendari(
            archivio = archivio,
            preferenze = preferenze,
            onIndietro = { schermata = Schermata.Agenda },
            onCambio = {
                scelti = it
                AgendaWidget.aggiorna(context)
            },
        )
        Schermata.Indice -> IndiceMese(
            iniziale = giornoScelto,
            archivio = archivio,
            scelti = scelti,
            versione = versione,
            onScegli = {
                giornoScelto = it
                schermata = Schermata.Agenda
            },
            onChiudi = { schermata = Schermata.Agenda },
        )
        is Schermata.Editor -> EditorImpegno(
            archivio = archivio,
            preferenze = preferenze,
            impegno = attuale.impegno,
            giorno = attuale.giorno,
            ora = attuale.ora,
        ) { salvato ->
            if (salvato) {
                modifiche++
                AgendaWidget.aggiorna(context)
            }
            schermata = Schermata.Agenda
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VistaAgenda(
    archivio: ArchivioCalendario,
    scelti: Set<Long>?,
    versione: Int,
    settimana: Boolean,
    giornoScelto: LocalDate,
    onGiornoScelto: (LocalDate) -> Unit,
    onSettimana: (Boolean) -> Unit,
    onApri: (Impegno) -> Unit,
    onNuovo: (LocalDate, Int?) -> Unit,
    onRicerca: () -> Unit,
    onCalendari: () -> Unit,
    onIndice: () -> Unit,
) {
    val oggi = remember { LocalDate.now() }
    val larga = LocalConfiguration.current.screenWidthDp >= 600
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    val giornoAttuale by rememberUpdatedState(giornoScelto)

    val stato = key(settimana) {
        rememberPagerState(
            initialPage = if (settimana) CENTRO + settimaneTra(oggi, giornoScelto) else CENTRO + giorniTra(oggi, giornoScelto),
        ) { PAGINE }
    }

    // Tiene allineato il giorno scelto con la pagina aperta.
    LaunchedEffect(stato, settimana) {
        snapshotFlow { stato.currentPage }.collect { pagina ->
            val scarto = (pagina - CENTRO).toLong()
            if (settimana) {
                val lunedi = lunediDi(oggi).plusWeeks(scarto)
                if (lunediDi(giornoAttuale) != lunedi) onGiornoScelto(lunedi)
            } else {
                onGiornoScelto(oggi.plusDays(scarto))
            }
        }
    }

    fun vaiAOggi() {
        scope.launch {
            if (abs(stato.currentPage - CENTRO) > 3) stato.scrollToPage(CENTRO) else stato.animateScrollToPage(CENTRO)
        }
    }

    Scaffold(
        containerColor = Colori.Copertina,
        topBar = {
            BarraSuperiore("Agenda", onIndietro = null) {
                TextButton(
                    onClick = { vaiAOggi() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) { Text("Oggi", fontWeight = FontWeight.Bold) }
                IconButton(onClick = onRicerca) { Icon(Icons.Filled.Search, contentDescription = "Cerca") }
                IconButton(onClick = onIndice) { Icon(Icons.Filled.DateRange, contentDescription = "Indice del mese") }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Altre funzioni")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (settimana) "Vista giornaliera" else "Vista settimanale") },
                            onClick = {
                                menu = false
                                onSettimana(!settimana)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Calendari da mostrare") },
                            onClick = {
                                menu = false
                                onCalendari()
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNuovo(giornoScelto, null) },
                containerColor = Colori.Nastro,
                contentColor = Color.White,
            ) { Icon(Icons.Filled.Add, contentDescription = "Nuovo impegno") }
        },
    ) { spazio ->
        Box(Modifier.padding(spazio).fillMaxSize()) {
            Sfogliatore(stato, Modifier.fillMaxSize()) { pagina ->
                if (settimana) {
                    val lunedi = lunediDi(oggi).plusWeeks((pagina - CENTRO).toLong())
                    val impegni by produceState<List<Impegno>?>(null, lunedi, scelti, versione) {
                        value = withContext(Dispatchers.IO) { archivio.impegni(lunedi, lunedi.plusDays(6), scelti) }
                    }
                    PagineSettimana(
                        lunedi = lunedi,
                        impegni = impegni,
                        larga = larga,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        onApri = onApri,
                        onGiorno = { giorno ->
                            onGiornoScelto(giorno)
                            onSettimana(false)
                        },
                        onNuovo = onNuovo,
                    )
                } else {
                    val giorno = oggi.plusDays((pagina - CENTRO).toLong())
                    val impegni by produceState<List<Impegno>?>(null, giorno, scelti, versione) {
                        value = withContext(Dispatchers.IO) { archivio.impegni(giorno, giorno, scelti) }
                    }
                    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopCenter) {
                        PaginaGiorno(
                            giorno = giorno,
                            impegni = impegni,
                            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight(),
                            onApri = onApri,
                            onNuovo = onNuovo,
                            onIndice = onIndice,
                        )
                    }
                }
            }
            Nastro(Modifier.align(Alignment.TopEnd).padding(end = 40.dp))
        }
    }
}

@Composable
private fun RichiestaPermesso(onConsenti: () -> Unit, onImpostazioni: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Colori.Copertina).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Pagina(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            Text(
                "Agenda Sfogliabile",
                fontFamily = Grazie,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Colori.Inchiostro,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Per mostrarti i tuoi impegni l'agenda deve leggere il calendario del telefono, " +
                    "lo stesso che si sincronizza con Google Calendar. Gli appuntamenti restano sul " +
                    "telefono: l'applicazione non li invia a nessuno.",
                fontSize = 16.sp,
                color = Colori.Inchiostro,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Per aggiungere e modificare impegni dall'agenda, consenti anche la modifica del calendario.",
                fontSize = 16.sp,
                color = Colori.Inchiostro,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onConsenti,
                colors = ButtonDefaults.buttonColors(containerColor = Colori.Copertina, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Consenti l'accesso al calendario") }
            TextButton(onClick = onImpostazioni, modifier = Modifier.fillMaxWidth()) {
                Text("La richiesta non compare? Apri le impostazioni", color = Colori.Copertina)
            }
        }
    }
}
