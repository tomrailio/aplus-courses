package fi.aalto.cs.apluscourses.intellij.actions

import com.intellij.execution.configurations.{ConfigurationFactory, JavaCommandLineState, RunProfileState}
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.{Executor, RunManagerEx}
import com.intellij.openapi.actionSystem.{AnActionEvent, CommonDataKeys, DataContext}
import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtilRt.toSystemIndependentName
import fi.aalto.cs.apluscourses.intellij.Repl
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings.MODULE_REPL_INITIAL_COMMANDS_FILE_NAME
import fi.aalto.cs.apluscourses.intellij.utils.{ModuleUtils, ReplChangesObserver}
import fi.aalto.cs.apluscourses.presentation.ReplConfigurationFormModel
import fi.aalto.cs.apluscourses.ui.repl.{ReplConfigurationDialog, ReplConfigurationForm}
import fi.aalto.cs.apluscourses.utils.PluginResourceBundle.{getAndReplaceText, getText}
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.scala.actions.ScalaActionUtil
import org.jetbrains.plugins.scala.console.actions.RunConsoleAction
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfiguration
import org.jetbrains.plugins.scala.project.ProjectExt

import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Custom class that adjusts Scala Plugin's own RunConsoleAction with A+ requirements.
 */
class ReplAction extends RunConsoleAction {

  override def update(e: AnActionEvent): Unit = {
    if (e.getProject == null || e.getProject.isDisposed) return // scalastyle:ignore

    if (e.getProject.hasScala) {
      ScalaActionUtil.enablePresentation(e)
    } else {
      ScalaActionUtil.disablePresentation(e)
    }
  }

  override def actionPerformed(@NotNull e: AnActionEvent): Unit = {
    val dataContext = e.getDataContext
    val project = CommonDataKeys.PROJECT.getData(dataContext)

    if (project == null) return // scalastyle:ignore

    val runManagerEx = RunManagerEx.getInstanceEx(project)

    /*
     * The "priority order" is as follows:
     *   1. If a file is open in the editor and it belongs to a module that has Scala SDK as a
     *      library dependency, start the REPL for the module of the file.
     *   2. Otherwise if a module (or file inside a module) is selected in the project menu on the
     *      left and the module has Scala SDK as a library dependency, start the REPL for that
     *      module.
     *   3. Otherwise start a project level REPL
     *
     * Checking that the module has Scala SDK as a dependency is done to avoid the "no Scala facet
     * configured for module" error.
     */
    val selectedModule = getScalaModuleOfEditorFile(project, dataContext)
      .orElse(getScalaModuleOfSelectedFile(project, dataContext)).orElse(getDefaultModule(project))

    val setting = runManagerEx.createConfiguration(
      getText("ui.repl.console.scala.repl"), new ReplConfigurationFactory())
    val configuration = setting.getConfiguration.asInstanceOf[ScalaConsoleRunConfiguration]

    selectedModule match {
      case Some(module) =>
        if (!setConfigurationConditionally(project, module, configuration)) {
          return // scalastyle:ignore
        }
      case None =>
        super.actionPerformed(e) // Delegate to the original Scala Plugin REPL
        return // scalastyle:ignore
    }
    RunConsoleAction.runExisting(setting, runManagerEx, project)
  }

  private class ReplConfigurationFactory() extends ConfigurationFactory(getMyConfigurationType) {
    override def createTemplateConfiguration(project: Project): ScalaConsoleRunConfiguration = {
      new ReplConfiguration(project, this,
        getText("ui.repl.console.scala.repl"))
    }

    override def getId: String = getText("ui.repl.console.scala.repl.extended")

    private class ReplConfiguration(project: Project,
                                    configurationFactory: ConfigurationFactory,
                                    name: String)
      extends ScalaConsoleRunConfiguration(project, configurationFactory, name) {

      private def getModule: Option[Module] = Option(getConfigurationModule.getModule)

      override def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState = {
        val state = super.getState(executor, env).asInstanceOf[JavaCommandLineState]

        getModule match {
          case Some(module) =>
            state.setConsoleBuilder(new MyBuilder(module))
          case None =>
        }

        state
      }

      private class MyBuilder(module: Module) extends TextConsoleBuilderImpl(module.getProject) {
        override def createConsole(): ConsoleView = new Repl(module)
      }

    }

  }

