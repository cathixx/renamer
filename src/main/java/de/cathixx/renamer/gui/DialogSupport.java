package de.cathixx.renamer.gui;

import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class DialogSupport {

  public static void showErrorWithException(String property, ResourceBundle res) {
    showErrorWithException(property, res, null);
  }

  public static void showErrorWithException(String property, ResourceBundle res, String repl) {
    showError(property, res, repl, false);
    throw new RuntimeException(res.getString(property + ".text"));
  }

  public static void showError(String property, ResourceBundle res, String repl, boolean close) {
    String title = res.getString(property + ".title");
    String header = res.getString(property + ".header");
    String text = res.getString(property + ".text");
    if (repl != null) {
      text = text.replace("{}", repl);
    }
    showDialog(AlertType.ERROR, title, header, text, close);
  }

  private static void showDialog(AlertType alertType, String title, String header, String text, boolean close) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(text);
    alert.showAndWait();
    if (close) {
      System.exit(1);
    }
  }
}
