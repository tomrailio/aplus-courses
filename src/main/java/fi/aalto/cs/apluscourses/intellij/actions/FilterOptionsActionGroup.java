package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.base.BaseTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import fi.aalto.cs.apluscourses.utils.Streamable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilterOptionsActionGroup extends DefaultActionGroup implements DumbAware, Toggleable {

  private final MainViewModelProvider mainViewModelProvider;

  private FilterOptionAction @NotNull [] filterOptionActions = new FilterOptionAction[0];

  public FilterOptionsActionGroup() {
    this(PluginSettings.getInstance());
  }

  public FilterOptionsActionGroup(MainViewModelProvider mainViewModelProvider) {
    this.mainViewModelProvider = mainViewModelProvider;
  }

  @Override
  public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
    this.filterOptionActions = getFilterOptionActions(e);
    List<AnAction> actions = new ArrayList<>(Arrays.asList(this.filterOptionActions));
    if (e != null) {
      actions.add(new Separator());
      Options filterOptions =
          mainViewModelProvider.getMainViewModel(e.getProject()).getExerciseFilterOptions();
      actions.add(new SelectAllFiltersAction(filterOptions));
    }
    return actions.toArray(new AnAction[0]);
  }

  /**
   * Returns filter option actions.
   *
   * @param e AnActionEvent
   * @return An array of FilterOptionActions
   */
  private FilterOptionAction @NotNull [] getFilterOptionActions(@Nullable AnActionEvent e) {
    return Optional.ofNullable(e)
        .map(AnActionEvent::getProject)
        .map(mainViewModelProvider::getMainViewModel)
        .map(MainViewModel::getExercises)
        .map(BaseTreeViewModel::getFilterOptions)
        .map(Streamable::stream)
        .orElseGet(Stream::empty)
        .map(FilterOptionAction::new)
        .toArray(FilterOptionAction[]::new);
  }

  public FilterOptionAction @NotNull [] getFilterOptionActions() {
    return filterOptionActions;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    var exercisesTreeViewModel =
        mainViewModelProvider.getMainViewModel(project).exercisesViewModel.get();
    e.getPresentation().setEnabled(exercisesTreeViewModel != null);
    boolean filterActive =
        Arrays.stream(this.filterOptionActions).anyMatch(action -> !action.isSelected(e));
    Toggleable.setSelected(e.getPresentation(), filterActive);
  }
}
