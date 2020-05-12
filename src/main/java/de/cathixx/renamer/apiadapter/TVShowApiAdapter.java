package de.cathixx.renamer.apiadapter;

import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import java.util.SortedSet;

public interface TVShowApiAdapter {

  SortedSet<TVShow> findTvShow(String name);

  SortedSet<Episode> findEpisodes(TVShow tvShow, Language language);

}
