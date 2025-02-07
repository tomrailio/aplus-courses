package fi.aalto.cs.apluscourses.intellij.model;

import com.intellij.diagnostic.VMOptions;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.io.FileUtilRt;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.utils.APlusLogger;
import fi.aalto.cs.apluscourses.utils.CoursesClient;
import fi.aalto.cs.apluscourses.utils.DomUtil;
import fi.aalto.cs.apluscourses.utils.PluginResourceBundle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SettingsImporter {

  private static final Logger logger = APlusLogger.logger;

  // Some hard coded custom settings for workspace.xml. This information should eventually come from
  // the course configuration file in some way.
  private static final String WORKSPACE_XML_COMPONENT_NAME = "CompilerWorkspaceConfiguration";
  private static final String WORKSPACE_XML_OPTION_NAME = "AUTO_SHOW_ERRORS_IN_EDITOR";
  private static final String WORKSPACE_XML_OPTION_VALUE = "false";

  /**
   * Downloads the course IDE settings ZIP file to a temporary file. Also adds IDEA startup actions
   * that unzip the temporary file to the IDEA configuration path after which the temporary file is
   * deleted. Therefore, the new IDE settings only take effect once IDEA is restarted and the
   * temporary file must still exist at that point.
   *
   * @throws IOException If an IO error occurs (e.g. network issues).
   */
  public void importIdeSettings(@NotNull Course course) throws IOException {
    URL ideSettingsUrl = course.getAppropriateIdeSettingsUrl();
    if (ideSettingsUrl == null) {
      return;
    }

    File file = FileUtilRt.createTempFile("course-ide-settings", ".zip");
    CoursesClient.fetch(ideSettingsUrl, file);
    String configPath = FileUtilRt.toSystemIndependentName(PathManager.getConfigPath());
    StartupActionScriptManager.addActionCommands(
        List.of(
            new StartupActionScriptManager.UnzipCommand(file.toPath(), Path.of(configPath)),
            new StartupActionScriptManager.DeleteCommand(file.toPath())
        )
    );

    UpdateSettings.getInstance().forceCheckForUpdateAfterRestart();

    PluginSettings.getInstance().setImportedIdeSettingsId(course.getId());
  }

  /**
   * Imports the VM options from the course configuration file into the IDE. If there are no
   * options to import, this function does nothing.
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public void importVMOptions(@NotNull Course course) throws IOException {
    if (!VMOptions.canWriteOptions()) {
      logger.warn("Cannot import VM options because the IDE is configured not to use them");
      return;
    }

    var options = course.getVMOptions();
    for (var option : options.entrySet()) {
      VMOptions.setProperty(option.getKey(), option.getValue());
    }

    logger.info("Imported " + options.size() + " VM options");
  }

  /**
   * Returns the ID of the course for which the latest IDE settings import has been done.
   */
  @Nullable
  public String currentlyImportedIdeSettings() {
    return PluginSettings.getInstance().getImportedIdeSettingsId();
  }

  /**
   * Downloads the course project settings ZIP file to a temporary file. After that the files from
   * the .idea directory of the ZIP file are extracted to the .idea directory of the given project,
   * after which the project is reloaded. If the course does not provide custom project settings,
   * this method does nothing.
   *
   * @throws IOException If an IO error occurs (e.g. network issues).
   */
  public void importProjectSettings(@NotNull Path basePath, @NotNull Course course)
      throws IOException {
    URL settingsUrl = course.getResourceUrls().get("projectSettings");
    if (settingsUrl == null) {
      return;
    }

    Path settingsPath = basePath.resolve(Project.DIRECTORY_STORE_FOLDER);

    File settingsZip = FileUtilRt.createTempFile("course-project-settings", ".zip");
    CoursesClient.fetch(settingsUrl, settingsZip);
    ZipFile zipFile = new ZipFile(settingsZip);

    extractZipTo(zipFile, settingsPath);

    Path workspaceXmlPath = settingsPath.resolve("workspace.xml");
    Document workspaceXml = createCustomWorkspaceXml(workspaceXmlPath);
    DomUtil.writeDocumentToFile(workspaceXml, workspaceXmlPath.toFile());
    logger.info("Imported project settings");
  }

  /**
   * Downloads a custom properties file if it exists, and saves it to the project's
   * .idea directory.
   *
   * @throws IOException If an IO error occurs (e.g. network issues).
   */
  public void importCustomProperties(@NotNull Path basePath, @NotNull Course course,
                                     @NotNull Project project)
      throws IOException {
    URL settingsUrl = course.getResourceUrls().get("customProperties");
    if (settingsUrl == null) {
      return;
    }

    Path settingsPath = basePath.resolve(Project.DIRECTORY_STORE_FOLDER);

    File file = settingsPath.resolve(PluginResourceBundle.CUSTOM_RESOURCES_FILENAME).toFile();
    CoursesClient.fetch(settingsUrl, file);

    PluginResourceBundle.setCustomBundle(file, project);
    logger.info("Imported custom properties");
  }

  /**
   * Downloads the feedback CSS and saves it to the MainViewModel.
   *
   * @throws IOException If an IO error occurs (e.g. network issues).
   */
  public void importFeedbackCss(@NotNull Project project, @NotNull Course course) throws IOException {
    var cssUrl = course.getResourceUrls().get("feedbackCss");
    if (cssUrl == null) {
      return;
    }

    // TODO cache when string cache is available
    var stream = CoursesClient.fetch(cssUrl);

    var bytes = stream.readAllBytes();
    var s = new String(bytes, StandardCharsets.UTF_8);

    PluginSettings.getInstance().getMainViewModel(project).setFeedbackCss(s);
    logger.info("Imported feedback CSS");
  }

  private static void extractZipTo(@NotNull ZipFile zipFile, @NotNull Path target)
      throws IOException {
    List<String> fileNames = getZipFileNames(zipFile);
    for (String fileName : fileNames) {
      Path path = Paths.get(fileName);
      // The ZIP contains a .idea directory with all of the settings files. We want to extract the
      // files to the .idea directory without the .idea prefix, as otherwise we would end up with
      // .idea/.idea/<settings_files>.
      Path pathWithoutRoot = path.subpath(1, path.getNameCount());
      zipFile.extractFile(path.toString(), target.toString(), pathWithoutRoot.toString());
    }
  }


  /**
   * Returns the names of the files inside the given ZIP file. Directories are not included, but
   * files inside directories are included.
   *
   * @param zipFile The ZIP file from which the file names are read.
   * @return A list of names of the files inside the given ZIP file.
   * @throws IOException If an IO error occurs.
   */
  @NotNull
  private static List<String> getZipFileNames(@NotNull ZipFile zipFile) throws IOException {
    return zipFile
        .getFileHeaders()
        .stream()
        .filter(file -> !file.isDirectory())
        .map(FileHeader::getFileName)
        .collect(Collectors.toList());
  }

  /**
   * Parses the XML file (usually the workspace.xml file stored in .idea) at the given path and
   * returns a {@link Document} with file contents and an additional setting added.
   *
   * @param workspaceXmlPath The path pointing to the XML file.
   * @throws IOException           If an IO error occurs while parsing the XML file.
   * @throws IllegalStateException If the existing XML in the given file is malformed. In practice
   *                               this would mean that the {@code workspace.xml} file is malformed,
   *                               which shouldn't happen.
   */
  @NotNull
  public static Document createCustomWorkspaceXml(@NotNull Path workspaceXmlPath) throws
      IOException {
    try {
      Document document = DomUtil.parse(workspaceXmlPath.toFile());
      Node projectNode = document.getDocumentElement();

      // Check if a component with the given name already exists
      List<Node> componentMatches = DomUtil.getNodesFromXPath(
          "//component[@name=\"" + WORKSPACE_XML_COMPONENT_NAME + "\"]", projectNode);
      Node compilerConfigurationNode;
      if (componentMatches.isEmpty()) {
        Element compilerConfigurationElement = document.createElement("component");
        compilerConfigurationElement.setAttribute("name", WORKSPACE_XML_COMPONENT_NAME);
        compilerConfigurationNode = compilerConfigurationElement;
        projectNode.appendChild(compilerConfigurationNode);
      } else {
        // There should only be one match, otherwise the workspace.xml file is in a weird state
        compilerConfigurationNode = componentMatches.get(0);
      }

      Element option = document.createElement("option");
      option.setAttribute("name", WORKSPACE_XML_OPTION_NAME);
      option.setAttribute("value", WORKSPACE_XML_OPTION_VALUE);
      compilerConfigurationNode.appendChild(option);
      return document;
    } catch (SAXException ex) {
      // If workspace.xml is malformed, then something is seriously wrong...
      throw new IllegalStateException();
    }
  }

}
