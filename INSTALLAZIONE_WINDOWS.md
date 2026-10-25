# Installazione facile su Windows e Galaxy S25

Questa guida installa la build di prova dell'app direttamente dal fork. Non è
una versione pubblicata sul Play Store.

## Prima volta

Sul PC servono:

- Android Studio, con Android SDK Platform 35 e Build-Tools 34 installati;
- Git for Windows;
- Java 21. Se manca, da PowerShell eseguire `winget install --id Microsoft.OpenJDK.21 -e`.

Sul telefono:

1. Attiva **Opzioni sviluppatore**.
2. Attiva **Debug USB**.
3. Collega il telefono con un cavo dati e sbloccalo.
4. Quando Android chiede se consentire il debug USB, scegli **Consenti**.

## Installa o aggiorna con un solo comando

Apri PowerShell e incolla tutto questo blocco. Clona il progetto la prima volta;
nelle esecuzioni successive scarica gli aggiornamenti e reinstalla l'app.

```powershell
$ErrorActionPreference="Stop"; $env:JAVA_HOME=(Get-ChildItem "C:\Program Files\Microsoft\jdk-21*" -Directory | Select-Object -First 1).FullName; if (-not $env:JAVA_HOME) { throw "Java 21 non trovato: installalo con winget install --id Microsoft.OpenJDK.21 -e" }; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; $repo="$env:USERPROFILE\Downloads\daedalus-echo"; if (Test-Path "$repo\.git") { git -C $repo fetch origin; git -C $repo switch feat/italian-parakeet-meetings; git -C $repo pull --ff-only origin feat/italian-parakeet-meetings } else { git clone --branch feat/italian-parakeet-meetings https://github.com/samumar82/daedalus-echo.git $repo }; Set-Location "$repo\android"; .\gradlew.bat installDebug
```

Alla fine Android mostrerà **Daedalus Echo** fra le app. Se compare
`unauthorized`, sblocca il telefono, approva il messaggio di debug USB e
riesegui lo stesso comando.

## Primo avvio

1. Apri l'app e premi **Download All Models**.
2. Tieni l'app aperta mentre scarica i modelli.
3. La build scarica e verifica i file Parakeet singolarmente; non chiudere
   l'app prima del messaggio di completamento.
4. Poi registra una prova breve, premi stop e verifica trascrizione e
   riepilogo.

I modelli vengono elaborati sul telefono. Servono almeno **2 GB liberi** per
download, installazione e funzionamento agevole.

Ogni registrazione completata viene inoltre copiata in **Documenti/Daedalus Echo**
come documento, non nella libreria Musica. Questa copia resta disponibile anche
se l'app viene aggiornata o rimossa.

## Aggiornamenti

Per installare gli aggiornamenti futuri, ricollega il telefono e riesegui
esattamente il comando unico sopra. L'installazione aggiorna l'app senza
rimuovere registrazioni e modelli già validi.

## Risoluzione rapida dei problemi

- **Telefono non rilevato:** usa un cavo dati, non solo ricarica; cambia porta
  USB e verifica che il debug USB sia attivo.
- **Build ferma al download SDK:** lascia terminare Android Studio/Gradle; è
  normale al primo avvio.
- **Parakeet non scarica:** verifica Wi-Fi e spazio libero; dalla schermata
  Modelli usa Retry. Non spostare manualmente i file del modello.
- **L'app si chiude durante una prova:** conserva il log Android con
  `adb logcat -d` e apri una segnalazione, indicando modello del telefono e
  versione dell'app.

## Privacy e pubblicazione

Audio, trascrizioni e riepiloghi restano sul dispositivo finché non vengono
esportati volontariamente. Per il sito scolastico è consigliato pubblicare
soltanto riassunti e mappe concettuali revisionati, non registrazioni o
trascrizioni integrali.
