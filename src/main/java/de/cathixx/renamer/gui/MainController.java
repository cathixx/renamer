package de.cathixx.renamer.gui;

import de.cathixx.renamer.apiadapter.TVShowApiAdapter;
import de.cathixx.renamer.apiadapter.TheMovieDBAdapter;
import de.cathixx.renamer.data.DirectoryInfos;
import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.EpisodeTableItem;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import de.cathixx.renamer.gui.MainView.TaskType;
import de.cathixx.renamer.util.DelayedListener;
import de.cathixx.renamer.util.TVShowFileSupport;
import de.cathixx.renamer.util.TVShowRenamer;
import de.cathixx.renamer.util.TaskExecutor;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainController {

  private final MainModel model;

  private final MainView view;

  private final TaskExecutor executor;

  private final TVShowApiAdapter tvAdapter;

  public MainController(Stage primaryStage, ResourceBundle resources, DirectoryInfos directoryInfos) {
    this.model = new MainModel(primaryStage, directoryInfos, resources);
    this.view = new MainView(resources, this.model.getLoading());
    this.executor = new TaskExecutor(this.view, resources);
    this.tvAdapter = new TheMovieDBAdapter(this.view.getSelectableLanguages());
    initialize(directoryInfos);
    bindModelViewController();
  }

  private void bindModelViewController() {
    this.view.bindTVShowNameProperty(this.model.getTvShowName());
    this.view.bindLanguageProperty(this.model.getSelectedLanguage());
    this.view.bindTVShowList(this.model.getTvShows());
    this.view.bindEpisodesList(this.model.getEpisodes());
    this.view.bindSelectedTVShow(this.model.getSelectedTVShow());
    this.view.bindTVShowImage(this.model.getTvShowImage());
    this.view.bindIncludeTitle(this.model.getIncludeTitle());
    this.view.addRenameButtonClickListener(e -> renameClicked());
    this.model.getSelectedLanguage().addListener((obs, o, n) -> this.languageSelected(o, n));
    this.model.getIncludeTitle().addListener((obs, o, n) -> this.includeTitleSelected(o, n));
    this.model.getTvShowName().addListener(new DelayedListener<>(this::tvShowNameChanged, 1000));
    this.model.getSelectedTVShow().addListener((ob, o, n) -> updateEpisodeList());
  }

  private void initialize(DirectoryInfos directoryInfos) {
    this.view.setStatusText(directoryInfos.getMainDirectory().getAbsolutePath());
    this.model.getTvShowName().set(directoryInfos.getMainDirectory().getName());
    Collection<EpisodeTableItem> episodes = TVShowFileSupport
        .listEpisodes(this.model.getDirectoryInfos().getSeasonFiles());
    this.model.getEpisodes().get().setAll(episodes);
    episodes.forEach(e -> e.getNewName().addListener((ob, o, n) -> this.view.refreshTable()));
    episodes.forEach(e -> e.getSelected().addListener((ob, o, n) -> this.view.renameButtonEnabled()));
  }

  public void show() {
    this.view.show(this.model.getPrimaryStage());
    updateTvShowList();
  }

  private void renameClicked() {
    log.info("rename");
    this.executor.execute(TaskType.RENAME, this::rename, this::showFailedRenames, TaskExecutor.NOCACHE);
  }

  private Collection<EpisodeTableItem> rename() {
    Collection<EpisodeTableItem> result = TVShowRenamer.rename(this.model.getEpisodes().get());
    this.view.renameButtonEnabled();
    return result;
  }

  private void showFailedRenames(Collection<EpisodeTableItem> failed) {
    if (!failed.isEmpty()) {
      DialogSupport.showError("error.rename.failed", this.model.getResources(),
          failed.stream().map(e -> e.getFile().get().getName()).collect(Collectors.joining(", ")), false);
    }
  }

  private void tvShowNameChanged(final String oldValue, final String newValue) {
    log.debug("TV-show selected {}", newValue);
    updateTvShowList();
  }

  private void languageSelected(final Language oldValue, final Language newValue) {
    log.debug("language selected {}", newValue);
    updateEpisodeList();
  }

  private void includeTitleSelected(final boolean oldValue, final boolean newValue) {
    log.debug("include title {}", newValue);
    updateEpisodeList();
  }

  private void updateTvShowList() {
    String enteredName = this.model.getTvShowName().get().trim();
    if (enteredName.length() <= 2) {
      updateTvShowList(Collections.emptyList());
    } else {
      this.executor.execute(TaskType.TVSHOW,
          () -> this.tvAdapter.findTvShow(enteredName),
          this::updateTvShowList, enteredName);
    }
  }

  private void updateTvShowList(Collection<TVShow> tvShows) {
    this.model.getTvShows().get().setAll(tvShows);
    if (tvShows.isEmpty()) {
      resetEpisodes();
    } else {
      this.model.getSelectedTVShow().set(tvShows.iterator().next());
    }
  }

  private void updateEpisodeList() {
    TVShow selected = this.model.getSelectedTVShow().get();
    if (selected == null) {
      resetEpisodes();
    } else {
      if (selected.getBannerURL() != null) {
        this.executor.execute(TaskType.IMAGE, () -> loadImage(selected.getBannerURL()),
            this::updateImage, selected.getBannerURL());
      } else {
        this.model.getTvShowImage().set(null);
      }
      Language language = this.model.getSelectedLanguage().get();
      this.executor.execute(TaskType.EPISODES,
          () -> this.tvAdapter.findEpisodes(selected, language),
          this::updateEpisodeList, selected, language);
    }
  }

  private Image loadImage(String url) {
    return new Image(url);
  }

  private void updateImage(Image image) {
    this.model.getTvShowImage().set(image);
  }

  private void updateEpisodeList(Collection<Episode> episodes) {
    for (EpisodeTableItem episode : this.model.getEpisodes().get()) {
      String newName = null;
      Episode info = episodes.stream().filter(e -> sameEpisode(episode, e)).findAny().orElse(null);
      if (info != null) {
        newName = TVShowFileSupport.createNewName(info, this.model.getSelectedLanguage().get(),
            this.model.getIncludeTitle().get());
      }
      episode.getNewName().set(newName);
    }
  }

  private void resetEpisodes() {
    this.model.getTvShowImage().set(null);
    this.model.getEpisodes().get().forEach(e -> e.getNewName().set(null));
    this.view.refreshTable();
  }

  private static boolean sameEpisode(EpisodeTableItem e1, Episode e2) {
    return e1.getSeasonNumber().get() == e2.getSeasonNumber() && e1.getEpisodeNumber().get() == e2.getEpisodeNumber();
  }

}
