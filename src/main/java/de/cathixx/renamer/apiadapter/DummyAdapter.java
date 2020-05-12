package de.cathixx.renamer.apiadapter;

import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyAdapter implements TVShowApiAdapter {

  @Override
  public SortedSet<TVShow> findTvShow(String name) {
    log.info("find tvshow '{}'", name);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
    }
    SortedSet<TVShow> result = new TreeSet<>();
    //		result.add(new TVShowInfo(1, "Shameless"));
    //		result.add(new TVShowInfo(2, "Bones"));
    return result;
  }

  @Override
  public SortedSet<Episode> findEpisodes(TVShow tvShow, Language language) {
    log.info("find episodes '{}'", tvShow);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
    }
    SortedSet<Episode> result = new TreeSet<>();
    //		result.add(new EpisodeInfo(1, "S01E01 - jo"));
    //		result.add(new EpisodeInfo(2, "S01E02 - jo2"));
    return result;
  }

}