  def getDefaultModule(@NotNull project: Project): Option[Module] = {
    Option(PluginSettings.getInstance().getMainViewModel(project).courseViewModel.get) match {
      case Some(courseViewModel) =>
        Option(ModuleManager.getInstance(project).findModuleByName(
          courseViewModel
            .getModel
            .getAutoInstallComponentNames
            .asScala
            //  we, hereby, commonly agree, that the first in the list auto install component (module)
            //  is ultimately REPL's default module (as it's most likely to exist). sorry :pensive:
            .head))
      case None => None
    }
  }

  def getReplAdditionalArguments(@NotNull project: Project): String = {
    Option(PluginSettings.getInstance().getMainViewModel(project).courseViewModel.get) match {
      case Some(courseViewModel) => courseViewModel.getModel.getReplAdditionalArguments
      case None => ""
    }
  }

  def setConfigurationFields(@NotNull configuration: ScalaConsoleRunConfiguration,
                             @NotNull workingDirectory: String,
                             @NotNull module: Module): Unit = {
    configuration.setWorkingDirectory(workingDirectory)
    configuration.setModule(module)
    configuration.setName(getAndReplaceText("ui.repl.console.name", module.getName))

    var args = "-usejavacp " + getReplAdditionalArguments(module.getProject)

    ModuleUtils.createInitialReplCommandsFile(module)
    if (ModuleUtils.initialReplCommandsFileExists(module)) {
      args += " -i " + MODULE_REPL_INITIAL_COMMANDS_FILE_NAME
    }

    configuration.setMyConsoleArgs(args)
  }

  /**
   * Sets configuration fields for the given configuration and returns true. Returns false if
   * the REPL start is cancelled (i.e. user selects "Cancel" in the REPL configuration dialog).
   */
  def setConfigurationConditionally(@NotNull project: Project,
                                    @NotNull module: Module,
                                    @NotNull configuration: ScalaConsoleRunConfiguration): Boolean = {


    if (PluginSettings.getInstance.shouldShowReplConfigurationDialog) {
      setConfigurationFieldsFromDialog(configuration, project, module)
    } else {
      setConfigurationFields(configuration, ModuleUtils.getModuleDirectory(module), module)
      true
    }
  }

  /**
   * Sets the configuration fields from the REPL dialog. Returns true if it is done successfully,
   * and false if the user cancels the REPL dialog.
   */
  private def setConfigurationFieldsFromDialog(@NotNull configuration: ScalaConsoleRunConfiguration,
                                               @NotNull project: Project,
                                               @NotNull module: Module): Boolean = {
    val configModel = showReplDialog(project, module)
    if (!configModel.isStartRepl) {
      false
    } else {
      val changedModuleName = configModel.getTargetModuleName
      val changedModule = ModuleManager.getInstance(project).findModuleByName(changedModuleName)
      val changedWorkDir = toSystemIndependentName(configModel.getModuleWorkingDirectory)
      setConfigurationFields(configuration, changedWorkDir, changedModule)
      true
    }
  }

  private def showReplDialog(@NotNull project: Project,
                             @NotNull module: Module): ReplConfigurationFormModel = {
    val configModel = new ReplConfigurationFormModel(project, ModuleUtils.getModuleDirectory(module), module.getName)
    val configForm = new ReplConfigurationForm(configModel, project)
    val configDialog = new ReplConfigurationDialog
    configDialog.setReplConfigurationForm(configForm)
    configDialog.setVisible(true)
    configModel
  }

  private def getScalaModuleOfEditorFile(@NotNull project: Project,
                                         @NotNull context: DataContext): Option[Module] =
    ModuleUtils.getModuleOfEditorFile(project, context).filter(hasScalaSdkLibrary)

  private def getScalaModuleOfSelectedFile(@NotNull project: Project,
                                           @NotNull context: DataContext): Option[Module] =
    ModuleUtils.getModuleOfSelectedFile(project, context).filter(hasScalaSdkLibrary)

  private def hasScalaSdkLibrary(@NotNull module: Module): Boolean = ModuleUtils.nonEmpty(
    ModuleRootManager.getInstance(module)
      .orderEntries()
      .librariesOnly()
      .satisfying(_.getPresentableName.startsWith("scala-sdk-")))

}
