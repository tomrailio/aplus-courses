package fi.aalto.cs.apluscourses.intellij.actions

import com.intellij.openapi.actionSystem.{AnActionEvent, DataContext}
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import fi.aalto.cs.apluscourses.intellij.TestHelperScala
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfiguration
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito._

class ReplActionTest extends TestHelperScala {

  @Test
  def testDoesNothingIfProjectNull(): Unit = {
    val anActionEvent = mock(classOf[AnActionEvent])
    when(anActionEvent.getDataContext).thenReturn(new DataContext {
      override def getData(dataId: String) = null
    })
    // This would throw an exception if the method wouldn't return early when the project is null
    (new ReplAction).actionPerformed(anActionEvent)
  }

  @Test
  def testSetConfigurationConditionallyWithDoNotShowReplFlagWorks(): Unit = {
    //  given
    val project = mock(classOf[Project])
    val module = mock(classOf[Module])
    val configuration = mock(classOf[ScalaConsoleRunConfiguration])
    val actionSpy = spy(new ReplAction)
    doReturn("path").when(actionSpy).getModuleDirectory(module)
    doNothing().when(actionSpy).initializeReplCommands(configuration, module)

    //  when
    val result = actionSpy.setConfigurationConditionally(project, module, configuration, false)

    //  then
    verify(actionSpy).setConfigurationFields(configuration, "path", module)
    assertTrue("Returns `true` as the REPL is gonna be started.", result)
  }

  @Test
  def testSetConfigurationConditionallyWithShowReplFlagWorks(): Unit = {
    //  given
    val project = mock(classOf[Project])
    val module = mock(classOf[Module])
    val configuration = mock(classOf[ScalaConsoleRunConfiguration])
    val actionSpy = spy(new ReplAction)
    doReturn("path").when(actionSpy).getModuleDirectory(module)
    doReturn(false).when(actionSpy).setConfigurationFieldsFromDialog(configuration, project, module)

    //  when
    val result = actionSpy.setConfigurationConditionally(project, module, configuration, true)

    //  then
    verify(actionSpy).setConfigurationFieldsFromDialog(configuration, project, module)
    assertFalse("Returns `false` as the REPL start has been cancelled.", result)
  }

  @Test
  def testSetConfigurationFieldsWorks(): Unit = {
    //  given
    val moduleWorkDir = ""
    val correctName= "REPL for name"
    val module = mock(classOf[Module])
    doReturn("name").when(module).getName
    val configuration = mock(classOf[ScalaConsoleRunConfiguration])
    val actionSpy = spy(new ReplAction)
    doNothing().when(actionSpy).initializeReplCommands(configuration, module)

    //  when
    actionSpy.setConfigurationFields(configuration, moduleWorkDir, module)

    //  then
    verify(configuration).setWorkingDirectory(moduleWorkDir)
    verify(configuration).setModule(module)
    verify(configuration).setName(correctName)
    verify(actionSpy).initializeReplCommands(configuration, module)
  }
}
