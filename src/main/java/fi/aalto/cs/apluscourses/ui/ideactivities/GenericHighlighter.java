package fi.aalto.cs.apluscourses.ui.ideactivities;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Rectangle;

public class GenericHighlighter {

  private final @NotNull Component component;

  public @NotNull Component getComponent() {
    return component;
  }

  public Rectangle[] getArea() {
    return new Rectangle[] { new Rectangle(0, 0, component.getWidth(), component.getHeight()) };
  }

  public GenericHighlighter(@NotNull Component component) {
    this.component = component;
  }
}
