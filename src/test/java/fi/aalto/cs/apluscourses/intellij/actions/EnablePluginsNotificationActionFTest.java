package fi.aalto.cs.apluscourses.intellij.actions;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnActionEvent;
import fi.aalto.cs.apluscourses.PluginsTestHelper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

public class EnablePluginsNotificationActionFTest extends PluginsTestHelper {

  @Test
  public void testActionPerformed() {
    //given
    List<IdeaPluginDescriptor> descriptorList = new ArrayList<>();
    IdeaPluginDescriptor ideaPluginDescriptor = getIdeaCorePluginDescriptor();
    descriptorList.add(ideaPluginDescriptor);
    boolean disabled = PluginManager
        .disablePlugin(ideaPluginDescriptor.getPluginId().getIdString());
    assertTrue("The core plugin is disabled.", disabled);

    //when
    EnablePluginsNotificationAction enableMissingPluginsAction =
        new EnablePluginsNotificationAction(descriptorList);

    AnActionEvent anActionEvent = Mockito.mock(AnActionEvent.class);
    Notification notification = Mockito.mock(Notification.class);
    enableMissingPluginsAction.actionPerformed(anActionEvent, notification);

    //then
    assertTrue("The core plugin is enabled.",
        ideaPluginDescriptor.isEnabled());
    assertEquals("Action text contains a name of a core plugin.",
        "Enable the required plugin(s) (IDEA CORE).",
        enableMissingPluginsAction.getTemplateText());
    verify(notification, times(1)).expire();
  }
}
