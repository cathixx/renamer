package de.cathixx.renamer.util;

import java.util.function.BiConsumer;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.Duration;

public class DelayedListener<T> implements ChangeListener<T> {

  private final PauseTransition pauseTransition;

  private final BiConsumer<T, T> consumer;

  public DelayedListener(BiConsumer<T, T> consumer, int millies) {
    this.pauseTransition = new PauseTransition(Duration.millis(millies));
    this.consumer = consumer;
  }

  @Override
  public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
    this.pauseTransition.setOnFinished(event -> this.consumer.accept(oldValue, newValue));
    this.pauseTransition.playFromStart();
  }
}
