package de.cathixx.renamer.apiadapter;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.entities.EpisodesResponse;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResultsResponse;
import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import de.cathixx.renamer.key.ApiKeyProvider;
import de.cathixx.renamer.util.TVShowComparator;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
public class TheTvDBAdapter implements TVShowApiAdapter {

  private static final String BANNER_URL = "https://www.thetvdb.com/banners/";

  private final TheTvdb tvDB = new TheTvdb(ApiKeyProvider.getTvDbKey());

  private final ExecutorService executor = Executors.newCachedThreadPool(TheTvDBAdapter::createThread);

  private final Collection<Language> languages;

  public TheTvDBAdapter(Collection<Language> languages) {
    this.languages = languages;
  }

  @Override
  public SortedSet<TVShow> findTvShow(String name) {
    log.info("request for TV-show '{}'", name);
    try {
      Map<Language, Future<Collection<Series>>> futurs = createGeneralSeriesFutures(name);
      Map<Integer, Map<Language, Series>> seriesResult = resolveGeneralSeriesFutures(futurs);
      Map<Integer, Map<Language, Future<Series>>> missing = createMissingFutures(seriesResult);
      resolveMissingFutures(seriesResult, missing);
      return convertData(name, seriesResult);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<Language, Future<Collection<Series>>> createGeneralSeriesFutures(String name) {
    Map<Language, Future<Collection<Series>>> futurs = new HashMap<>();
    for (Language language : this.languages) {
      Future<Collection<Series>> future = this.executor.submit(() -> findSeries(name, language));
      futurs.put(language, future);
    }
    return futurs;
  }

  private Map<Integer, Map<Language, Series>> resolveGeneralSeriesFutures(
      Map<Language, Future<Collection<Series>>> futurs) throws InterruptedException, ExecutionException {
    Map<Integer, Map<Language, Series>> seriesResult = new HashMap<>();
    for (Map.Entry<Language, Future<Collection<Series>>> entry : futurs.entrySet()) {
      Language lang = entry.getKey();
      Collection<Series> seriess = entry.getValue().get();
      for (Series series : seriess) {
        if (filterSeries(series)) {
          Map<Language, Series> map = getOrCreate(seriesResult, series.id, HashMap::new);
          map.put(lang, series);
        }
      }
    }
    return seriesResult;
  }

  private Map<Integer, Map<Language, Future<Series>>> createMissingFutures(
      Map<Integer, Map<Language, Series>> seriesResult) {
    Map<Integer, Map<Language, Future<Series>>> missing = new HashMap<>();
    for (Map.Entry<Integer, Map<Language, Series>> entry : seriesResult.entrySet()) {
      Map<Language, Series> value = entry.getValue();
      Series series = value.values().iterator().next();
      for (Language language : this.languages) {
        if (!value.containsKey(language)) {
          Future<Series> future = this.executor.submit(() -> getSeries(series, language));
          Map<Language, Future<Series>> map = getOrCreate(missing, series.id, HashMap::new);
          map.put(language, future);
        }
      }
    }
    return missing;
  }

  private void resolveMissingFutures(Map<Integer, Map<Language, Series>> seriesResult,
      Map<Integer, Map<Language, Future<Series>>> missing) throws InterruptedException, ExecutionException {
    for (Map<Language, Future<Series>> missMap : missing.values()) {
      for (Map.Entry<Language, Future<Series>> entry : missMap.entrySet()) {
        Language language = entry.getKey();
        Series series = entry.getValue().get();
        if (filterSeries(series)) {
          Map<Language, Series> map = seriesResult.get(series.id);
          map.put(language, series);
        }
      }
    }
  }

  private static SortedSet<TVShow> convertData(String name, Map<Integer, Map<Language, Series>> seriesResult) {
    SortedSet<TVShow> result = new TreeSet<>(new TVShowComparator(name));
    for (Map<Language, Series> seriess : seriesResult.values()) {
      Series series = seriess.values().iterator().next();
      Integer year = getFirstAired(series).map(LocalDate::getYear).orElse(null);
      String bannerURL = Optional.ofNullable(series.banner).map(b -> BANNER_URL + b).orElse(null);
      TVShow show = new TVShow(series.id, year, createNames(seriess), bannerURL, series.aliases, 0, 0);
      result.add(show);
    }
    return result;
  }

  private Collection<Series> findSeries(String name, Language language) throws IOException {
    long start = System.currentTimeMillis();
    try {
      String lang = language.getApiName();
      Response<SeriesResultsResponse> response = this.tvDB.search().series(name, null, null, lang).execute();
      if (response.isSuccessful()) {
        return response.body().data;
      }
      return Collections.emptyList();
    } finally {
      long time = System.currentTimeMillis() - start;
      log.debug("request for series ({}ms) [{}] {}", time, language.getApiName(), name);
    }
  }

  private Series getSeries(Series series, Language language) throws IOException {
    long start = System.currentTimeMillis();
    try {
      Response<SeriesResponse> response = this.tvDB.series().series(series.id, language.getApiName()).execute();
      if (response.isSuccessful()) {
        return response.body().data;
      }
      return null;
    } finally {
      long time = System.currentTimeMillis() - start;
      log.debug("request for series ({}ms) [{}] {}", time, language.getApiName(), series.seriesName);
    }
  }

  @Override
  public SortedSet<Episode> findEpisodes(TVShow tvShow, Language language) {
    log.info("request for episodes '{}'", tvShow.getName(language));
    try {
      List<Future<EpisodesResponse>> futures = new ArrayList<>();
      Future<EpisodesResponse> firstFuture = createEpisodeFuture(tvShow, language, 1);
      futures.add(firstFuture);
      Integer last = firstFuture.get().links.last;
      for (int i = 2; i <= last; i++) {
        futures.add(createEpisodeFuture(tvShow, language, i));
      }
      List<com.uwetrottmann.thetvdb.entities.Episode> episodes = new ArrayList<>();
      for (Future<EpisodesResponse> future : futures) {
        episodes.addAll(future.get().data);
      }
      episodes.removeIf(e -> e.airedSeason == null);
      episodes.removeIf(e -> e.airedEpisodeNumber == null);
      episodes.removeIf(e -> e.episodeName == null);
      return convertData(language, episodes, tvShow);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Future<EpisodesResponse> createEpisodeFuture(TVShow tvShow, Language language, final int i) {
    return this.executor.submit(() -> getEpisodes(tvShow, language, i));
  }

  private EpisodesResponse getEpisodes(TVShow tvShow, Language language, int page) throws IOException {
    long start = System.currentTimeMillis();
    try {
      Response<EpisodesResponse> resp = this.tvDB.series().episodes(tvShow.getId(), page, language.getApiName())
          .execute();
      if (resp.isSuccessful()) {
        return resp.body();
      }
      return null;
    } finally {
      long time = System.currentTimeMillis() - start;
      log.debug("request for episodes ({}ms) [{}] {}", time, language.getApiName(), tvShow.getName(language));
    }
  }

  private static SortedSet<Episode> convertData(Language language,
      List<com.uwetrottmann.thetvdb.entities.Episode> episodes, TVShow tvShow) {
    SortedSet<Episode> result = new TreeSet<>();
    for (com.uwetrottmann.thetvdb.entities.Episode episode : episodes) {
      Map<Language, String> names = new HashMap<>();
      names.put(language, episode.episodeName);
      Episode episodeInfo = new Episode(episode.id, episode.airedSeason, episode.airedEpisodeNumber, tvShow.getNames(),
          names);
      result.add(episodeInfo);
    }
    return result;
  }

  private static Map<Language, String> createNames(Map<Language, Series> seriess) {
    return seriess.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> getName(e.getValue())));
  }

  private static boolean filterSeries(Series series) {
    return series.seriesName != null && !series.seriesName.matches("\\*\\* .* \\*\\*");
  }

  private static String getName(Series series) {
    String result = series.seriesName;
    if (result != null) {
      Pattern pattern = Pattern.compile("(.*)\\([1-2][09][0-9][0-9]\\)");
      Matcher matcher = pattern.matcher(result);
      if (matcher.matches()) {
        result = matcher.group(1).trim();
      }
    }
    return result;
  }

  private static Optional<LocalDate> getFirstAired(Series series) {
    Optional<LocalDate> result = Optional.empty();
    if (series.firstAired != null && !series.firstAired.isEmpty()) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      result = Optional.of(LocalDate.parse(series.firstAired, formatter));
    }
    return result;
  }

  private static <K, V> V getOrCreate(Map<K, V> map, K key, Supplier<V> valueSupplier) {
    V value = map.get(key);
    if (value == null) {
      value = valueSupplier.get();
      map.put(key, value);
    }
    return value;
  }

  private static Thread createThread(Runnable r) {
    Thread t = Executors.defaultThreadFactory().newThread(r);
    t.setDaemon(true);
    return t;
  }

}
