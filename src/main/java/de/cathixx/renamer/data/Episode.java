package de.cathixx.renamer.data;

import java.util.Comparator;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Episode implements Comparable<Episode> {

  private final int id;

  private final int seasonNumber;

  private final int episodeNumber;

  private final Map<Language, String> showNames;

  private final Map<Language, String> names;

  @Override
  public int compareTo(Episode o) {
    return Comparator.comparing(Episode::getId).compare(this, o);
  }
}
