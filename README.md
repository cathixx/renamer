# MediaRenamer by cathixx

This is a JavaFX based tool to rename your locally stored tv show recordings.

# Usage

### API key

The application uses [TheMovieDB](https://www.themoviedb.org/) and needs a TMDB API key (v3).
The key is embedded into the jar at build time and is taken from

- the environment variable `TMDB_API_KEY`, or
- a `.env` file in the project directory (copy `.env.example` to `.env` and fill in your key).

Without a key the build still works, but the application then requires the environment variable
`TMDB_API_KEY` at runtime. The environment variable always takes precedence, so you can also use
your own key with a released jar.

### compile

Compile this application by executing

 `gradlew build`

### execute

execute application from console (example)

`java -jar build/libs/renamer.jar src/test/resources/Eureka`
