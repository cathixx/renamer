package de.cathixx.renamer.util;

import de.cathixx.renamer.data.EpisodeTableItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TVShowRenamer {

  public static Collection<EpisodeTableItem> rename(Collection<EpisodeTableItem> tvShows) {
    Collection<EpisodeTableItem> failed = new ArrayList<>();
    for (EpisodeTableItem tableItem : tvShows) {
      if (isRename(tableItem)) {
        log.info("rename '{}' -> '{}'", tableItem.getOldName().get(), tableItem.getNewName().get());
        File file = tableItem.getFile().get();
        String newName = tableItem.getNewName().get();
        File newFile = TVShowFileSupport.createNewFileName(file, newName);
        boolean success = file.renameTo(newFile);
        if (success) {
          tableItem.getFile().set(newFile);
          tableItem.getOldName().set(newName);
        } else {
          failed.add(tableItem);
        }
      }
    }
    return failed;
  }

  public static boolean isRename(EpisodeTableItem tableItem) {
    return tableItem.getSelected().get() && tableItem.getNewName().get() != null &&
        !tableItem.getOldName().get().equals(tableItem.getNewName().get());
  }
}
