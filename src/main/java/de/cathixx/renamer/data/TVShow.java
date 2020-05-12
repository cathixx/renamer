package de.cathixx.renamer.data;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(of = "id")
public class TVShow implements Comparable<TVShow> {

  private final int id;

  private final Integer year;

  private final Map<Language, String> names;

  private final String bannerURL;

  private final Collection<String> aliasses;

  private final int numberOfSeasons;

  private final float popularity;

  public TVShow(int id, Integer year, Map<Language, String> names, String bannerURL, Collection<String> aliasses,
      int numberOfSeasons, float popularity) {
    this.id = id;
    this.year = year;
    this.names = Map.copyOf(names);
    this.bannerURL = bannerURL;
    this.aliasses = Set.copyOf(aliasses);
    this.numberOfSeasons = numberOfSeasons;
    this.popularity = popularity;
  }

  @Override
  public int compareTo(TVShow o) {
    return Comparator.comparing(TVShow::getId).compare(this, o);
  }

  public String getName(Language language) {
    String result = getNames().get(language);
    if (result == null) {
      result = getNames().values().iterator().next();
    }
    return result;
  }

}
