package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import fi.aalto.cs.apluscourses.ui.ideactivities.ComponentDatabase;
import fi.aalto.cs.apluscourses.ui.ideactivities.OverlayPane;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;

public class ComponentDatabaseAction extends DumbAwareAction {

  private OverlayPane overlay;

  private int index = -1;

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (overlay == null) {
      overlay = OverlayPane.installOverlay();
      index = 0;
    }

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
