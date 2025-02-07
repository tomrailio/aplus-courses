package fi.aalto.cs.apluscourses.intellij.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.model.CourseProject;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.CourseProjectProvider;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import fi.aalto.cs.apluscourses.utils.async.RepeatedTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserNameActionTest {
  private AnActionEvent event;
  private CourseProject courseProject;
  private UserNameAction action;

  /**
   * Called before each test.
   */
  @BeforeEach
  void setUp() throws Exception {
    event = mock(AnActionEvent.class);
    var project = mock(Project.class);
    when(event.getProject()).thenReturn(project);
    var presentation = new Presentation();
    when(event.getPresentation()).thenReturn(presentation);

    var course = new ModelExtensions.TestCourse("oe1");
    courseProject = new CourseProject(course,
        RepeatedTask.create(() -> {
        }),
        RepeatedTask.create(() -> {
        }),
        project, mock(Notifier.class));
    var courseProjectProvider = mock(CourseProjectProvider.class);
    when(courseProjectProvider.getCourseProject(project)).thenReturn(courseProject);

    action = new UserNameAction(courseProjectProvider);
  }

  @Test
  void testUserNameAction() {
    action.update(event);
    Assertions.assertEquals("Not Logged In", event.getPresentation().getText());
    var authentication = mock(Authentication.class);
    courseProject.setAuthentication(authentication);
    action.update(event);
    Assertions.assertEquals("test", event.getPresentation().getText());
  }
}
