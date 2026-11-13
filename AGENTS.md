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

## Git-Workflow

- **Commits:** Jede Änderung darf ohne Rückfrage direkt committet werden.
  Nur die zur Änderung gehörenden Dateien stagen (keine reinen Zeilenende-Änderungen).
- **Push:** Niemals ohne ausdrückliche Freigabe pushen – die Freigabe gilt jeweils
  nur für den konkreten Push, nicht pauschal für spätere.
- **Amend:** Werden direkt hintereinander zwei Änderungen angefordert, die thematisch
  zusammengehören, und ist der vorherige Commit noch nicht gepusht, wird per
  `git commit --amend` in diesen Commit aufgenommen statt einen neuen anzulegen.
  Vorher prüfen, dass der Commit wirklich lokal ist (z. B. `git status` / `git log origin/main..HEAD`).
  Bereits gepushte Commits nie amenden.
- **Lineare Historie, keine Merge-Commits:** PRs (z. B. von Dependabot) werden per
  Fast-Forward übernommen (`git fetch && git merge --ff-only origin/<pr-branch>`, dann
  pushen – GitHub markiert den PR automatisch als gemergt). Ist kein Fast-Forward möglich,
  den PR-Branch vorher rebasen. In der GitHub-Oberfläche sind Merge-Commits deaktiviert,
  nur „Squash“ und „Rebase and merge“ sind erlaubt.
- **Signaturen:** Alle Commits müssen signiert sein (`commit.gpgsign=true`). Commits von
  GitHub/Dependabot sind von GitHub signiert und bleiben bei Fast-Forward unverändert.
- **GPG-Key in der Sandbox einrichten:** Im Sandbox-Container ist `commit.gpgsign=true` mit
  `user.signingkey=D6FC8D086220A70A` gesetzt, der Schlüssel fehlt aber im (frisch angelegten)
  Keyring. Der private Schlüssel liegt read-only unter `/c/workspace/fobi/gpg-key/cathixx_sec.gpg`
  (Host-Ordner-Mount). Einmal pro Sandbox importieren, vor dem ersten Commit:

  ```bash
  gpg --list-secret-keys D6FC8D086220A70A || gpg --batch --import /c/workspace/fobi/gpg-key/cathixx_sec.gpg
  ```

  Der Schlüssel hat keine Passphrase, es ist kein Pinentry nötig. Die Warnung
  `[ unknown]` beim Vertrauen ist für das Signieren unerheblich. Nie in den Ordner
  schreiben, den Schlüssel nicht kopieren oder ins Repo legen. Danach prüfen mit
  `git log --show-signature -1`.