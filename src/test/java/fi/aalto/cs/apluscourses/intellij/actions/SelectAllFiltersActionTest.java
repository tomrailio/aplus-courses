package fi.aalto.cs.apluscourses.intellij.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.presentation.filter.Option;
import fi.aalto.cs.apluscourses.presentation.filter.Options;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SelectAllFiltersActionTest {

  @Test
  void testSelectAllFilters() {
    Project project = mock(Project.class);
    AnActionEvent e = mock(AnActionEvent.class);
    when(e.getProject()).thenReturn(project);

    Option filterOption1 = new Option("Just some filter", null, item -> Optional.of(true)).init();
    Option filterOption2 = new Option("Another filter", null, item -> Optional.of(false)).init();
    Options filterOptions = new Options(filterOption1, filterOption2);

    SelectAllFiltersAction selectAllFiltersAction = new SelectAllFiltersAction(filterOptions);

    Assertions.assertEquals(true, filterOption1.isSelected.get());
    Assertions.assertEquals(true, filterOption2.isSelected.get());
    Assertions.assertEquals("Deselect all", selectAllFiltersAction.getTemplateText());

    filterOption1.isSelected.set(false);
    Assertions.assertEquals("Select all", selectAllFiltersAction.getTemplateText());

    selectAllFiltersAction.actionPerformed(e);
    Assertions.assertEquals(true, filterOption1.isSelected.get());
    Assertions.assertEquals(true, filterOption2.isSelected.get());
    Assertions.assertEquals("Deselect all", selectAllFiltersAction.getTemplateText());

    selectAllFiltersAction.actionPerformed(e);
    Assertions.assertEquals(false, filterOption1.isSelected.get());
    Assertions.assertEquals(false, filterOption2.isSelected.get());
    Assertions.assertEquals("Select all", selectAllFiltersAction.getTemplateText());
  }

}
