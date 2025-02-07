package fi.aalto.cs.apluscourses.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

//  For this to work the 'INTEGRATION=true' environment variable is added to the GitHub workflow .yml file
@EnabledIfEnvironmentVariable(named = "INTEGRATION", matches = "true")
class ApiTest {

  private static final String BASE_URL = "http://localhost:8000/api/v2/";

  @Test
  void testGetPoints() {
    final String firstExercise = "modules[0].exercises[0]";
    final String url = BASE_URL + "courses/100/points/me/";

    given()
        .auth()
        .preemptive()
        .basic("zoralst1", "zoralst1")
        .when()
        .get(url)
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON)
        .body(firstExercise + ".id", equalTo(300))
        .body(firstExercise + ".max_points", equalTo(100))
        .body(firstExercise + ".points_to_pass", equalTo(50))
        .body(firstExercise + ".submission_count", equalTo(3))
        .body(firstExercise + ".points", equalTo(60))
        .body(firstExercise + ".passed", equalTo(true))
        .body(firstExercise + ".best_submission",
            equalTo("http://localhost:8000/api/v2/submissions/401/"))
        .body(firstExercise + ".submissions[0]", containsString("402"))
        .body(firstExercise + ".submissions[1]", containsString("401"))
        .body(firstExercise + ".submissions[2]", containsString("400"));
  }

  @Test
  void testGetStudentsGroups() {
    given()
        .auth()
        .preemptive()
        .basic("zoralst1", "zoralst1")
        .when()
        .get(BASE_URL + "courses/100/mygroups/")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON)
        .body("results[0].id", equalTo(200))
        .body("results[0].members.full_name", hasItems("Perry Cash", "Zorita Alston"));
  }

  @Test
  void testGetExercises() {
    given()
        .auth()
        .preemptive()
        .basic("zoralst1", "zoralst1")
        .when()
        .get("http://localhost:8000/api/v2/courses/100/exercises/")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON)
        .body("results.display_name", hasItems("1. First module", "1. Second module"))
        .body("results.exercises.id[0]", hasItems(300, 301))
        .body("results.exercises.id[1]", hasItems(302));
  }

  @Test
  void testGetIndividualExercise() {
    given()
        .auth()
        .preemptive()
        .basic("zoralst1", "zoralst1")
        .when()
        .get("http://localhost:8000/api/v2/exercises/301/")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON)
        .body("exercise_info", equalTo(null))
        .body("max_submissions", equalTo(5));
  }

  @Test
  void testGetSubmissions() {
    given()
        .auth()
        .preemptive()
        .basic("zoralst1", "zoralst1")
        .when()
        .get("http://localhost:8000/api/v2/exercises/301/submissions/me/")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON)
        .body("count", equalTo(0));
  }

  @Test
  void testGetIndividualSubmission() {
    given()
        .auth()
        .preemptive()
        .basic("zoralst1", "zoralst1")
        .when()
        .get("http://localhost:8000/api/v2/submissions/401/")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_OK)
        .contentType(ContentType.JSON)
        .body(
            "exercise.html_url",
            equalTo("http://localhost:8000/test-course/test-instance/first-module/easy-exercise/")
        )
        .body("late_penalty_applied", equalTo(null))
        .body("status", equalTo("ready"));
  }
}
