package fi.aalto.cs.apluscourses.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import fi.aalto.cs.apluscourses.utils.BuildInfo;
import fi.aalto.cs.apluscourses.utils.observable.ValidationError;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CourseProjectViewModelTest {

  private final Course emptyCourse = new ModelExtensions.TestCourse(
      "123",
      "NiceCourse",
      "http://localhost:9999",
      Collections.singletonList("de"),
      // modules
      Collections.emptyList(),
      // libraries
      Collections.emptyList(),
      // exerciseModules
      Collections.emptyMap(),
      // resourceUrls
      Collections.emptyMap(),
      // vmOptions
      Collections.emptyMap(),
      // optionalCategories
      Collections.emptySet(),
      // autoInstallComponentNames
      Collections.emptyList(),
      // replInitialCommands
      Collections.emptyMap(),
      // replAdditionalArguments
      "",
      // courseVersion
      BuildInfo.INSTANCE.courseVersion,
      // tutorials
      Collections.emptyMap()
  );

  @Test
  void testInformationTextIncludesCourseName() {
    CourseProjectViewModel courseProjectViewModel = new CourseProjectViewModel(emptyCourse, "");
    Assertions.assertEquals("NiceCourse", courseProjectViewModel.getCourseName(),
        "The information text contains the course name");
  }

  @Test
  void testIdeSettingsNotPreviouslyImported() {
    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(emptyCourse, "different");

    Assertions.assertFalse(courseProjectViewModel.userOptsOutOfSettings(),
        "By default the user should not want to opt out");

    Assertions.assertTrue(courseProjectViewModel.canUserOptOutSettings(), "Settings opt out should be available");

    Assertions.assertTrue(courseProjectViewModel.shouldShowSettingsInfo(),
        "The settings text should mention that IDEA settings will be adjusted");

    Assertions.assertFalse(courseProjectViewModel.shouldShowSettingsSegment(),
        "The settings panel in the dialog box should not be shown when there's no URL");
  }

  @Test
  void testIdeSettingsAlreadyImported() {
    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(emptyCourse, "123");

    Assertions.assertTrue(courseProjectViewModel.userOptsOutOfSettings());

    Assertions.assertFalse(courseProjectViewModel.canUserOptOutSettings(), "Settings opt out should not be available");

    Assertions.assertTrue(courseProjectViewModel.shouldShowCurrentSettings(),
        "The settings text should mention that settings are already imported");
  }

  @Test
  void testGetLanguages() {
    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(emptyCourse, "987");
    Assertions.assertArrayEquals(new String[] {"de"}, courseProjectViewModel.getLanguages());
  }

  @Test
  void testLanguageSelectionValidation() {
    CourseProjectViewModel courseProjectViewModel
        = new CourseProjectViewModel(emptyCourse, "111");
    ValidationError error = courseProjectViewModel.languageProperty.validate();
    MatcherAssert.assertThat(error.getDescription(), containsString("Select a language"));
    courseProjectViewModel.languageProperty.set("fi");
    Assertions.assertNull(courseProjectViewModel.languageProperty.validate());
  }
}
