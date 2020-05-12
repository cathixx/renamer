package de.cathixx.renamer.gui;

import de.cathixx.renamer.data.EpisodeTableItem;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

public class EpisodeTableSupport {

  private final TableView<EpisodeTableItem> viewEpisodesTable;

  private final ResourceBundle resources;

  private final BooleanProperty loading;

  private EpisodeTableSupport(TableView<EpisodeTableItem> viewEpisodesTable, ResourceBundle resources,
      BooleanProperty loading) {
    this.viewEpisodesTable = viewEpisodesTable;
    this.resources = resources;
    this.loading = loading;
  }

  @SuppressWarnings("unchecked")
  private void init() {
    ObservableList<TableColumn<EpisodeTableItem, ?>> columns = this.viewEpisodesTable.getColumns();
    columns.clear();
    TableColumn<EpisodeTableItem, Boolean> colCheckbox = createCheckboxColumn();
    TableColumn<EpisodeTableItem, Number> colNumber = createNumberColumn();
    TableColumn<EpisodeTableItem, String> colFileName = createOldNameColumn();
    TableColumn<EpisodeTableItem, String> colName = createNewNameColumn();
    columns.addAll(colCheckbox, colNumber, colFileName, colName);
    this.viewEpisodesTable.setEditable(true);
  }

  private TableColumn<EpisodeTableItem, Boolean> createCheckboxColumn() {
    String label = this.resources.getString("view.comp.table.episodes.column.checkbox");
    TableColumn<EpisodeTableItem, Boolean> result = new TableColumn<>(label);
    result.setSortable(false);
    result.setPrefWidth(30);
    result.setCellFactory(c -> {
      CheckBoxTableCell checkBoxTableCell = new CheckBoxTableCell();
      this.loading.addListener((ob, o, n) -> this.viewEpisodesTable.refresh());
      return checkBoxTableCell;
    });
    result.setCellValueFactory(d -> d.getValue().getSelected());
    return result;
  }

  private TableColumn<EpisodeTableItem, Number> createNumberColumn() {
    String label = this.resources.getString("view.comp.table.episodes.column.number");
    TableColumn<EpisodeTableItem, Number> result = new TableColumn<>(label);
    result.setSortable(false);
    result.setEditable(false);
    result.setPrefWidth(50);
    result.setCellFactory(c -> new NumberTableCell());
    result.setCellValueFactory(d -> d.getValue().getNumber());
    return result;
  }

  private TableColumn<EpisodeTableItem, String> createOldNameColumn() {
    String label = this.resources.getString("view.comp.table.episodes.column.oldname");
    TableColumn<EpisodeTableItem, String> result = new TableColumn<>(label);
    result.setSortable(false);
    result.setEditable(false);
    result.setPrefWidth(400);
    result.setCellFactory(c -> new OldNameTableCell());
    result.setCellValueFactory(d -> d.getValue().getOldName());
    return result;
  }

  private TableColumn<EpisodeTableItem, String> createNewNameColumn() {
    String label = this.resources.getString("view.comp.table.episodes.column.newname");
    TableColumn<EpisodeTableItem, String> result = new TableColumn<>(label);
    result.setSortable(false);
    result.setEditable(false);
    result.setPrefWidth(400);
    result.setCellFactory(c -> {
      NewNameTableCell newNameTableCell = new NewNameTableCell();
      EpisodeTableSupport.this.loading.addListener((ob, o, n) -> this.viewEpisodesTable.refresh());
      return newNameTableCell;
    });
    result.setCellValueFactory(d -> d.getValue().getNewName());
    return result;
  }

  private static EpisodeTableItem getRowItem(TableCell<EpisodeTableItem, ?> tableCell) {
    EpisodeTableItem result = null;
    TableRow<EpisodeTableItem> row = tableCell.getTableRow();
    Optional<EpisodeTableItem> op = Optional.ofNullable(row).map(TableRow::getItem);
    if (op.isPresent()) {
      result = op.get();
    }
    return result;
  }

  private static void setRowStyle(EpisodeTableItem item, StringBuilder style) {
    if (item != null && item.isFirstOfSeason()) {
      style.append("-fx-border-color: black transparent transparent transparent;");
    } else {
      style.append("-fx-border-color: transparent;");
    }
  }

  private static boolean isNameChange(EpisodeTableItem item) {
    String oldName = item.getOldName().get();
    String newName = item.getNewName().get();
    return newName != null && !oldName.equals(newName);
  }

  private static boolean isMinorNameChange(EpisodeTableItem item) {
    String oldName = item.getOldName().get();
    String newName = item.getNewName().get();
    return newName != null && !oldName.equals(newName) && oldName.equalsIgnoreCase(newName);
  }

  private static void setBoldStyle(EpisodeTableItem item, StringBuilder style) {
    if (isMinorNameChange(item)) {
      style.append("-fx-text-fill: navy;");
    } else if (isNameChange(item)) {
      style.append("-fx-font-weight: bold;");
    }
  }

  private class CheckBoxTableCell extends javafx.scene.control.cell.CheckBoxTableCell<EpisodeTableItem, Boolean> {

    @Override
    public void updateItem(Boolean value, boolean empty) {
      super.updateItem(value, empty);
      StringBuilder style = new StringBuilder();
      EpisodeTableItem item = null;
      if (!empty) {
        item = getRowItem(this);
        getGraphic().setVisible(item != null && !EpisodeTableSupport.this.loading.get() && isNameChange(item));
      }
      setRowStyle(item, style);
      setStyle(style.toString());
    }
  }

  private static class NumberTableCell extends AbstractTextTableCell<Number> {

    @Override
    public void update(EpisodeTableItem item, StringBuilder style) {
      setText(String.format("%03d", item.getNumber().get()));
    }
  }

  private static class OldNameTableCell extends AbstractTextTableCell<String> {

    @Override
    public void update(EpisodeTableItem item, StringBuilder style) {
    }
  }

  private class NewNameTableCell extends AbstractTextTableCell<String> {

    private final String notFoundString;

    private final String loadingString;

    NewNameTableCell() {
      this.notFoundString = EpisodeTableSupport.this.resources.getString("view.comp.table.episode.notfound");
      this.loadingString = EpisodeTableSupport.this.resources.getString("view.comp.table.episode.load");
    }

    @Override
    public void update(EpisodeTableItem item, StringBuilder style) {
      String value = item.getNewName().get();
      if (EpisodeTableSupport.this.loading.get()) {
        style.append("-fx-font-style: italic;-fx-text-fill: lightgrey;");
        setText(this.loadingString);
      } else if (value == null || value.isEmpty()) {
        style.append("-fx-font-style: italic;-fx-text-fill: firebrick;");
        setText(this.notFoundString);
      } else {
        setBoldStyle(item, style);
      }
    }
  }

  private abstract static class AbstractTextTableCell<T> extends TextFieldTableCell<EpisodeTableItem, T> {

    @Override
    public void updateItem(T value, boolean empty) {
      super.updateItem(value, empty);
      StringBuilder style = new StringBuilder();
      if (empty) {
        setRowStyle(null, style);
        setText(null);
      } else {
        setText(value != null ? value.toString() : "");
        EpisodeTableItem item = getRowItem(this);
        if (item != null) {
          update(item, style);
        }
        setRowStyle(item, style);
      }
      setStyle(style.toString());
    }

    public abstract void update(EpisodeTableItem item, StringBuilder style);
  }

  static void initializeTable(TableView<EpisodeTableItem> viewEpisodesTable, ResourceBundle resources,
      BooleanProperty loading) {
    new EpisodeTableSupport(viewEpisodesTable, resources, loading).init();
  }

}
