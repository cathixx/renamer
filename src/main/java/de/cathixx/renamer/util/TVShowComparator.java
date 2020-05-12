package de.cathixx.renamer.util;

import de.cathixx.renamer.data.TVShow;
import java.util.Comparator;

public class TVShowComparator implements Comparator<TVShow> {

  private final String searchName;

  public TVShowComparator(String searchName) {
    this.searchName = searchName.toLowerCase();
  }

  @Override
  public int compare(TVShow o1, TVShow o2) {
    if (o1.getId() == o2.getId()) {
      return 0;
    }
    boolean o1exact = o1.getNames().values().stream().anyMatch(this.searchName::equalsIgnoreCase);
    boolean o2exact = o2.getNames().values().stream().anyMatch(this.searchName::equalsIgnoreCase);
    if (o1exact && !o2exact) {
      return -1;
    }
    if (o2exact && !o1exact) {
      return 1;
    }
    return Comparator.comparing(TVShow::getPopularity)
        .thenComparing((TVShow t) -> t.getNames().size())
        .thenComparing(t -> t.getYear() != null ? t.getYear() : Integer.MAX_VALUE)
        .reversed().thenComparing(TVShow::getId).compare(o1, o2);
  }

}
