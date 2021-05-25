package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.ui.ideactivities.OverlayPane;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Please ignore this class in any potential code reviews.
 * This class is only used for testing purposes, and will be removed in production.
 */
public class ComponentDatabaseAction extends DumbAwareAction {

  private OverlayPane overlay;

  private int index = -1;

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (overlay == null) {
      overlay = OverlayPane.installOverlay();
      index = 0;
    }

    overlay.showEditor(ComponentDatabase.getEditorComponent("GoodStuff"), Arrays.asList(14, 15, 16));
    overlay.showEditor(ComponentDatabase.getEditorComponent("CategoryDisplay"), Arrays.asList(1, 2, 3, 5, 7, 8, 9, 55, 56, 88));

    if (true) return;

    Component c = null;
    switch (index) {
      case 0:
        c = ComponentDatabase.getProjectPane();
        break;
      case 1:
        c = ComponentDatabase.getEditorWindow();
        break;
      case 2:
        c = ComponentDatabase.getReplPanel();
        break;
      case 3:
        c = ComponentDatabase.getModuleList();
        break;
      case 4:
        c = ComponentDatabase.getExerciseList();
        break;
      case 5:
        c = ComponentDatabase.getAPlusToolWindow();
        break;
      default:
    }

    if (c == null) {
      overlay.remove();
      overlay = null;
      return;
    }

    overlay.reset();
    overlay.showComponent(c);
    index++;
  }
}
