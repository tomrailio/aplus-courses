package fi.aalto.cs.apluscourses.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SubmissionInfoTest {

  @Test
  void testSubmissionInfo() {

    SubmittableFile file1 = new SubmittableFile("file1", "f1.py");
    SubmittableFile file2 = new SubmittableFile("file2", "f2.py");
    Map<String, List<SubmittableFile>> files = new HashMap<>();
    String langFi = "fi";
    String langSe = "se";
    files.put(langFi, List.of(file1, file2));
    files.put(langSe, List.of(file1, file2));

    SubmissionInfo info = new SubmissionInfo(files);
    String message = "The filenames are the same as those given to the constructor";
    Assertions.assertEquals(file1.getName(), info.getFiles(langFi).get(0).getName(), message);
    Assertions.assertEquals(file2.getName(), info.getFiles(langFi).get(1).getName(), message);
    Assertions.assertEquals(file1.getName(), info.getFiles(langSe).get(0).getName(), message);
    Assertions.assertEquals(file2.getName(), info.getFiles(langSe).get(1).getName(), message);
  }

  @Test
  void testFromJsonObject() {
    JSONArray formSpec = new JSONArray()
        .put(new JSONObject()
            .put("key", "file1")
            .put("title", "i18n_coolFilename.scala")
            .put("type", "file"))
        .put(new JSONObject()
            .put("key", "file2")
            .put("title", "ignored because")
            .put("type", "is not file"));

    JSONObject formLocalization = new JSONObject()
        .put("i18n_coolFilename.scala", new JSONObject()
            .put("en", "coolEnglishFilename.scala")
            .put("fi", "coolFinnishFilename.scala"));

    JSONObject exerciseInfo = new JSONObject()
        .put("form_spec", formSpec)
        .put("form_i18n", formLocalization);

    JSONObject json = new JSONObject()
        .put("id", 321)
        .put("name", "test exercise")
        .put("exercise_info", exerciseInfo);

    SubmissionInfo info = SubmissionInfo.fromJsonObject(json);

    Assertions.assertEquals("coolEnglishFilename.scala", info.getFiles("en").get(0).getName(),
        "The filenames are parsed from the JSON");
    Assertions.assertEquals("coolFinnishFilename.scala", info.getFiles("fi").get(0).getName(),
        "The filenames are parsed from the JSON");
  }

}
