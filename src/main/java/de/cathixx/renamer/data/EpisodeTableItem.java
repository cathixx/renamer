package de.cathixx.renamer.data;

import java.io.File;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class EpisodeTableItem {

  private final BooleanProperty selected = new SimpleBooleanProperty();

  private final IntegerProperty number = new SimpleIntegerProperty();

  private final StringProperty oldName = new SimpleStringProperty();

  private final StringProperty newName = new SimpleStringProperty();

  private final ObjectProperty<File> file = new SimpleObjectProperty<>();

  private final IntegerProperty seasonNumber = new SimpleIntegerProperty();

  private final IntegerProperty episodeNumber = new SimpleIntegerProperty();

  private final BooleanProperty firstOfSeason = new SimpleBooleanProperty();

  public EpisodeTableItem(File file, int seasonNumber, int episodeNumber, boolean firstOfSeason) {
    this.file.set(file);
    this.seasonNumber.set(seasonNumber);
    this.episodeNumber.set(episodeNumber);
    this.firstOfSeason.set(firstOfSeason);
  }

  public int getSeasonNumberInt() {
    return getSeasonNumber().get();
  }

  public int getEpisodeNumberInt() {
    return getEpisodeNumber().get();
  }

  public boolean isFirstOfSeason() {
    return this.firstOfSeason.get();
  }

}
