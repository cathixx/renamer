package de.cathixx.renamer.data;

import java.io.File;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DirectoryInfos {

  private final File mainDirectory;

  private final List<File> seasonFiles;
}
