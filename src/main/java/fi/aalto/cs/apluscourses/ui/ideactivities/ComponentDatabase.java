package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.openapi.ui.ThreeComponentsSplitter;
import java.awt.Component;
import java.awt.Container;
import java.util.Optional;

import com.intellij.openapi.wm.impl.ToolWindowsPane;
import org.jetbrains.annotations.Nullable;

public class ComponentDatabase {
  public static @Nullable Component getProjectPane() {
    Component component = ComponentLocator.getComponentByClass("ProjectViewPane");
    return component == null ? null : component.getParent();
  }

  public static @Nullable Component getEditorWindow() {
    return ComponentLocator.getComponentByClass("EditorWindow");
  }

  public static @Nullable Component getReplPanel() {
    return ComponentLocator.getComponentsByClass("InternalDecorator").stream()
        .filter(c -> ComponentLocator.isComponentOfClass(c.getParent(), "ThreeComponentsSplitter"))
        //.filter(c -> ComponentLocator.isComponentOfClass(c.getParent().getParent(), "ToolWindowsPane"))
        .filter(c -> ComponentLocator.hasChildOfClass((Container) c, "Repl"))
        .findFirst().orElse(null);
  }

  private ComponentDatabase() {

  }
}
