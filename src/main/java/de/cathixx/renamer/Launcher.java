package de.cathixx.renamer;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point of the application.
 * <p>
 * The main method must not be located in {@link Main}: if the main class extends {@link Application}, the java launcher
 * requires JavaFX on the module path and aborts with "JavaFX runtime components are missing" when it's only on the
 * classpath (e.g. fat jar, IntelliJ run configuration).
 */
@Slf4j
public class Launcher {

  public static void main(String[] args) {
    log.info("start");
    Application.launch(Main.class, args);
  }
}
