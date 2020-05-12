package de.cathixx.renamer.gui;

import de.cathixx.renamer.data.DirectoryInfos;
import de.cathixx.renamer.data.EpisodeTableItem;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainModel {

  private final Stage primaryStage;

  private final DirectoryInfos directoryInfos;

  private final ResourceBundle resources;

  private final ObjectProperty<Language> selectedLanguage = new SimpleObjectProperty<>();

  private final ObjectProperty<String> tvShowName = new SimpleObjectProperty<>();

  private final ObjectProperty<ObservableList<TVShow>> tvShows = new SimpleObjectProperty<>(
      FXCollections.observableArrayList());

  private final ObjectProperty<ObservableList<EpisodeTableItem>> episodes = new SimpleObjectProperty<>(
      FXCollections.observableArrayList());

  private final ObjectProperty<TVShow> selectedTVShow = new SimpleObjectProperty<>();

  private final BooleanProperty loading = new SimpleBooleanProperty();

  private final ObjectProperty<Image> tvShowImage = new SimpleObjectProperty<>();

  private final BooleanProperty includeTitle = new SimpleBooleanProperty();

  public MainModel(Stage primaryStage, DirectoryInfos directoryInfos, ResourceBundle resources) {
    this.primaryStage = primaryStage;
    this.directoryInfos = directoryInfos;
    this.resources = resources;
  }

  public Stage getPrimaryStage() {
    return this.primaryStage;
  }

  public DirectoryInfos getDirectoryInfos() {
    return this.directoryInfos;
  }

  public ObjectProperty<Language> getSelectedLanguage() {
    return this.selectedLanguage;
  }

  public ObjectProperty<String> getTvShowName() {
    return this.tvShowName;
  }

  public ObjectProperty<ObservableList<TVShow>> getTvShows() {
    return this.tvShows;
  }

  public ObjectProperty<ObservableList<EpisodeTableItem>> getEpisodes() {
    return this.episodes;
  }

  public ObjectProperty<TVShow> getSelectedTVShow() {
    return this.selectedTVShow;
  }

  public BooleanProperty getLoading() {
    return this.loading;
  }

  public BooleanProperty getIncludeTitle() {
    return includeTitle;
  }

  public ObjectProperty<Image> getTvShowImage() {
    return this.tvShowImage;
  }

  public ResourceBundle getResources() {
    return this.resources;
  }

}
