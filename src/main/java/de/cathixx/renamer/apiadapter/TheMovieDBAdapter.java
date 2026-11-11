package de.cathixx.renamer.apiadapter;

import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import de.cathixx.renamer.key.ApiKeyProvider;
import de.cathixx.renamer.util.TVShowComparator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import uk.co.conoregan.themoviedbapi.TmdbApi;
import uk.co.conoregan.themoviedbapi.model.core.TvSeries;
import uk.co.conoregan.themoviedbapi.model.tv.season.TvSeasonDb;
import uk.co.conoregan.themoviedbapi.model.tv.season.TvSeasonEpisode;
import uk.co.conoregan.themoviedbapi.model.tv.series.TvSeriesDb;
import uk.co.conoregan.themoviedbapi.tools.TmdbException;
import uk.co.conoregan.themoviedbapi.tools.TmdbHttpClient;
import uk.co.conoregan.themoviedbapi.tools.TmdbRequest;
import uk.co.conoregan.themoviedbapi.tools.TmdbRequestExecutor;
import uk.co.conoregan.themoviedbapi.tools.TmdbResponse;

@Slf4j
public class TheMovieDBAdapter implements TVShowApiAdapter {

  private static final String BANNER_URL = "https://image.tmdb.org/t/p/w400";

  private final Collection<Language> languages;

  private final ExecutorService executor = Executors.newCachedThreadPool(TheMovieDBAdapter::createThread);

  private final TmdbApi tmdbApi;

  public TheMovieDBAdapter(Collection<Language> languages) {
    this.languages = languages;
    this.tmdbApi = new TmdbApi(new ApiKeyRequestExecutor(ApiKeyProvider.getMovieDbKey()));
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
      String bannerURL = Optional.ofNullable(series.getPosterPath()).filter(b -> !b.isBlank()).map(b -> BANNER_URL + b).orElse(null);
      // search results don't contain the number of seasons
      float popularity = Optional.ofNullable(series.getPopularity()).map(Double::floatValue).orElse(0f);
      TVShow show = new TVShow(series.getId(), year, createNames(seriess), bannerURL, Collections.emptyList(), 0,
          popularity);
      result.add(show);
    }
    return result;
  }

  private Collection<TvSeries> findSeries(String name, Language language) {
    long start = System.currentTimeMillis();
    Collection<TvSeries> result = Collections.emptyList();
    try {
      String lang = language.getApiName();
      result = this.tmdbApi.getSearch().searchTv(name, null, false, lang, null, null).getResults();
    } catch (TmdbException | RuntimeException e) {
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
        TvSeriesDb detail = this.tmdbApi.getTvSeries().getDetails(tvShow.getId(), language.getApiName());
        last = detail.getNumberOfSeasons();
      } catch (TmdbException | RuntimeException e) {
        log.error(e.toString());
      }
      List<Future<TvSeasonDb>> futures = new ArrayList<>();
      for (int i = 1; i <= last; i++) {
        futures.add(createEpisodeFuture(tvShow, language, i));
      }
      List<TvSeasonEpisode> episodes = new ArrayList<>();
      for (Future<TvSeasonDb> future : futures) {
        if (future.get() != null) {
          episodes.addAll(future.get().getEpisodes());
        }
      }
      return convertData(language, episodes, tvShow);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Future<TvSeasonDb> createEpisodeFuture(TVShow tvShow, Language language, final int i) {
    return this.executor.submit(() -> getEpisodes(tvShow, language, i));
  }

  private TvSeasonDb getEpisodes(TVShow tvShow, Language language, int season) {
    long start = System.currentTimeMillis();
    TvSeasonDb result = null;
    try {
      result = this.tmdbApi.getTvSeasons().getDetails(tvShow.getId(), season, language.getApiName());
    } catch (TmdbException | RuntimeException e) {
      log.error(e.toString());
    }
    long time = System.currentTimeMillis() - start;
    log.debug("request for episodes ({}ms) [{}] {}", time, language.getApiName(), tvShow.getName(language));
    return result;
  }

  private static SortedSet<Episode> convertData(Language language, List<TvSeasonEpisode> episodes, TVShow tvShow) {
    SortedSet<Episode> result = new TreeSet<>();
    for (TvSeasonEpisode episode : episodes) {
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

  /**
   * The library only authenticates via "Authorization: Bearer", which requires a TMDB v4 read access token. The key
   * from {@link ApiKeyProvider} is a v3 API key, so it's additionally passed as "api_key" query parameter.
   */
  private static class ApiKeyRequestExecutor implements TmdbRequestExecutor {

    private final String apiKey;

    private final TmdbHttpClient delegate;

    ApiKeyRequestExecutor(String apiKey) {
      this.apiKey = apiKey;
      this.delegate = new TmdbHttpClient(apiKey);
    }

    @Override
    public TmdbResponse execute(TmdbRequest request) throws TmdbException {
      String separator = request.url().contains("?") ? "&" : "?";
      String url = request.url() + separator + "api_key=" + URLEncoder.encode(this.apiKey, StandardCharsets.UTF_8);
      return this.delegate.execute(new TmdbRequest(url, request.requestType(), request.jsonBody()));
    }
  }

  private static Integer parseYear(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
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
