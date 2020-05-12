package de.cathixx.renamer.util;

import de.cathixx.renamer.gui.DialogSupport;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutor {

  public static final Object NOCACHE = new Object();

  private static final Logger LOG = LoggerFactory.getLogger(TaskExecutor.class);

  private final Map<MultiKey, Object> cache = new HashMap<>();

  private final Map<Object, Consumer<?>> currentTasks = new HashMap<>();

  private final ProgressUpdater progress;

  private final ResourceBundle resources;

  public TaskExecutor(ProgressUpdater progress, ResourceBundle resources) {
    this.progress = progress;
    this.resources = resources;
  }

  @SuppressWarnings("unchecked")
  public <T> void execute(Object type, Supplier<T> supplier, Consumer<T> consumer, Object... data) {
    synchronized (this.currentTasks) {
      Object result = this.cache.get(new MultiKey(type, data));
      if (data[0] != NOCACHE && result != null) {
        this.currentTasks.remove(type);
        this.progress.setShowProgress(Collections.unmodifiableCollection(this.currentTasks.keySet()));
        consumer.accept((T) result);
        return;
      }
      this.currentTasks.put(type, consumer);
      this.progress.setShowProgress(Collections.unmodifiableCollection(this.currentTasks.keySet()));
    }
    Thread thread = new Thread(() -> run(type, supplier, consumer, data));
    thread.setDaemon(true);
    thread.start();
  }

  private <T> void run(Object type, Supplier<T> supplier, Consumer<T> consumer, Object... data) {
    try {
      final T result = supplier.get();
      Platform.runLater(() -> callback(type, consumer, result, data));
    } catch (RuntimeException e) {
      Platform.runLater(() -> failedcallback(type, consumer, e));
    }
  }

  private <T> void callback(Object type, Consumer<T> consumer, T result, Object... data) {
    try {
      synchronized (this.currentTasks) {
        if (data[0] != NOCACHE) {
          this.cache.put(new MultiKey(type, data), result);
        }
        Consumer<?> current = this.currentTasks.get(type);
        if (consumer == current) {
          this.currentTasks.remove(type);
          this.progress.setShowProgress(Collections.unmodifiableCollection(this.currentTasks.keySet()));
          consumer.accept(result);
        }
      }
    } catch (RuntimeException e) {
      DialogSupport.showError("error.exception", this.resources, e.toString(), false);
    }
  }

  private <T> void failedcallback(Object type, Consumer<T> consumer, RuntimeException e) {
    LOG.error("request failed", e);
    synchronized (this.currentTasks) {
      Consumer<?> current = this.currentTasks.get(type);
      if (consumer == current) {
        this.currentTasks.remove(type);
        this.progress.setShowProgress(Collections.unmodifiableCollection(this.currentTasks.keySet()));
      }
    }
    DialogSupport.showError("error.exception", this.resources, e.toString(), false);
  }

  public interface ProgressUpdater {

    void setShowProgress(Collection<Object> loadingTypes);
  }

  public static class MultiKey {

    private final Object[] keys;

    MultiKey(Object key, Object... keys) {
      this.keys = Stream.concat(Stream.of(key), Arrays.stream(keys)).toArray();
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.keys);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MultiKey other = (MultiKey) obj;
      return Arrays.equals(this.keys, other.keys);
    }
  }

}
