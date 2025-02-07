package fi.aalto.cs.apluscourses.intellij.actions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.notifications.UrlRenderingErrorNotification;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmissionResult;
import fi.aalto.cs.apluscourses.model.UrlRenderer;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.base.SelectableNodeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseGroupViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionResultViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenItemActionTest {

  private Exercise exercise;
  private SubmissionResult submissionResult;
  private AnActionEvent actionEvent;
  private MainViewModelProvider mainViewModelProvider;
  private Notifier notifier;
  private UrlRenderer urlRenderer;

  /**
   * Called before each test.
   *
   * @param testableViewModel The SelectableNodeViewModel to be tested
   */
  void setUp(SelectableNodeViewModel<?> testableViewModel) {
    ExercisesTreeViewModel exercisesTree = mock(ExercisesTreeViewModel.class);
    doReturn(testableViewModel).when(exercisesTree).getSelectedItem();

    MainViewModel mainViewModel = new MainViewModel(new Options());
    mainViewModel.exercisesViewModel.set(exercisesTree);

    mainViewModelProvider = project -> mainViewModel;

    notifier = mock(Notifier.class);
    urlRenderer = mock(UrlRenderer.class);
    actionEvent = mock(AnActionEvent.class);
    doReturn(mock(Project.class)).when(actionEvent).getProject();
  }

  @Test
  void testOpenItemActionSubmission() throws Exception {
    var info = new SubmissionInfo(Collections.emptyMap());
    exercise = new Exercise(223, "TestEx", "http://example.com", info, 1, 10, OptionalLong.empty(), null, false);
    submissionResult
        = new SubmissionResult(1, 0, 0.0, SubmissionResult.Status.GRADED, exercise);
    setUp(new SubmissionResultViewModel(submissionResult, 1));
    OpenExerciseItemAction action = new OpenExerciseItemAction(
        mainViewModelProvider,
        urlRenderer,
        notifier
    );
    action.actionPerformed(actionEvent);

    ArgumentCaptor<String> argumentCaptor
        = ArgumentCaptor.forClass(String.class);
    verify(urlRenderer).show(argumentCaptor.capture());
    Assertions.assertEquals(submissionResult.getHtmlUrl(), argumentCaptor.getValue());
  }

  @Test
  void testOpenItemActionExercise() throws Exception {
    var info = new SubmissionInfo(Collections.emptyMap());
    exercise = new Exercise(223, "TestEx", "http://example.com", info, 1, 10, OptionalLong.empty(), null, false);
    setUp(new ExerciseViewModel(exercise));
    OpenExerciseItemAction action = new OpenExerciseItemAction(
        mainViewModelProvider,
        urlRenderer,
        notifier
    );
    action.actionPerformed(actionEvent);

    ArgumentCaptor<String> argumentCaptor
        = ArgumentCaptor.forClass(String.class);
    verify(urlRenderer).show(argumentCaptor.capture());
    Assertions.assertEquals(exercise.getHtmlUrl(), argumentCaptor.getValue());
  }

  @Test
  void testOpenItemActionWeek() throws Exception {
    var exerciseGroup = new ExerciseGroup(0, "", "https://url.com/", true, List.of(), List.of());
    setUp(new ExerciseGroupViewModel(exerciseGroup));
    OpenExerciseItemAction action = new OpenExerciseItemAction(
        mainViewModelProvider,
        urlRenderer,
        notifier
    );
    action.actionPerformed(actionEvent);

    ArgumentCaptor<String> argumentCaptor
        = ArgumentCaptor.forClass(String.class);
    verify(urlRenderer).show(argumentCaptor.capture());
    Assertions.assertEquals(exerciseGroup.getHtmlUrl(), argumentCaptor.getValue());
  }

  @Test
  void testErrorNotification() throws URISyntaxException {
    var info = new SubmissionInfo(Collections.emptyMap());
    exercise = new Exercise(223, "TestEx", "http://example.com", info, 1, 10, OptionalLong.empty(), null, false);
    submissionResult
        = new SubmissionResult(1, 0, 0.0, SubmissionResult.Status.GRADED, exercise);
    setUp(new SubmissionResultViewModel(submissionResult, 1));
    URISyntaxException exception = new URISyntaxException("input", "reason");
    doThrow(exception).when(urlRenderer).show(anyString());
    OpenExerciseItemAction action = new OpenExerciseItemAction(
        mainViewModelProvider,
        urlRenderer,
        notifier
    );
    action.actionPerformed(actionEvent);

    ArgumentCaptor<UrlRenderingErrorNotification> argumentCaptor
        = ArgumentCaptor.forClass(UrlRenderingErrorNotification.class);
    verify(notifier).notify(argumentCaptor.capture(), any(Project.class));
    Assertions.assertSame(exception, argumentCaptor.getValue().getException());
  }

}
