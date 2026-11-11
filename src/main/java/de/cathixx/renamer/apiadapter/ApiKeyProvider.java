package de.cathixx.renamer.apiadapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides the API keys of the online databases.
 * <p>
 * Lookup order:
 * <ol>
 *   <li>environment variable {@value #ENV_TMDB_API_KEY}: lets users run the application with their own key</li>
 *   <li>key embedded into the jar at build time (see {@code generateApiKeys} in build.gradle)</li>
 * </ol>
 * Note: a key shipped with a desktop application can always be extracted, so it's not a real secret. It's only kept
 * out of the source repository.
 */
@Slf4j
public final class ApiKeyProvider {

  public static final String ENV_TMDB_API_KEY = "TMDB_API_KEY";

  private static final String EMBEDDED_KEYS = "/de/cathixx/renamer/apikeys.properties";

  private ApiKeyProvider() {
  }

  public static Optional<String> getMovieDbKey() {
    Optional<String> userKey = nonBlank(System.getenv(ENV_TMDB_API_KEY));
    if (userKey.isPresent()) {
      log.info("using TMDB API key from environment variable {}", ENV_TMDB_API_KEY);
      return userKey;
    }
    return nonBlank(loadEmbeddedKeys().getProperty("tmdb"));
  }

  private static Properties loadEmbeddedKeys() {
    Properties properties = new Properties();
    try (InputStream in = ApiKeyProvider.class.getResourceAsStream(EMBEDDED_KEYS)) {
      if (in != null) {
        properties.load(in);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return properties;
  }

  private static Optional<String> nonBlank(String value) {
    return Optional.ofNullable(value).map(String::trim).filter(v -> !v.isEmpty());
  }
}
