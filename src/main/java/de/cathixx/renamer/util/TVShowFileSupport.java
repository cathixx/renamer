package de.cathixx.renamer.util;

import de.cathixx.renamer.data.DirectoryInfos;
import de.cathixx.renamer.data.Episode;
import de.cathixx.renamer.data.EpisodeTableItem;
import de.cathixx.renamer.data.Language;
import de.cathixx.renamer.gui.DialogSupport;
import java.io.File;
import java.io.FileFilter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TVShowFileSupport {

  private static final Pattern SEASON_DIR_PATTERN = Pattern.compile("^[a-zA-Z]+ ([\\d]+).*$");

  private static String[] FILE_EXTENSIONS = {".avi", ".mkv", ".mpg", ".mp4", ".mov"};

  private static final FileFilter EPISODE_FILTER = file -> {
    if (!file.isFile()) {
      return false;
    }
    String name = file.getName().toLowerCase();
    for (String ext : FILE_EXTENSIONS) {
      if (name.endsWith(ext)) {
        return true;
      }
    }
    return false;
  };

  public static DirectoryInfos handleArgs(List<String> args, ResourceBundle res) {
    if (args.size() == 0) {
      DialogSupport.showErrorWithException("error.args.missing", res);
    }
    String path = args.get(0);
    log.debug("argument path: '{}'", path);
    File file = new File(path);
    if (!file.isDirectory()) {
      DialogSupport.showErrorWithException("error.args.nodir.text", res, path);
    }
    List<File> files;
    if (file.getName().startsWith(res.getString("application.dir.season.name"))) {
      files = args.stream().map(File::new).collect(Collectors.toList());
      file = file.getParentFile();
    } else {
      files = Arrays.asList(Objects.requireNonNull(file.listFiles(TVShowFileSupport::filterSeasonDirs)));
    }
    files.sort(TVShowFileSupport::compare);
    log.debug("tv show path: '{}'", file.getAbsolutePath());
    log.debug("seasons: {}",
        files.stream().map(s -> Integer.toString(getSeasonNumber(s))).collect(Collectors.joining(", ")));
    if (files.isEmpty()) {
      DialogSupport.showErrorWithException("error.args.noseasons", res);
    }
    return new DirectoryInfos(file, files);
  }

  private static int compare(File o1, File o2) {
    Matcher matcher1 = SEASON_DIR_PATTERN.matcher(o1.getName());
    Matcher matcher2 = SEASON_DIR_PATTERN.matcher(o2.getName());
    if (matcher1.find() && matcher2.find()) {
      return Integer.valueOf(matcher1.group(1)).compareTo(Integer.valueOf(matcher2.group(1)));
    } else {
      throw new RuntimeException();
    }
  }

  private static int getSeasonNumber(File file) {
    Matcher matcher = SEASON_DIR_PATTERN.matcher(file.getName());
    if (!matcher.find()) {
      throw new RuntimeException();
    }
    return Integer.parseInt(matcher.group(1));
  }

  private static boolean filterSeasonDirs(File f) {
    return f.isDirectory() && SEASON_DIR_PATTERN.matcher(f.getName()).matches();
  }

  public static Collection<EpisodeTableItem> listEpisodes(Collection<File> seasonFiles) {
    List<EpisodeTableItem> result = new ArrayList<>();
    for (File seasonDir : seasonFiles) {
      Matcher matcher = SEASON_DIR_PATTERN.matcher(seasonDir.getName());
      if (matcher.find()) {
        int seasonNumber = Integer.parseInt(matcher.group(1));
        File[] episodeFiles = seasonDir.listFiles(EPISODE_FILTER);
        Arrays.sort(episodeFiles, Comparator.comparing(File::getName));
        boolean firstOfSeason = !result.isEmpty();
        List<File> missing = new ArrayList<>();
        BitSet found = new BitSet();
        for (File file : episodeFiles) {
          Integer episodeNumber = getEpisodeNumber(file, seasonNumber);
          if (episodeNumber != null) {
            found.set(episodeNumber);
            addEpisode(result, seasonNumber, firstOfSeason, file, getName(file), episodeNumber);
            firstOfSeason = false;
          } else {
            missing.add(file);
          }
        }
        for (File file : missing) {
          for (int episodeNumber = 1; episodeNumber < 1000; episodeNumber++) {
            if (!found.get(episodeNumber)) {
              found.set(episodeNumber);
              addEpisode(result, seasonNumber, firstOfSeason, file, getName(file), episodeNumber);
              break;
            }
          }
        }
      }
    }
    result.sort(Comparator.comparingInt(EpisodeTableItem::getSeasonNumberInt)
        .thenComparingInt(EpisodeTableItem::getEpisodeNumberInt));
    return result;
  }

  private static void addEpisode(Collection<EpisodeTableItem> result, int seasonNumber, boolean firstOfSeason,
      File file, String name, Integer episodeNumber) {
    EpisodeTableItem item = new EpisodeTableItem(file, seasonNumber, episodeNumber, firstOfSeason);
    item.getOldName().set(name);
    result.add(item);
    item.getNumber().set(result.size());
    item.getSelected().set(true);
  }

  private static String getName(File file) {
    String name = file.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    return name;
  }

  private static Integer getEpisodeNumber(File file, int seasonNumber) {
    String fileName = file.getName();
    final String[] removeStr = {"-x264-"};
    final String[] patterns = {"(?i:S)%S(?i:E)%E", "(?i:S)%S(?i:x)%E", "%S(?i:x)%E", "(?i:S)%S.(?i:E)%E", "- %S%E -",
        "(?i:Ep)%E", "-(?i:E)%E-", "%S-%E", "%S%E$", "^%E$"};
    for (String remove : removeStr) {
      fileName = fileName.replaceAll(remove, "");
    }
    for (String patternString : patterns) {
      for (int i = 2; i >= 1; i--) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(i);
        String ps = patternString.replace("%S", nf.format(seasonNumber));
        ps = ps.replace("%E", "([\\d][\\d])");
        Pattern pattern = Pattern.compile(ps);
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
          return Integer.valueOf(matcher.group(1));
        }
      }
    }
    log.warn("no pattern matches to " + fileName);
    return null;
  }

  static File createNewFileName(File oldFile, String newName) {
    String extension = oldFile.getName().substring(oldFile.getName().lastIndexOf('.'));
    String newFileName = newName + extension;
    return new File(oldFile.getParent(), newFileName);
  }

  public static String createNewName(Episode episode, Language language, boolean includeShowTitle) {
    StringBuilder title = new StringBuilder();
    title.append(String.format("S%02dE%02d", episode.getSeasonNumber(), episode.getEpisodeNumber()));
    if (includeShowTitle) {
      Optional<String> showTitle = Optional.of(episode.getShowNames()).map(n -> n.get(language));
      if (showTitle.isPresent()) {
        title.insert(0, " - ");
        title.insert(0, replaceMultipleWhitespaces(removeUnallowedChars(showTitle.get())));
      }
    }
    String episodeName = Optional.of(episode.getNames()).map(n -> n.get(language)).orElse("");
    Pattern pattern = Pattern.compile("[.]*([^.].*[^.])[.]*");
    Matcher matcher = pattern.matcher(episodeName);
    if (matcher.find()) {
      episodeName = matcher.group(1);
    }
    title.append(" - ");
    title.append(replaceMultipleWhitespaces(removeUnallowedChars(episodeName)));
    return title.toString();
  }

  private static String replaceMultipleWhitespaces(String str) {
    str = str.trim();
    Pattern pattern = Pattern.compile("^(.*[^\\s])([\\s][\\s]+)([^\\s].*)$");
    Matcher matcher;
    while ((matcher = pattern.matcher(str)).matches()) {
      str = matcher.group(1) + " " + matcher.group(3);
    }
    return str;
  }

  private static String removeUnallowedChars(String fileName) {
    fileName = fileName.replace(":", "");
    fileName = fileName.replace("?", "");
    fileName = fileName.replace("/", " ");
    fileName = fileName.replace("\\", " ");
    fileName = fileName.replace("*", " ");
    fileName = fileName.replace("\"", "'");
    fileName = fileName.replace("<", "");
    fileName = fileName.replace(">", "");
    fileName = fileName.replace("|", "");
    fileName = fileName.replace('–', '-');
    fileName = fileName.replace("...", "…");
    fileName = fileName.replace('’', '\'');
    fileName = fileName.replace('„', '\'');
    fileName = fileName.replace('“', '\'');
    fileName = fileName.replace("!", "");
    return fileName;
  }
}
