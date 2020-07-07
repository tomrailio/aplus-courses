package fi.aalto.cs.apluscourses.ui.exercise;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import fi.aalto.cs.apluscourses.intellij.actions.ActionUtil;
import fi.aalto.cs.apluscourses.intellij.actions.GetSubmissionsDashboardAction;
import fi.aalto.cs.apluscourses.intellij.utils.ExtendedDataContext;
import fi.aalto.cs.apluscourses.presentation.ModuleSelectionViewModel;
import fi.aalto.cs.apluscourses.ui.IconListCellRenderer;
import fi.aalto.cs.apluscourses.ui.OurComboBox;
import icons.PluginIcons;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModuleSelectionDialog extends DialogWrapper {
  private ModuleSelectionViewModel viewModel;
  private JPanel basePanel;
  private OurComboBox<Module> modulesComboBox;
  private final Project project;

  /**
   * Construct a module selection dialog with the given view model.
   */
  public ModuleSelectionDialog(@Nullable Project project,
                               @NotNull ModuleSelectionViewModel viewModel) {
    super(project);
    this.project = project;
    this.viewModel = viewModel;
    setButtonsAlignment(SwingConstants.CENTER);
    setTitle("Select Module");
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return basePanel;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[] { getOKAction(), getCancelAction() };
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (modulesComboBox.getSelectedItem() == null) {
      return new ValidationInfo("Select a module", modulesComboBox);
    }
    return null;
  }

  @Override
  public boolean showAndGet() {
    ActionUtil.launch(GetSubmissionsDashboardAction.ACTION_ID,
        new ExtendedDataContext().withProject(project));
    return super.showAndGet();
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private void createUIComponents() {
    modulesComboBox = new OurComboBox<>(viewModel.getModules(), Module.class);
    modulesComboBox.setRenderer(new IconListCellRenderer<>(viewModel.getPrompt(),
        Module::getName, PluginIcons.A_PLUS_MODULE));
    modulesComboBox.selectedItemBindable.bindToSource(viewModel.selectedModule);
  }
}
