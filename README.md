# Agenda Sfogliabile

Applicazione Android che mostra il calendario di Google come un'agenda di carta:
pagine color avorio, righe delle ore, nastrino segnalibro e pagine che si
girano con il dito.

## Che cosa fa

| Funzione | Come si usa |
|---|---|
| Vista giornaliera | Un giorno per pagina, con una riga per ogni ora. Si sfoglia scorrendo a destra o a sinistra. |
| Vista settimanale | Menu ⋮, «Vista settimanale». Con il telefono in orizzontale o sul tablet la settimana è un'agenda aperta su due pagine: lunedì, martedì e mercoledì a sinistra, gli altri giorni a destra. |
| Indice del mese | Icona del calendario in alto, oppure un tocco sulla data in testa alla pagina. Si tocca un giorno e l'agenda si apre lì. I puntini indicano i giorni con impegni. |
| Oggi | Il pulsante «Oggi» riporta alla pagina del giorno. |
| Nuovo impegno | Pulsante rosso «+», oppure un tocco sulla riga di un'ora. |
| Modifica ed eliminazione | Un tocco su un impegno apre la scheda. Gli impegni che si ripetono si aprono in sola lettura, con il pulsante per aprirli in Google Calendar, così non si altera tutta la serie. |
| Ricerca | Icona della lente: cerca in titolo, luogo e note, dall'anno scorso ai prossimi due anni. |
| Scelta dei calendari | Menu ⋮, «Calendari da mostrare». |
| Riquadro sulla schermata iniziale | Tieni premuto su uno spazio vuoto della schermata iniziale, scegli i riquadri (widget) e cerca «Agenda Sfogliabile». Mostra gli impegni di oggi e si aggiorna da solo. |

## Come si collega a Google Calendar

Al primo avvio si sceglie la fonte degli impegni (si cambia poi dal menu ⋮,
«Calendari e collegamento»):

| Fonte | Come funziona |
|---|---|
| Google Calendar, accesso diretto (consigliata) | L'agenda entra nell'account Google con il consenso della persona e legge e scrive gli impegni tramite l'interfaccia ufficiale di Google Calendar. Serve Internet. |
| Calendario del telefono | L'agenda legge i calendari che Android ha già sincronizzato. Funziona senza rete, ma su alcuni telefoni il calendario Google non arriva ad Android. |

### Configurazione su Google Cloud (una volta sola)

L'accesso diretto richiede che l'applicazione sia registrata su Google Cloud:

1. Su console.cloud.google.com crea un progetto, per esempio «Agenda Sfogliabile».
2. In «API e servizi», «Libreria», abilita **Google Calendar API**.
3. In «Google Auth Platform» completa la configurazione: nome dell'applicazione,
   indirizzo di assistenza, pubblico **Esterno**. Poi, nella sezione «Pubblico»,
   pubblica l'applicazione (altrimenti il consenso scade ogni sette giorni).
4. In «Client» crea un client di tipo **Android** con:
   - nome del pacchetto: `it.guidocostalonga.agendasfogliabile`
   - impronta SHA-1 della chiave di firma dell'applicazione.

Nell'applicazione non va inserito alcun codice: Google la riconosce dal nome del
pacchetto e dall'impronta della chiave di firma.

## Come si installa

1. Dal telefono apri la pagina **Releases** di questo repository e scarica il file
   `AgendaSfogliabile.apk` dell'ultima versione.
2. Apri il file scaricato. La prima volta Android chiede di consentire
   l'installazione da questa fonte (il browser o l'applicazione File): consentila.
3. Al primo avvio concedi l'accesso al calendario.

Requisiti: Android 8 o successivo, telefono o tablet.

## Aggiornamenti

A ogni modifica del codice sul ramo `main`, GitHub Actions compila una nuova
versione e la pubblica fra le Releases. Per aggiornare basta scaricare e aprire
il nuovo file APK (Android Package, il formato di installazione delle
applicazioni Android).

### Chiave di firma fissa (consigliata)

Android installa un aggiornamento solo se è firmato con la stessa chiave della
versione già presente. Senza impostazioni particolari la compilazione usa una
chiave di prova conservata nella memoria temporanea di GitHub Actions, che però
scade se non si compila per sette giorni: in quel caso bisogna disinstallare e
reinstallare l'applicazione (i dati del calendario non si perdono, stanno su
Google).

Per avere una chiave definitiva, una volta sola, da un computer con Java:

```
keytool -genkeypair -v -keystore firma.jks -alias agenda -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 firma.jks > firma.txt
```

Poi, nel repository su GitHub, in Settings, Secrets and variables, Actions,
aggiungi quattro segreti:

| Nome | Valore |
|---|---|
| `KEYSTORE_BASE64` | il contenuto di `firma.txt` |
| `KEYSTORE_PASSWORD` | la password scelta per il file |
| `KEY_ALIAS` | `agenda` |
| `KEY_PASSWORD` | la password della chiave |

Conserva `firma.jks` in un luogo sicuro e non caricarlo nel repository.

## Struttura del codice

| Percorso | Contenuto |
|---|---|
| `app/src/main/java/.../data/ArchivioCalendario.kt` | Lettura e scrittura del calendario del telefono |
| `app/src/main/java/.../ui/Sfogliatore.kt` | L'effetto della pagina che si gira |
| `app/src/main/java/.../ui/VistaGiorno.kt` | La pagina del giorno |
| `app/src/main/java/.../ui/VistaSettimana.kt` | La settimana su una o due pagine |
| `app/src/main/java/.../ui/IndiceMese.kt` | L'indice del mese |
| `app/src/main/java/.../ui/EditorImpegno.kt` | Scheda per creare e modificare un impegno |
| `app/src/main/java/.../widget/` | Il riquadro per la schermata iniziale |
| `.github/workflows/compila.yml` | Compilazione automatica e pubblicazione dell'APK |

Scritta in Kotlin con Jetpack Compose. Per compilarla in locale serve Android
Studio, oppure Gradle 8.9 con JDK (Java Development Kit) 17 e il comando
`gradle assembleRelease`.
