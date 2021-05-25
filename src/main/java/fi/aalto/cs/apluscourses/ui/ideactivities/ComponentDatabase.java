package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.openapi.editor.impl.EditorComponentImpl;
import fi.aalto.cs.apluscourses.ui.exercise.ExercisesView;
import fi.aalto.cs.apluscourses.ui.module.ModulesView;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nullable;

public class ComponentDatabase {
  public static @Nullable Component getProjectPane() {
    Component component = ComponentLocator.getComponentByClass("ProjectViewPane");
    return component == null ? null : component.getParent();
  }

  public static @Nullable Component getEditorWindow() {
    return ComponentLocator.getComponentByClass("EditorWindow");
  }

  public static @Nullable EditorComponentImpl getEditorComponent() {
    return (EditorComponentImpl) ComponentLocator.getComponentByClass("EditorComponentImpl");
  }

  public static @Nullable EditorComponentImpl getEditorComponent(String fileNameSubstring) {
    var editors = ComponentLocator.getComponentsByClass("EditorComponentImpl");
    for (var editorComponent : editors) {
      var editor = (EditorComponentImpl) editorComponent;
      if (editor.getEditor().getVirtualFile().getName().contains(fileNameSubstring)) {
        return editor;
      }
    }
    return null;
  }

  public static @Nullable Component getReplPanel() {
    return ComponentLocator.getComponentsByClass("InternalDecorator").stream()
        .filter(c -> ComponentLocator.isComponentOfClass(c.getParent(), "ThreeComponentsSplitter"))
        .filter(c -> ComponentLocator.hasChildOfClass((Container) c, "Repl"))
        .findFirst().orElse(null);
  }

  public static @Nullable Component getModuleList() {
    return ComponentLocator.getComponentsByClass("JPanel").stream()
        .filter(c -> ((JComponent) c).getClientProperty(ModulesView.class.getName()) != null)
        .findFirst().orElse(null);
  }

  public static @Nullable Component getExerciseList() {
    return ComponentLocator.getComponentsByClass("JPanel").stream()
            .filter(c -> ((JComponent) c).getClientProperty(ExercisesView.class.getName()) != null)
            .findFirst().orElse(null);
  }

  public static @Nullable Component getAPlusToolWindow() {
    var moduleList = getExerciseList();
    return (moduleList == null) ? null : moduleList.getParent();
  }

  private ComponentDatabase() {

  }
}
