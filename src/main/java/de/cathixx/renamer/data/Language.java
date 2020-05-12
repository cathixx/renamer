package de.cathixx.renamer.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Language {

  private final String apiName;
  private final String label;

  @Override
  public String toString() {
    return this.label;
  }
}
