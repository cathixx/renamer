# Funktionsumfang MediaRenamer (Ist-Stand)

Diese Datei beschreibt vollständig, was die bestehende JavaFX-Anwendung tut. Sie ist die Grundlage für die
technologische Neuumsetzung und beschreibt bewusst das **tatsächliche** Verhalten des Codes, nicht das gewünschte.
Stellen, an denen das Verhalten vermutlich nicht beabsichtigt ist, sind in [Abschnitt 10](#10-auffälligkeiten-im-ist-verhalten)
gesammelt und im Text mit (→ A‑n) markiert. Was sich in der Neuumsetzung ändern soll, steht in
[Abschnitt 11](#11-vorgaben-für-die-neuumsetzung).

Stand: Commit `ddab237`. Alle Angaben stammen aus dem Quellcode; Dateierkennung, Namensbildung, Sortierung und
das Überschreiben bei Namenskollisionen wurden zusätzlich gegen die gebauten Klassen ausgeführt und geprüft.
Nicht ausgeführt wurden die Oberfläche selbst und die Aufrufe gegen TheMovieDB.

## 1. Zweck

Die Anwendung benennt lokal gespeicherte Videodateien von TV-Serien einheitlich um. Die Dateien liegen als einzelne
Folgen in Staffelordnern. Die Anwendung ermittelt je Datei Staffel- und Folgennummer, lädt die Folgentitel aus der
Online-Datenbank TheMovieDB (TMDB), zeigt alten und neuen Dateinamen nebeneinander an und benennt die Dateien auf
Knopfdruck um.

Zielformat des Dateinamens:

```
S<Staffel>E<Folge> - <Folgentitel>.<Endung>
<Serienname> - S<Staffel>E<Folge> - <Folgentitel>.<Endung>     (Option „Serienname einfügen“)
```

Beispiel: `S01E02 - Pilot.mp4`

Ablauf aus Nutzersicht:

1. Anwendung mit dem Pfad eines Serienordners starten.
2. Die Anwendung listet die Videodateien aller Staffeln auf und sucht die Serie anhand des Ordnernamens bei TMDB.
3. Bei Bedarf Suchbegriff, Treffer, Sprache und die Option „Serienname einfügen“ anpassen und einzelne Dateien abwählen.
4. „Umbenennen“ klicken.

## 2. Start und Eingabe

Die Anwendung hat keine Verzeichnisauswahl in der Oberfläche. Der Ordner kommt ausschließlich über
Kommandozeilenargumente: `java -jar renamer.jar <Pfad> [<Pfad> …]`

Im Alltag wird sie unter Windows über das Explorer-Menü „Senden an“ gestartet. Die im Explorer markierten Ordner
kommen dabei als Argumente an. Daraus erklären sich die beiden Modi: ein markierter Serienordner (E‑1) oder ein bzw.
mehrere markierte Staffelordner (E‑2).

| Nr. | Verhalten |
|---|---|
| E‑1 | **Serienordner-Modus:** Das erste Argument ist ein Verzeichnis, dessen Name nicht mit `Staffel` beginnt. Es gilt als Serienordner. Alle direkten Unterordner, deren Name dem Staffelmuster (E‑3) entspricht, sind die Staffeln. Weitere Argumente werden ignoriert. |
| E‑2 | **Staffelordner-Modus:** Der Name des ersten Arguments beginnt mit `Staffel` (Groß-/Kleinschreibung relevant). Dann sind **alle** Argumente Staffelordner, und der Serienordner ist das Elternverzeichnis des ersten Arguments. So lassen sich einzelne Staffeln einer Serie bearbeiten. Die weiteren Argumente werden nicht geprüft (→ A‑3). |
| E‑3 | **Staffelmuster:** `^[a-zA-Z]+ ([\d]+).*$` – ein Wort aus Buchstaben A–Z, genau ein Leerzeichen, eine Zahl, danach beliebiger Text. Die Zahl ist die Staffelnummer. Passend: `Staffel 01`, `Staffel 1`, `Season 3 extras`. Nicht passend: `Staffel01`, `Specials`, `Zusätze 1` (Umlaut). |
| E‑4 | Die Staffeln werden numerisch nach Staffelnummer sortiert. |
| E‑5 | Der Name des Serienordners wird als Suchbegriff vorbelegt, sein absoluter Pfad steht in der Statuszeile. |
| E‑6 | Das Wort `Staffel` aus E‑2 ist ein Text aus der Sprachdatei (`application.dir.season.name`). Im Serienordner-Modus funktioniert dagegen jedes Wort (z. B. `Season 1`). Ein direkt übergebener Ordner `Season 1` wird deshalb als Serienordner behandelt. |

Fehlerfälle beim Start stehen in [Abschnitt 8](#8-fehlerbehandlung).

## 3. Dateierkennung

### 3.1 Videodateien

| Nr. | Verhalten |
|---|---|
| D‑1 | Berücksichtigt werden reguläre Dateien direkt im Staffelordner (nicht rekursiv) mit den Endungen `.avi`, `.mkv`, `.mpg`, `.mp4`, `.mov`, Groß-/Kleinschreibung egal. |
| D‑2 | Alle anderen Dateien (Untertitel, `.nfo`, Bilder …) werden ignoriert und nie umbenannt. |
| D‑3 | Der „alte Dateiname“ in der Anzeige ist der Dateiname ohne Endung (alles vor dem letzten Punkt). |
| D‑4 | Die Staffelnummer einer Datei ist immer die des Ordners, nie die aus dem Dateinamen. |

### 3.2 Folgennummer aus dem Dateinamen

Aus dem vollständigen Dateinamen (inklusive Endung) wird zuerst die Zeichenfolge `-x264-` entfernt. Danach werden die
folgenden Muster der Reihe nach ausprobiert, jedes zuerst mit zweistelliger Staffelnummer (`01`) und dann mit
einstelliger (`1`). Der erste Treffer an beliebiger Stelle im Namen gewinnt. `SS` steht für die Staffelnummer des
Ordners, `EE` für **genau zwei Ziffern**.

| Rang | Muster | Beispiel (Staffel 1) | Hinweis |
|---|---|---|---|
| 1 | `S`SS`E`EE | `S01E02`, `s1e02` | S und E ohne Beachtung der Groß-/Kleinschreibung |
| 2 | `S`SS`x`EE | `S01x02` | |
| 3 | SS`x`EE | `01x02`, `1x02` | |
| 4 | `S`SS, ein beliebiges Zeichen, `E`EE | `S01.E02`, `S01_E02`, `S01 E02` | |
| 5 | `- `SSEE` -` | `Serie - 0102 - Titel` | mit Leerzeichen wie gezeigt |
| 6 | `Ep`EE | `Serie Ep02` | |
| 7 | `-E`EE`-` | `serie-E02-x` | |
| 8 | SS`-`EE | `01-02` | |
| 9 | SSEE am Namensende | – | greift nie (→ A‑2), entfällt (V‑8) |
| 10 | Name besteht nur aus EE | – | greift nie (→ A‑2), entfällt (V‑8) |

Folgen daraus (geprüft):

- Die Staffelnummer im Dateinamen muss zur Ordnernummer passen. `S02E03.mkv` im Ordner `Staffel 01` wird nicht erkannt.
- Einstellige Folgennummern werden nicht erkannt (`S01E1`). Bei dreistelligen zählen die ersten zwei Ziffern
  (`S01E123` → 12) (→ A‑4).
- Doppelfolgen werden nicht erkannt, `S01E02E03` ist Folge 2 (→ A‑4).
- Nicht erkannt werden z. B. `Folge 02.mkv`, `E02.mkv`, `02.mkv`, `Serie 0102.mkv`, `Serie 102.mkv`.

### 3.3 Dateien ohne erkannte Folgennummer

| Nr. | Verhalten |
|---|---|
| D‑5 | Dateien ohne erkannte Nummer bekommen in Reihenfolge ihrer Dateinamen die jeweils kleinste noch freie Folgennummer der Staffel (1 bis 999). Sie füllen also die Lücken zwischen den erkannten Folgen auf. Die Reihenfolge ist rein zeichenweise (Großbuchstaben vor Kleinbuchstaben). |
| D‑6 | Haben zwei Dateien dieselbe erkannte Folgennummer, erscheinen beide in der Liste mit derselben Nummer und bekommen denselben neuen Namen (→ A‑1). |
| D‑7 | Die Liste ist nach Staffel, dann Folgennummer sortiert. |
| D‑8 | Jede Datei erhält eine laufende Nummer über alle Staffeln hinweg (Spalte `#`) und ist anfangs zum Umbenennen ausgewählt. |

## 4. Online-Abgleich mit TheMovieDB

Einzige Datenquelle ist TMDB (API v3). Die Anbindung liegt hinter einer Schnittstelle mit zwei Operationen:
Serie suchen und Folgen einer Serie laden.

### 4.1 Seriensuche

| Nr. | Verhalten |
|---|---|
| T‑1 | Gesucht wird beim Start mit dem Ordnernamen und danach bei jeder Änderung des Suchfelds, jeweils 1 Sekunde nach der letzten Eingabe. |
| T‑2 | Der Suchbegriff wird um Leerzeichen am Rand gekürzt. Bei 2 oder weniger Zeichen wird nicht gesucht: Trefferliste, neue Namen und Poster werden geleert (→ A‑9). |
| T‑3 | Es wird parallel in **allen** angebotenen Sprachen gesucht (Deutsch `de`, Englisch `en`): `GET /3/search/tv` mit `query`, `language`, `include_adult=false`. Nur die erste Ergebnisseite je Sprache wird ausgewertet. |
| T‑4 | Treffer, deren Name keinen Buchstaben A–Z und keine Ziffer enthält, werden verworfen (z. B. rein nicht-lateinische Titel). |
| T‑5 | Treffer aus beiden Sprachen werden über die TMDB-ID zusammengeführt. Eine Serie hat dadurch je Sprache einen Namen, sofern sie in dieser Sprache gefunden wurde. |
| T‑6 | Endet ein Serienname auf eine Jahreszahl in Klammern, wird diese entfernt: `Doctor Who (2005)` → `Doctor Who`. |
| T‑7 | Je Serie werden übernommen: ID, Namen, Jahr der Erstausstrahlung (aus `first_air_date`), Poster-Pfad, Popularität. |
| T‑8 | **Sortierung der Treffer:** 1. Serien, deren Name in irgendeiner Sprache exakt dem Suchbegriff entspricht (Groß-/Kleinschreibung egal), zuerst. 2. Popularität absteigend. 3. Anzahl der Sprachen mit Namen absteigend. 4. Jahr absteigend, Serien ohne Jahr zuerst. 5. ID aufsteigend. |
| T‑9 | Nach jeder Suche wird automatisch der erste Treffer ausgewählt und dessen Folgenliste geladen. |
| T‑10 | Anzeige eines Treffers: Name in der gewählten Sprache, ersatzweise ein Name aus einer anderen Sprache, gefolgt von ` (Jahr)`, falls bekannt. |

### 4.2 Folgen laden

Ausgelöst durch: Auswahl eines Treffers, Wechsel der Sprache, Umschalten von „Serienname einfügen“.

| Nr. | Verhalten |
|---|---|
| T‑11 | `GET /3/tv/{id}` in der gewählten Sprache liefert die Anzahl der Staffeln. Schlägt der Aufruf fehl, wird 1 angenommen. |
| T‑12 | Für die Staffeln 1 bis Anzahl wird parallel `GET /3/tv/{id}/season/{n}` in der gewählten Sprache geladen. Staffel 0 (Specials) wird nie geladen (→ A‑5). |
| T‑13 | **Zuordnung:** Eine lokale Datei gehört zu der TMDB-Folge mit gleicher Staffel- und Folgennummer. Es gibt keine andere Zuordnung (kein Titelvergleich, keine absolute Nummerierung, keine alternative Reihenfolge). |
| T‑14 | Dateien ohne passende TMDB-Folge haben keinen neuen Namen und werden als `<nicht gefunden>` angezeigt. |
| T‑15 | Ist kein Treffer ausgewählt, werden alle neuen Namen und das Poster geleert. |

### 4.3 Poster

| Nr. | Verhalten |
|---|---|
| T‑16 | Zum ausgewählten Treffer wird das Poster von `https://image.tmdb.org/t/p/w400<poster_path>` geladen und rechts angezeigt. Ohne Poster bleibt die Fläche leer. |

### 4.4 Zwischenspeicher und Nebenläufigkeit

| Nr. | Verhalten |
|---|---|
| T‑17 | Ergebnisse werden für die Laufzeit der Anwendung im Speicher gehalten: Suchergebnisse je Suchbegriff, Folgenlisten je Serie und Sprache, Poster je URL. Wiederholte Anfragen kommen ohne Netzwerkzugriff sofort zurück. Es gibt keinen dauerhaften Cache. |
| T‑18 | Alle Netzwerkzugriffe und das Umbenennen laufen im Hintergrund, die Oberfläche bleibt bedienbar. |
| T‑19 | Laufen mehrere Anfragen derselben Art, wird nur das Ergebnis der zuletzt gestarteten angezeigt. Ältere Ergebnisse landen nur im Zwischenspeicher. |

### 4.5 API-Key

| Nr. | Verhalten |
|---|---|
| T‑20 | Reihenfolge: 1. Umgebungsvariable `TMDB_API_KEY` zur Laufzeit. 2. Beim Build in die JAR eingebetteter Key (aus `TMDB_API_KEY` oder der Datei `.env`). |
| T‑21 | Es ist ein TMDB-v3-Key, der als Query-Parameter `api_key` an jede Anfrage gehängt wird. |
| T‑22 | Ohne Key erscheint ein Fehlerdialog und die Anwendung beendet sich (siehe Abschnitt 8). |

## 5. Bildung des neuen Dateinamens

| Nr. | Verhalten |
|---|---|
| N‑1 | Grundform: `S%02dE%02d - <Folgentitel>`. Staffel und Folge sind mindestens zweistellig, größere Zahlen bleiben vollständig (`S12E123`). |
| N‑2 | Mit „Serienname einfügen“ wird `<Serienname> - ` vorangestellt, in der gewählten Sprache. Hat die Serie in dieser Sprache keinen Namen, entfällt der Vorsatz stillschweigend (→ A‑7). |
| N‑3 | Vom Folgentitel werden Punkte am Anfang und Ende entfernt (`...und dann?` → `und dann`, `Ende.` → `Ende`). Ein Titel aus nur einem Zeichen bleibt unverändert. |
| N‑4 | Serienname und Folgentitel werden bereinigt (Tabelle unten), danach am Rand gekürzt, und Folgen aus mehreren Leerzeichen zu einem zusammengefasst. |
| N‑5 | Die Dateiendung bleibt unverändert erhalten, auch in ihrer Schreibweise. Die Datei bleibt im selben Ordner. |
| N‑6 | Liefert TMDB keinen Titel, lautet der Name `S01E02 - ` mit Leerzeichen am Ende (→ A‑6). |

Zeichenersetzung (N‑4):

| Zeichen | wird zu |
|---|---|
| `:` `?` `<` `>` `\|` `!` | entfernt |
| `/` `\` `*` | Leerzeichen |
| `"` | `'` |
| `–` (Halbgeviertstrich) | `-` |
| `...` (drei Punkte) | `…` (ein Auslassungszeichen) |
| `’` `„` `“` | `'` |

Alle anderen Zeichen bleiben erhalten, auch Umlaute, Akzente, `%`, `&`, `#`, Klammern sowie `”`, `‘` und `—` (→ A‑8).

Beispiele (geprüft):

| Titel von TMDB | Ergebnis |
|---|---|
| `Teil 1: Der Anfang` | `S01E02 - Teil 1 Der Anfang` |
| `Wait... what?` | `S01E02 - Wait… what` |
| `Say "Hi" <now> \| later` | `S01E02 - Say 'Hi' now later` |
| `A/B\C*D` | `S01E02 - A B C D` |
| Serie `Doctor Who: Die Serie?`, Titel `Pilot` | `Doctor Who Die Serie - S01E02 - Pilot` |

## 6. Oberfläche

Ein einzelnes Fenster (Startgröße 1600 × 800), Titel `Media Renamer   © cathixx   <Version>`. Die Version stammt aus
dem Manifest der JAR, ohne Angabe steht dort `snapshot`. Alle Texte sind deutsch, es gibt keine Sprachumschaltung
der Oberfläche.

### 6.1 Aufbau

| Bereich | Element | Funktion |
|---|---|---|
| links | Textfeld | Suchbegriff für die Serie, vorbelegt mit dem Ordnernamen |
| links | Liste | Suchtreffer (T‑10), Einfachauswahl |
| links | Checkbox „Serienname einfügen“ | schaltet N‑2, anfangs aus |
| links | Auswahlbox Sprache | `Deutsch` (Vorgabe), `Englisch`; bestimmt die Sprache der Folgentitel, des Seriennamens und der Trefferanzeige |
| links | Schaltfläche „Umbenennen“ | startet das Umbenennen |
| Mitte | Tabelle | eine Zeile je Videodatei |
| rechts | Bild | Poster der gewählten Serie, maximal 400 × 750, Seitenverhältnis bleibt |
| unten | Statuszeile | Ladeanzeige (drehendes Symbol) und absoluter Pfad des Serienordners |

### 6.2 Tabelle

| Spalte | Inhalt |
|---|---|
| (ohne Titel) | Checkbox „diese Datei umbenennen“. Nur sichtbar, wenn nicht geladen wird und sich der neue Name vom alten unterscheidet. |
| `#` | laufende Nummer, dreistellig (`001`) |
| `alter Dateiname` | aktueller Name ohne Endung |
| `neuer Dateiname` | Vorschlag ohne Endung |

- Die Spalten sind nicht sortierbar. Die Namen sind nicht editierbar, nur die Checkbox lässt sich ändern.
- Vor der ersten Zeile jeder weiteren Staffel steht eine schwarze Trennlinie.
- Darstellung der Spalte „neuer Dateiname“:

| Zustand | Darstellung |
|---|---|
| es wird geladen | `Lade...`, kursiv, hellgrau |
| kein Name gefunden | `<nicht gefunden>`, kursiv, rot |
| Name unterscheidet sich nur in Groß-/Kleinschreibung | dunkelblau |
| Name unterscheidet sich sonst | fett |
| Name ist identisch | normal |

### 6.3 Zustände der Bedienelemente

| Situation | Wirkung |
|---|---|
| irgendeine Hintergrundaufgabe läuft | Ladeanzeige sichtbar, Tabelle gesperrt, „Umbenennen“ gesperrt |
| Seriensuche läuft | zusätzlich Trefferliste gesperrt |
| Umbenennen läuft | zusätzlich Trefferliste, Suchfeld und Sprachauswahl gesperrt |
| keine Datei ist ausgewählt **und** änderbar | „Umbenennen“ gesperrt |

## 7. Umbenennen

| Nr. | Verhalten |
|---|---|
| U‑1 | Umbenannt wird jede Datei, die ausgewählt ist, einen neuen Namen hat und deren neuer Name sich vom alten unterscheidet (Groß-/Kleinschreibung zählt als Unterschied). |
| U‑2 | Es gibt keine Sicherheitsabfrage, kein Rückgängig und kein Protokoll außer der Konsolenausgabe. Die Vorschau in der Tabelle ist die einzige Kontrolle. |
| U‑3 | Die Dateien werden nacheinander im selben Ordner umbenannt. Ein Fehlschlag bricht den Lauf nicht ab. |
| U‑4 | Nach Erfolg zeigt die Zeile den neuen Namen als alten Namen. Sie gilt damit als erledigt, die Checkbox verschwindet. |
| U‑5 | Fehlgeschlagene Dateien werden am Ende gesammelt in einem Fehlerdialog genannt. Sie bleiben unverändert in der Liste und können erneut versucht werden. |
| U‑6 | Es wird nicht geprüft, ob die Zieldatei schon existiert (→ A‑1). |
| U‑7 | Nach dem Umbenennen bleibt die Anwendung offen. Sprache oder Serie können gewechselt und die Dateien erneut umbenannt werden. |

## 8. Fehlerbehandlung

Alle Dialoge haben den Titel `Fehler`.

| Auslöser | Kopfzeile / Text | Danach |
|---|---|---|
| kein Argument übergeben | „Es ist ein Fehler aufgetreten“ / „Keine Parameter gefunden!“ | Anwendung beendet sich |
| erstes Argument ist kein Verzeichnis | vorgesehen: „"<Pfad>" ist kein Verzeichnispfad!“ | Anwendung beendet sich ohne Dialog (→ A‑3) |
| keine Staffelordner gefunden | „Es ist ein Fehler aufgetreten“ / „Keine Staffel Ordner gefunden!“ | Anwendung beendet sich |
| kein TMDB-Key vorhanden | „Kein API-Key für TheMovieDB vorhanden“ / „Diese Version wurde ohne API-Key gebaut. Bitte einen eigenen TMDB-API-Key in der Umgebungsvariable TMDB_API_KEY hinterlegen.“ | Anwendung beendet sich |
| Dateien konnten nicht umbenannt werden | „Es ist ein Fehler aufgetreten“ / „Folgende Dateien konnten nicht umbenannt werden: <Dateinamen, kommagetrennt>“ | Anwendung läuft weiter |
| unerwarteter Fehler in einer Hintergrundaufgabe | „Es ist ein unerwarteter Fehler aufgetreten“ / technische Fehlermeldung | Anwendung läuft weiter |

Netzwerk- und TMDB-Fehler führen zu **keinem** Dialog. Sie werden nur auf der Konsole protokolliert und wie ein
leeres Ergebnis behandelt (→ A‑10).

## 9. Rahmenbedingungen

| Thema | Ist-Stand |
|---|---|
| Auslieferung | eine ausführbare JAR `renamer.jar` mit allen Abhängigkeiten und den JavaFX-Bibliotheken für Windows, Linux und macOS (x64, nicht Apple Silicon). Genutzt wird sie ausschließlich unter Windows. |
| Laufzeit | Java 25 |
| Konfiguration | keine. Es wird nichts gespeichert, weder Einstellungen noch Fensterzustand noch Verlauf. |
| Protokoll | nur Konsole (log4j2), u. a. jede Umbenennung als `rename 'alt' -> 'neu'` |
| Versionierung | jeder Push auf `main` erzeugt per GitHub Actions ein Release mit hochgezählter Version (Patch; `#minor`/`#major` in der Commit-Nachricht) |
| Tests | keine automatisierten Tests, nur ein Beispielordner `src/test/resources/Eureka` |
| Sprache der Oberfläche | nur Deutsch. Die Texte liegen in `main_de.properties`, die Beschriftung „Serienname einfügen“ steht fest im Layout. |

Nicht vorhanden sind unter anderem: Filme (nur Serien), andere Datenquellen als TMDB, Verschieben von Dateien oder
Anlegen von Ordnern, Umbenennen von Begleitdateien (Untertitel, `.nfo`), frei konfigurierbares Namensformat,
manuelles Bearbeiten des neuen Namens oder der Folgennummer, Verzeichnisauswahl oder Drag & Drop, Stapelverarbeitung
mehrerer Serien, Betrieb ohne Oberfläche.

## 10. Auffälligkeiten im Ist-Verhalten

Die Spalte „Entscheidung“ hält fest, was für die Neuumsetzung gilt: **übernehmen** (Verhalten ist gewollt),
**ändern** (siehe Vorgabe in Abschnitt 11) oder **offen** (noch nicht entschieden).

| Nr. | Auffälligkeit | Entscheidung |
|---|---|---|
| A‑1 | **Namenskollision kann Dateien überschreiben.** Erhalten zwei Dateien denselben Zielnamen (gleiche Folgennummer und Endung, D‑6) oder existiert die Zieldatei bereits, findet keine Prüfung statt. Unter Linux wurde geprüft: die vorhandene Datei wird stillschweigend ersetzt. Unter Windows, dem einzigen genutzten System, schlägt das Umbenennen in diesem Fall laut Java-Dokumentation üblicherweise fehl und landet im Fehlerdialog (nicht geprüft). | ändern (V‑3) |
| A‑2 | Die Muster 9 und 10 der Folgenerkennung können nie greifen, weil gegen den Namen **mit** Endung geprüft wird (`0102.mp4` endet nicht auf Ziffern). Vermutlich war der Name ohne Endung gemeint. | ändern (V‑8) |
| A‑3 | **Startfehler ohne Dialog.** Für „kein Verzeichnis“ wird ein nicht vorhandener Textschlüssel verwendet, die Anwendung stürzt ab statt den Dialog zu zeigen. Ebenso ohne Dialog: im Staffelordner-Modus ein weiteres Argument, das nicht dem Staffelmuster entspricht oder nicht existiert, sowie ein relativer Pfad ohne Elternverzeichnis (`Staffel 01`). | ändern (V‑2, V‑10); relativer Pfad kommt bei „Senden an“ nicht vor |
| A‑4 | Folgennummern müssen genau zweistellig sein, Doppelfolgen werden als Einzelfolge behandelt (Abschnitt 3.2). | zweistellig: übernehmen (V‑7); Doppelfolgen: ändern (V‑11) |
| A‑5 | Staffel 0 / Specials: Ein Ordner `Staffel 00` wird gelesen, bei TMDB aber nie abgefragt. Alle Dateien darin bleiben `<nicht gefunden>` und werden nie umbenannt. | nicht umbenennen: übernehmen; nicht mehr anzeigen: ändern (V‑5) |
| A‑6 | Fehlt der Folgentitel, entsteht ein Name mit Leerzeichen am Ende (`S01E02 - `), der als gültiger Vorschlag gilt und umbenannt wird. | ändern (V‑6) |
| A‑7 | „Serienname einfügen“ wirkt nicht, wenn die Serie in der gewählten Sprache nicht unter den Suchtreffern war. Die Oberfläche zeigt dafür keinen Hinweis. | entfällt, Option wird entfernt (V‑9) |
| A‑8 | Die Zeichenbereinigung ist unvollständig: `”`, `‘`, `—` und geschützte Leerzeichen bleiben erhalten, Tabulatoren ebenfalls. | ändern (V‑12) |
| A‑9 | Serien mit ein- oder zweistelligem Namen (z. B. „24“, „ER“) lassen sich nicht suchen (T‑2). | ändern (V‑13) |
| A‑10 | **Netzwerkfehler sind unsichtbar und bleiben haften.** Ein fehlgeschlagener Abruf liefert ein leeres Ergebnis, das im Zwischenspeicher landet. Derselbe Suchbegriff bzw. dieselbe Serie liefert dann bis zum Neustart weiter das leere oder unvollständige Ergebnis. | ändern (V‑4) |
| A‑11 | Die laufende Nummer `#` wird vor dem Sortieren vergeben. Bei Dateien ohne erkannte Folgennummer stimmt sie nicht mit der Anzeigereihenfolge überein. | ändern (V‑14) |
| A‑12 | Die Trennlinie zwischen Staffeln hängt an der alphabetisch ersten erkannten Datei, nicht an der ersten angezeigten Zeile. Enthält eine Staffel nur nicht erkannte Dateien, hat jede ihrer Zeilen eine Linie. | ändern (V‑15) |
| A‑13 | Von jeder Suche wird nur die erste Ergebnisseite je Sprache ausgewertet. Jahr und Poster einer Serie stammen aus dem Treffer einer nicht festgelegten Sprache. | übernehmen (V‑16) |
| A‑14 | Die Checkbox „Serienname einfügen“ bleibt während des Umbenennens bedienbar, alle anderen Eingaben sind gesperrt. | entfällt, Option wird entfernt (V‑9) |

## 11. Vorgaben für die Neuumsetzung

Entscheidungen vom 15.11.2026. Alles, was hier nicht genannt ist, soll sich wie in den Abschnitten
2 bis 9 beschrieben verhalten.

### 11.1 Rahmen

| Nr. | Vorgabe |
|---|---|
| V‑1 | **Start über „Senden an“ bleibt.** Die Anwendung wird weiterhin aus dem Windows-Explorer mit den markierten Ordnern als Argumenten gestartet. Die Modi E‑1 und E‑2 müssen erhalten bleiben. Zielplattform ist ausschließlich Windows. |

### 11.2 Änderungen gegenüber dem Ist-Stand

| Nr. | Vorgabe | ersetzt |
|---|---|---|
| V‑2 | **Verzeichnisauswahl:** Wird beim Start kein Pfad übergeben, erscheint kein Fehler mehr. Stattdessen kann ein Verzeichnis ausgewählt werden. Der Ordner lässt sich außerdem jederzeit in der laufenden Anwendung wechseln. | Fehlerdialog „Keine Parameter gefunden!“ |
| V‑3 | **Kein Datenverlust, Folgennummern eindeutig:** Unter den zum Umbenennen ausgewählten Dateien darf jede Folgennummer je Staffel nur einmal vorkommen. Kommt eine Nummer mehrfach vor, wird ein Fehler angezeigt und das Umbenennen ist nicht möglich. Es reicht, betroffene Dateien abzuwählen, bis nur noch eine je Nummer ausgewählt ist; danach ist das Umbenennen wieder möglich. Eine vorhandene Datei darf nie überschrieben werden. | A‑1, D‑6, U‑6 |
| V‑4 | **Netzwerkfehler anzeigen:** Schlägt ein Abruf bei TMDB fehl, muss das in der Oberfläche sichtbar sein. | A‑10 |
| V‑5 | **`Staffel 00` wird ignoriert.** Der Ordner dient als Ablage für Folgen, die nicht umbenannt werden sollen. Seine Dateien werden weder angezeigt noch umbenannt. | A‑5 |
| V‑6 | **Fehlender Folgentitel:** Der Name besteht dann nur aus Staffel- und Folgennummer, ohne Trenner und ohne Titel: `S01E01.mp4`. | A‑6, N‑6 |
| V‑8 | **Wirkungslose Erkennungsmuster entfallen:** Die Muster 9 (SSEE am Namensende) und 10 (Name nur aus EE) aus Abschnitt 3.2 werden nicht übernommen. Da sie heute nie greifen, ändert sich am Ergebnis nichts. | A‑2 |
| V‑9 | **„Serienname einfügen“ entfällt.** Die Option wird nicht mehr genutzt und komplett entfernt. Neue Namen haben immer die Form `S01E02 - <Folgentitel>` bzw. `S01E02`. Der Serienname wird nur noch in der Trefferliste angezeigt. | N‑2, A‑7, A‑14 |
| V‑10 | **Ungültige Startargumente führen nicht mehr zum Absturz.** Wird eine Datei statt eines Ordners übergeben, öffnet sich die Anwendung mit einem Hinweis, und man kann direkt einen Ordner wählen (V‑2). Enthält eine Auswahl im Staffelordner-Modus Einträge, die keine Staffelordner sind, werden diese ignoriert, und ein Hinweis nennt sie. | A‑3 |
| V‑11 | **Doppelfolgen in einer Datei:** Enthält eine Datei zwei Folgen, wird sie als solche erkannt und mit beiden Nummern benannt, z. B. `S01E02E03 - <Titel>.mkv`. Der Titel ist der der ersten Folge; fehlt er, lautet der Name `S01E02E03.mkv` (V‑6). Erkannt wird eine Doppelfolge nur an der Schreibweise `SxxEyyEzz` im Dateinamen (Groß-/Kleinschreibung egal). Andere Schreibweisen wie `S01E02-E03` oder `S01E02-03` kommen nicht vor und müssen nicht erkannt werden. Bereits so benannte Dateien (der heutige Bestand) werden damit korrekt als Doppelfolge erkannt. Beide Folgennummern gelten als belegt (V‑3, D‑5). Liegen die beiden Folgen als getrennte Dateien vor, werden sie wie zwei normale Folgen behandelt. | A‑4 |
| V‑12 | **Zeichenbereinigung** des Folgentitels, in dieser Reihenfolge: 1. Alle Arten von Anführungszeichen (`"` `'` `’` `‘` `„` `“` `”` `«` `»` …) werden zu `'`. 2. `...` wird zu `…`, `–` zu `-` (wie bisher). 3. `:` und `?` werden ersatzlos entfernt (wie bisher). 4. Die Satzzeichen `-` `.` `,` `'` `&` `(` `)` `…` bleiben erhalten. Alle übrigen Sonderzeichen werden durch `_` ersetzt, auch die unter Windows verbotenen und `!`. Buchstaben bleiben vollständig erhalten, auch Umlaute, Akzente und `ß` (`á`, `ß`, `ç` …), ebenso Ziffern. 5. Jedes Whitespace-Zeichen (Tabulator, geschütztes Leerzeichen usw.) wird zu einem normalen Leerzeichen, mehrere aufeinanderfolgende zu einem. Im fertigen Dateinamen stehen nie zwei Leerzeichen hintereinander. Beispiele: `AC/DC` → `AC_DC`, `Say "Hi"` → `Say 'Hi'`, `Stop!` → `Stop_`, `Teil 1: Der Anfang` → `Teil 1 Der Anfang`, `Was nun?` → `Was nun`, `Dr. House & Co.` bleibt unverändert. | N‑4, A‑8 |
| V‑13 | **Seriensuche ab einem Zeichen:** Gesucht wird, sobald der Suchbegriff (ohne Leerzeichen am Rand) mindestens ein Zeichen hat, damit Serien wie „24“ oder „ER“ auffindbar sind. Nur ein leeres Suchfeld leert die Trefferliste. | T‑2, A‑9 |
| V‑14 | **Laufende Nummer folgt der Anzeige:** Die Spalte `#` wird nach dem Sortieren vergeben und zählt die Zeilen in der angezeigten Reihenfolge. | A‑11 |
| V‑15 | **Trennlinie zwischen Staffeln:** Die Linie steht immer genau vor der ersten angezeigten Zeile jeder weiteren Staffel. | A‑12 |

Folgerungen, die sich aus diesen Vorgaben ergeben, aber nicht ausdrücklich entschieden wurden:

- Zu V‑3: Doppelte Nummern können nur bei im Dateinamen **erkannten** Nummern entstehen. Dateien ohne erkannte
  Nummer bekommen immer eine noch freie (D‑5). Sind die Nummern eindeutig, sind auch die neuen Namen innerhalb eines
  Staffelordners eindeutig. Ein neuer Name enthält immer `SxxEyy` und würde bei einer bereits vorhandenen Datei
  gleichen Namens deren Nummer belegen, was V‑3 schon als Fehler meldet. Das Verbot, Dateien zu überschreiben, bleibt
  trotzdem als letzte Sicherung beim eigentlichen Umbenennen bestehen.
- Zu V‑3: Der Fehler sollte die betroffenen Dateien nennen bzw. markieren, damit man sie finden und z. B. nach
  `Staffel 00` verschieben kann.
- Zu V‑4: Ein fehlgeschlagener Abruf darf nicht als gültiges Ergebnis im Zwischenspeicher landen, sonst liefert ein
  erneuter Versuch weiter nichts (zweiter Teil von A‑10).
- Zu V‑11: Die Doppelfolge zählt nur einmal bei der laufenden Nummer `#` und steht in der Liste an der Stelle
  ihrer ersten Folge.
- Zu V‑5: Maßgeblich ist die Staffelnummer 0, also auch `Staffel 0`. Enthält ein Serienordner nur `Staffel 00`, gilt
  er als Ordner ohne Staffeln.

### 11.3 Ausdrücklich beizubehalten

| Nr. | Vorgabe |
|---|---|
| V‑7 | **Folgennummern sind immer zweistellig**, sowohl bei der Erkennung im Dateinamen (Abschnitt 3.2) als auch im neuen Namen. |
| V‑16 | **Nur die erste Ergebnisseite** der TMDB-Suche je Sprache (bis zu 20 Treffer) wird ausgewertet. Das reicht, weil der gesuchte Treffer praktisch immer oben steht. |

### 11.4 Noch offen

Keine. Alle Auffälligkeiten aus Abschnitt 10 sind entschieden.

## 12. Fundstellen im Code

| Thema | Datei |
|---|---|
| Argumente, Dateierkennung, Namensbildung | `src/main/java/de/cathixx/renamer/util/TVShowFileSupport.java` |
| Umbenennen | `src/main/java/de/cathixx/renamer/util/TVShowRenamer.java` |
| TMDB-Anbindung | `src/main/java/de/cathixx/renamer/apiadapter/TheMovieDBAdapter.java` |
| API-Key | `src/main/java/de/cathixx/renamer/apiadapter/ApiKeyProvider.java`, `build.gradle` |
| Sortierung der Treffer | `src/main/java/de/cathixx/renamer/util/TVShowComparator.java` |
| Ablaufsteuerung | `src/main/java/de/cathixx/renamer/gui/MainController.java` |
| Hintergrundaufgaben, Zwischenspeicher | `src/main/java/de/cathixx/renamer/util/TaskExecutor.java` |
| Oberfläche, Tabellendarstellung | `src/main/java/de/cathixx/renamer/gui/MainView.java`, `EpisodeTableSupport.java`, `src/main/resources/de/cathixx/renamer/view/main.fxml` |
| Texte, Fehlerdialoge | `src/main/resources/de/cathixx/renamer/bundles/main_de.properties`, `src/main/java/de/cathixx/renamer/gui/DialogSupport.java` |
