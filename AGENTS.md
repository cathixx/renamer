# AGENTS.md

Hinweise für KI-Agenten, die in diesem Repository arbeiten.

## Git

- Änderungen dürfen ohne Rückfrage **committet** werden.
- **Pushen nur mit ausdrücklicher Zustimmung**, und zwar für jeden Push neu. Eine frühere Zustimmung gilt nicht für spätere Pushes.
  Das gilt auch für Force-Pushes, Tags und alles andere, was den Stand auf GitHub verändert.
- Grund: Jeder Push auf `main` erzeugt über den Workflow `Build & Release` automatisch ein öffentliches GitHub-Release.
- `#minor` oder `#major` in einer Commit-Nachricht erhöht die Release-Version entsprechend (Standard: Patch).
  Diese Schlüsselwörter nur verwenden, wenn das ausdrücklich gewünscht ist.

## Projekt

- JavaFX-Desktop-Anwendung zum Umbenennen von Serien-Videodateien anhand von TheMovieDB.
- Build: `./gradlew build` (Java-Toolchain 25, wird bei Bedarf automatisch heruntergeladen).
- Alle Versionen von Abhängigkeiten und Plugins stehen in `gradle/libs.versions.toml`.
- Einstiegspunkt ist `de.cathixx.renamer.Launcher`, nicht `Main` (sonst: „JavaFX-Runtime-Komponenten fehlen“).
- Alle Textdateien verwenden LF als Zeilenende (siehe `.gitattributes`, `.editorconfig`).
- TMDB-API-Key: wird beim Build aus der Umgebungsvariable `TMDB_API_KEY` (CI: Repository-Secret) oder aus `.env`
  (lokal, siehe `.env.example`) in die JAR eingebettet. `.env` niemals committen und den Key nicht ausgeben.
