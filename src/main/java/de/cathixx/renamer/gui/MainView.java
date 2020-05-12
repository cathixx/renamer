package de.cathixx.renamer.gui;

import de.cathixx.renamer.data.EpisodeTableItem;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.data.TVShow;
import de.cathixx.renamer.util.TVShowRenamer;
import de.cathixx.renamer.util.TaskExecutor.ProgressUpdater;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class MainView implements Initializable, ProgressUpdater {

  public static String FILE_FXML = "/de/cathixx/renamer/view/main.fxml";

  private final ResourceBundle resources;

  private final Parent contentView;

  @FXML
  private ImageView viewTVShowImage;

  @FXML
  private TextField viewTVShowName;

  @FXML
  private ListView<TVShow> viewTVShowList;

  @FXML
  private ChoiceBox<Language> viewLanguageBox;

  @FXML
  private Button viewRenameButton;

  @FXML
  private TableView<EpisodeTableItem> viewEpisodesTable;

  @FXML
  private ProgressIndicator viewProgress;

  @FXML
  private Label viewStatusText;

  @FXML
  private CheckBox viewIncludeTitle;

  private final BooleanProperty loading;

  public MainView(ResourceBundle resources, BooleanProperty loading) {
    this.resources = resources;
    this.loading = loading;
    try {
      URL fxmlResource = this.getClass().getResource(FILE_FXML);
      FXMLLoader loader = new FXMLLoader(fxmlResource, resources);
      loader.setController(this);
      this.contentView = loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void show(Stage primaryStage) {
    primaryStage.setScene(new Scene(this.contentView));
    primaryStage.show();
  }

  @Override
  public void initialize(URL location, ResourceBundle res) {
    this.viewRenameButton.setText(this.resources.getString("view.comp.label.renamebutton"));
    initializeLanguages();
    this.viewTVShowList
        .setCellFactory(p -> new TVShowListCell(this.viewLanguageBox.getSelectionModel().selectedItemProperty()));
    EpisodeTableSupport.initializeTable(this.viewEpisodesTable, res, this.loading);
    this.viewLanguageBox.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> refreshTable());
    this.loading.addListener((ob, o, n) -> renameButtonEnabled());
  }

  public void refreshTable() {
    this.viewTVShowList.refresh();
    this.viewEpisodesTable.refresh();
    renameButtonEnabled();
  }

  private void initializeLanguages() {
    ObservableList<Language> langList = FXCollections.observableArrayList();
    this.viewLanguageBox.setItems(langList);
    String languagelist = this.resources.getString("view.comp.language.selections");
    String[] languages = languagelist.split(",");
    for (String lang : languages) {
      String propKey = "view.comp.language." + lang;
      String label = this.resources.getString(propKey + ".label");
      String api = this.resources.getString(propKey + ".api");
      Language info = new Language(api, label);
      langList.add(info);
    }
    this.viewLanguageBox.getSelectionModel().select(0);
  }

  @Override
  public void setShowProgress(Collection<Object> loadingTypes) {
    this.viewProgress.setVisible(!loadingTypes.isEmpty());
    this.viewTVShowList.setDisable(loadingTypes.contains(TaskType.TVSHOW) || loadingTypes.contains(TaskType.RENAME));
    this.viewEpisodesTable.setDisable(!loadingTypes.isEmpty());
    this.viewTVShowName.setDisable(loadingTypes.contains(TaskType.RENAME));
    this.viewLanguageBox.setDisable(loadingTypes.contains(TaskType.RENAME));
    this.loading.set(!loadingTypes.isEmpty());
  }

  public void renameButtonEnabled() {
    boolean ren = this.viewEpisodesTable.itemsProperty().get().stream().filter(TVShowRenamer::isRename).findAny()
        .isPresent();
    this.viewRenameButton.setDisable(this.loading.get() || !ren);
  }

  public Collection<Language> getSelectableLanguages() {
    return this.viewLanguageBox.getItems();
  }

  public void bindLanguageProperty(ObjectProperty<Language> selectedLanguage) {
    selectedLanguage.bind(this.viewLanguageBox.getSelectionModel().selectedItemProperty());
  }

  public void bindTVShowNameProperty(ObjectProperty<String> tvShowName) {
    this.viewTVShowName.textProperty().bindBidirectional(tvShowName);
  }

  public void setStatusText(String text) {
    this.viewStatusText.setText(text);
  }

  public void bindTVShowList(ObjectProperty<ObservableList<TVShow>> tvShows) {
    this.viewTVShowList.itemsProperty().bind(tvShows);
  }

  public void bindEpisodesList(ObjectProperty<ObservableList<EpisodeTableItem>> episodes) {
    this.viewEpisodesTable.itemsProperty().bind(episodes);
  }

  public void bindSelectedTVShow(ObjectProperty<TVShow> selectedTVShow) {
    this.viewTVShowList.selectionModelProperty().get().selectedItemProperty()
        .addListener((ob, o, n) -> selectedTVShow.set(n));
    selectedTVShow.addListener((ob, o, n) -> this.viewTVShowList.selectionModelProperty().get().select(n));
  }

  public void bindTVShowImage(ObjectProperty<Image> imageProperty) {
    this.viewTVShowImage.imageProperty().bind(imageProperty);
  }

  public void bindIncludeTitle(BooleanProperty property) {
    this.viewIncludeTitle.selectedProperty().bindBidirectional(property);
  }

  public void addRenameButtonClickListener(EventHandler<ActionEvent> eventHandler) {
    this.viewRenameButton.setOnAction(eventHandler);
  }

  private static class TVShowListCell extends ListCell<TVShow> {

    private final ReadOnlyObjectProperty<Language> language;

    public TVShowListCell(ReadOnlyObjectProperty<Language> language) {
      this.language = language;
    }

    @Override
    protected void updateItem(TVShow item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
      } else {
        String name = item.getName(this.language.get());
        if (item.getYear() != null) {
          name += " (" + item.getYear() + ")";
        }
        setText(name);
      }
    }
  }

  public enum TaskType {
    TVSHOW, EPISODES, RENAME, IMAGE;
  }

}
