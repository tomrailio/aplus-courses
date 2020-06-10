package fi.aalto.cs.apluscourses.intellij.actions;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import fi.aalto.cs.apluscourses.intellij.model.SettingsImporter;
import fi.aalto.cs.apluscourses.model.ComponentInstaller;
import fi.aalto.cs.apluscourses.model.ComponentInstallerImpl;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.presentation.CourseProjectViewModel;
import fi.aalto.cs.apluscourses.ui.courseproject.CourseProjectActionDialogs;
import fi.aalto.cs.apluscourses.utils.PostponedRunnable;
import fi.aalto.cs.apluscourses.utils.async.ImmediateTaskManager;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CourseProjectActionTest {

  private class TestDialogs implements CourseProjectActionDialogs {

    private boolean doCancel;
    private boolean doRestart;
    private boolean doOptOut;

    private String lastErrorMessage = "";

    public TestDialogs(boolean doCancel, boolean doRestart, boolean doOptOut) {
      this.doCancel = doCancel;
      this.doRestart = doRestart;
      this.doOptOut = doOptOut;
    }

    @NotNull
    public String getLastErrorMessage() {
      return lastErrorMessage;
    }

    @Override
    public boolean showMainDialog(@NotNull Project project,
                                  @NotNull CourseProjectViewModel courseProjectViewModel) {
      courseProjectViewModel.settingsOptOutProperty.set(doOptOut);
      courseProjectViewModel.restartProperty.set(doRestart);
      return !doCancel;
    }

    @Override
    public boolean showRestartDialog(@NotNull Project project) {
      return doRestart;
    }

    @Override
    public void showErrorDialog(@NotNull String message, @NotNull String title) {
      lastErrorMessage = message;
    }

  }

  private AnActionEvent anActionEvent;
  private Course emptyCourse;
  private DummySettingsImporter settingsImporter;
  private ComponentInstaller.Factory installerFactory;
  private AtomicInteger restarterCallCount;
  private PostponedRunnable ideRestarter;
  private TestDialogs dialogs;

  class DummySettingsImporter implements SettingsImporter {
    private int importIdeSettingsCallCount = 0;
    private int importProjectSettingsCallCount = 0;

    public int getImportIdeSettingsCallCount() {
      return importIdeSettingsCallCount;
    }

    public int getImportProjectSettingsCallCount() {
      return importProjectSettingsCallCount;
    }

    @Override
    public void importIdeSettings(@NotNull Course course) {
      ++importIdeSettingsCallCount;
    }

    @NotNull
    @Override
    public String currentlyImportedIdeSettings() {
      return "";
    }

    @Override
    public void importProjectSettings(@NotNull Project project, @NotNull Course course)
        throws IOException {
      ++importProjectSettingsCallCount;
    }
  }

  /**
   * Called before each test method call.  Initializes private fields.
   */
  @Before
  public void createMockObjects() {
    Project project = mock(Project.class);
    doReturn(FileUtilRt.getTempDirectory()).when(project).getBasePath();

    anActionEvent = mock(AnActionEvent.class);
    doReturn(project).when(anActionEvent).getProject();

    emptyCourse = new Course("EMPTY", Collections.emptyList(), Collections.emptyList(),
        Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());

    settingsImporter = new DummySettingsImporter();

    installerFactory = new ComponentInstallerImpl.FactoryImpl<>(new ImmediateTaskManager());

    restarterCallCount = new AtomicInteger(0);

    ideRestarter = new PostponedRunnable(restarterCallCount::incrementAndGet, Runnable::run);

    dialogs = new TestDialogs(false, true, false);
  }

  @Test
  public void testCreateCourseProject() {
    AtomicInteger courseFactoryCallCount = new AtomicInteger(0);
    CourseProjectAction action = new CourseProjectAction(
        (url, proj) -> {
          courseFactoryCallCount.getAndIncrement();
          return emptyCourse;
        },
        false,
        settingsImporter,
        installerFactory,
        ideRestarter,
        dialogs);

    action.actionPerformed(anActionEvent);

    Assert.assertEquals("CourseFactory#fromUrl should get called", 1,
        courseFactoryCallCount.get());

    Assert.assertEquals("The project settings should get imported", 1,
        settingsImporter.getImportProjectSettingsCallCount());
    Assert.assertEquals("The IDE settings should get imported", 1,
        settingsImporter.getImportIdeSettingsCallCount());
    Assert.assertEquals("The IDE should get restarted", 1, restarterCallCount.get());

    Assert.assertEquals("No error dialogs should be shown", "",
        dialogs.getLastErrorMessage());
  }

  @Test
  public void testNotifiesUserOfCourseInitializationError() {
    CourseProjectAction action = new CourseProjectAction(
        (url, proj) -> {
          throw new IOException();
        },
        false,
        settingsImporter,
        installerFactory,
        ideRestarter,
        dialogs);

    action.actionPerformed(anActionEvent);

    Assert.assertThat("The user is notified of the course initialization error",
        dialogs.getLastErrorMessage(), containsString("Please check your network connection"));
    Assert.assertEquals("Project settings are not imported after the error", 0,
        settingsImporter.getImportProjectSettingsCallCount());
    Assert.assertEquals("IDE settings are not imported after the error", 0,
        settingsImporter.getImportIdeSettingsCallCount());
  }

  @Test
  public void testNotifiesUserOfSettingsImportError() {
    DummySettingsImporter failingSettingsImporter = new DummySettingsImporter() {
      @Override
      public void importProjectSettings(@NotNull Project project, @NotNull Course course)
          throws IOException {
        super.importProjectSettings(project, course);
        throw new IOException();
      }
    };

    CourseProjectAction action = new CourseProjectAction(
        (url, proj) -> emptyCourse,
        false,
        failingSettingsImporter,
        installerFactory,
        ideRestarter,
        dialogs);

    action.actionPerformed(anActionEvent);

    Assert.assertEquals("The action attempted to import project settings", 1,
        failingSettingsImporter.getImportProjectSettingsCallCount());
    Assert.assertThat("The user is notified of the project settings error",
        dialogs.getLastErrorMessage(), containsString("Please check your network connection"));
  }

  @Test
  public void testDoesNotImportIdeSettingsIfOptOut() {
    CourseProjectAction action = new CourseProjectAction(
        (url, proj) -> emptyCourse,
        false,
        settingsImporter,
        installerFactory,
        ideRestarter,
        new TestDialogs(false, true, true));

    action.actionPerformed(anActionEvent);

    Assert.assertEquals("IDE settings are not imported", 0,
        settingsImporter.getImportIdeSettingsCallCount());
    Assert.assertEquals("Project settings are imported", 1,
        settingsImporter.getImportProjectSettingsCallCount());
  }

  @Test
  public void testLetsUserCancelAction() {
    CourseProjectAction action = new CourseProjectAction(
        (url, proj) -> emptyCourse,
        true,
        settingsImporter,
        installerFactory,
        ideRestarter,
        new TestDialogs(true, true, true));

    action.actionPerformed(anActionEvent);

    Assert.assertEquals("Project settings are not imported", 0,
        settingsImporter.getImportProjectSettingsCallCount());
    Assert.assertEquals("IDE settings are not imported", 0,
        settingsImporter.getImportIdeSettingsCallCount());
    Assert.assertEquals("The IDE is not restarted", 0, restarterCallCount.get());
  }

  @Test
  public void testDoesNotRestartIfCheckboxUnselected() {
    CourseProjectAction action = new CourseProjectAction(
        (url, proj) -> emptyCourse,
        false,
        settingsImporter,
        installerFactory,
        ideRestarter,
        new TestDialogs(false, false, false));

    action.actionPerformed(anActionEvent);

    Assert.assertEquals("IDE settings are imported", 1,
        settingsImporter.getImportIdeSettingsCallCount());
    Assert.assertEquals("The IDE is not restarted", 0, restarterCallCount.get());
  }
}
