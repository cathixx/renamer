package de.cathixx.renamer.apiadapter;

import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import de.cathixx.renamer.key.ApiKeyProvider;
import de.cathixx.renamer.util.TVShowComparator;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TvResultsPage;
import info.movito.themoviedbapi.model.tv.TvEpisode;
import info.movito.themoviedbapi.model.tv.TvSeason;
import info.movito.themoviedbapi.model.tv.TvSeries;
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

@Slf4j
public class TheMovieDBAdapter implements TVShowApiAdapter {

  private static final String BANNER_URL = "https://image.tmdb.org/t/p/w400";

  private final Collection<Language> languages;

  private final ExecutorService executor = Executors.newCachedThreadPool(TheMovieDBAdapter::createThread);

  private final TmdbApi tmdbApi;

  public TheMovieDBAdapter(Collection<Language> languages) {
    this.languages = languages;
    this.tmdbApi = new TmdbApi(ApiKeyProvider.getMovieDbKey());
  }

  @Override
  public SortedSet<TVShow> findTvShow(String name) {
    log.info("request for TV-show '{}'", name);
    try {
      Map<Language, Future<Collection<TvSeries>>> futurs = createGeneralSeriesFutures(name);
      Map<Integer, Map<Language, TvSeries>> seriesResult = resolveGeneralSeriesFutures(futurs);
      return convertData(name, seriesResult);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<Language, Future<Collection<TvSeries>>> createGeneralSeriesFutures(String name) {
    Map<Language, Future<Collection<TvSeries>>> futurs = new HashMap<>();
    for (Language language : this.languages) {
      Future<Collection<TvSeries>> future = this.executor.submit(() -> findSeries(name, language));
      futurs.put(language, future);
    }
    return futurs;
  }

  private Map<Integer, Map<Language, TvSeries>> resolveGeneralSeriesFutures(
      Map<Language, Future<Collection<TvSeries>>> futurs) throws InterruptedException, ExecutionException {
    Map<Integer, Map<Language, TvSeries>> seriesResult = new HashMap<>();
    for (Map.Entry<Language, Future<Collection<TvSeries>>> entry : futurs.entrySet()) {
      Language lang = entry.getKey();
      Collection<TvSeries> seriess = entry.getValue().get();
      for (TvSeries series : seriess) {
        if (filterSeries(series)) {
          Map<Language, TvSeries> map = getOrCreate(seriesResult, series.getId(), HashMap::new);
          map.put(lang, series);
        }
      }
    }
    return seriesResult;
  }

  private static SortedSet<TVShow> convertData(String name, Map<Integer, Map<Language, TvSeries>> seriesResult) {
    SortedSet<TVShow> result = new TreeSet<>(new TVShowComparator(name));
    for (Map<Language, TvSeries> seriess : seriesResult.values()) {
      TvSeries series = seriess.values().iterator().next();
      Integer year = parseYear(series.getFirstAirDate());
      String bannerURL = Optional.ofNullable(series.getPosterPath()).map(b -> BANNER_URL + b).orElse(null);
      TVShow show = new TVShow(series.getId(), year, createNames(seriess), bannerURL, Collections.emptyList(),
          series.getNumberOfSeasons(), series.getPopularity());
      result.add(show);
    }
    return result;
  }

  private Collection<TvSeries> findSeries(String name, Language language) {
    long start = System.currentTimeMillis();
    Collection<TvSeries> result = Collections.emptyList();
    try {
      String lang = language.getApiName();
      TvResultsPage tvResult = this.tmdbApi.getSearch().searchTv(name, lang, 0);
      result = tvResult.getResults();
    } catch (RuntimeException e) {
      log.error(e.toString());
    }
    long time = System.currentTimeMillis() - start;
    log.debug("request for series ({}ms) [{}] {}", time, language.getApiName(), name);
    return result;
  }

  @Override
  public SortedSet<Episode> findEpisodes(TVShow tvShow, Language language) {
    log.info("request for episodes '{}'", tvShow.getName(language));
    try {
      int last = 1;
      try {
        TvSeries detail = this.tmdbApi.getTvSeries().getSeries(tvShow.getId(), language.getApiName());
        last = detail.getNumberOfSeasons();
      } catch (RuntimeException e) {
        log.error(e.toString());
      }
      List<Future<TvSeason>> futures = new ArrayList<>();
      for (int i = 1; i <= last; i++) {
        futures.add(createEpisodeFuture(tvShow, language, i));
      }
      List<TvEpisode> episodes = new ArrayList<>();
      for (Future<TvSeason> future : futures) {
        if (future.get() != null) {
          episodes.addAll(future.get().getEpisodes());
        }
      }
      return convertData(language, episodes, tvShow);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Future<TvSeason> createEpisodeFuture(TVShow tvShow, Language language, final int i) {
    return this.executor.submit(() -> getEpisodes(tvShow, language, i));
  }

  private TvSeason getEpisodes(TVShow tvShow, Language language, int season) {
    long start = System.currentTimeMillis();
    TvSeason result = null;
    try {
      result = this.tmdbApi.getTvSeasons().getSeason(tvShow.getId(), season, language.getApiName());
    } catch (RuntimeException e) {
      log.error(e.toString());
    }
    long time = System.currentTimeMillis() - start;
    log.debug("request for episodes ({}ms) [{}] {}", time, language.getApiName(), tvShow.getName(language));
    return result;
  }

  private static SortedSet<Episode> convertData(Language language, List<TvEpisode> episodes, TVShow tvShow) {
    SortedSet<Episode> result = new TreeSet<>();
    for (TvEpisode episode : episodes) {
      Map<Language, String> names = new HashMap<>();
      names.put(language, episode.getName());
      Episode episodeInfo = new Episode(episode.getId(), episode.getSeasonNumber(), episode.getEpisodeNumber(),
          tvShow.getNames(), names);
      result.add(episodeInfo);
    }
    return result;
  }

  private static Map<Language, String> createNames(Map<Language, TvSeries> seriess) {
    return seriess.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> getName(e.getValue())));
  }

  private static boolean filterSeries(TvSeries series) {
    return series.getName() != null && series.getName().matches(".*[A-Za-z0-9].*");
  }

  private static String getName(TvSeries series) {
    String result = series.getName();
    if (result != null) {
      Pattern pattern = Pattern.compile("(.*)\\([1-2][09][0-9][0-9]\\)");
      Matcher matcher = pattern.matcher(result);
      if (matcher.matches()) {
        result = matcher.group(1).trim();
      }
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

  private static Integer parseYear(String dateStr) {
    Integer result = null;
    try {
      LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      result = date.getYear();
    } catch (RuntimeException e) {
      log.error(e.toString());
    }
    return result;
  }

}
