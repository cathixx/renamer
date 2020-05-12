package de.cathixx.renamer;

import de.cathixx.renamer.data.DirectoryInfos;
import de.cathixx.renamer.gui.MainController;
import de.cathixx.renamer.util.TVShowFileSupport;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main extends Application {

  public static String BUNDLE = "de.cathixx.renamer.bundles.main";

  @Override
  public void start(Stage primaryStage) {
    ResourceBundle res = ResourceBundle.getBundle(BUNDLE, Locale.GERMAN);
    DirectoryInfos directoryInfos = TVShowFileSupport.handleArgs(getParameters().getRaw(), res);
    primaryStage.setTitle(getTitleString(res));
    MainController ctrl = new MainController(primaryStage, res, directoryInfos);
    ctrl.show();
  }

  private static String getTitleString(ResourceBundle res) {
    String baseTitleString = res.getString("application.title");
    String version = Optional.ofNullable(Main.class.getPackage().getImplementationVersion())
        .orElse("snapshot");
    return baseTitleString.replace("{}", version);
  }

  public static void main(String[] args) {
    log.info("start");
    launch(args);
  }

}
