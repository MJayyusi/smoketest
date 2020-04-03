package com.hungerstation.app;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import org.junit.Test;
import java.util.logging.Logger;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * Unit test for simple App.
 */
public class PetTest {
    private final static Logger LOGGER = Logger.getLogger(PetTest.class.getName());

    public PetTest() {
        RestAssured.baseURI = "http://petstore.swagger.io/v2";
        RestAssured.basePath = "/pet";
        // RestAssured.proxy("127.0.0.1", 8888); //enable this line only for debugging
        // with Fiddler

    }

    public Response createPet() {

        try {
            String newPetJsonPayload = new String(
                    Files.readAllBytes(Paths.get("src/test/java/com/hungerstation/res/newPet.json")));

            return given().accept("application/json").
            contentType("application/json").body(newPetJsonPayload).when()
                    .post();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void checkFindByStatusFilterHelper(String expectedStatus) {
        Response res = get("/findByStatus?status=" + expectedStatus).then().
        assertThat().contentType(ContentType.JSON)
                .and().statusCode(200).extract().response();
        List<String> statuses = res.jsonPath().getList("status");

        if (statuses.size() == 0) {
            LOGGER.warning(String.format("Found 0 items for status %s", expectedStatus));
        } else {
            LOGGER.info(String.format("Checking %s results for expected status %s", 
                        statuses.size(), expectedStatus));
        }

        for (String status : statuses) {
            assertEquals(expectedStatus, status);
        }
    }

    @Test
    public void checkFindByStatusFilterIsWorkingTest() {
        /**
         * Make sure all returend values contain correct status values
         */
        String[] expectedStauses = { "available", "pending", "sold" };
        for (String expectedStatus : expectedStauses) {
            checkFindByStatusFilterHelper(expectedStatus);
        }
    }

    @Test
    public void checkFindByStatusFilterInvalidStatusCodeTest() {
        /**
         * According to endpoint documentation This call shall return status code 400
         * This test is failing due to error in the endpoint
         */
        get("/findByStatus?status=InvalidStatus").then().assertThat().contentType(ContentType.JSON).and()
                .statusCode(400);

    }

    @Test
    public void findPetByIdTest() {
        /**
         * This test creates a new Pet and then uses its id to test against resulted pet
         * using find pet by id endpoint
         */
        Response newPet = createPet();
        JsonPath newPetJson = newPet.jsonPath();
        Long newPetId = newPetJson.get("id");

        get("/{Id}", newPetId).then().assertThat().statusCode(200).and().
        contentType(ContentType.JSON).body("",
                equalTo(newPetJson.getMap("")));

    }

    @Test
    public void petNotFoundPetByIdTest() {
        /**
         * Testing against a static pet id (0), Pet id 0 does not exist hence it should
         * return 'Pet not found' and code 404
         */

        get("/{Id}", 0).then().assertThat().statusCode(404).and().
        contentType(ContentType.JSON).body("message",
                equalTo("Pet not found"));
    }

    @Test
    public void invalidIdSuppliedPetByIdTest() {
        /**
         * Testing against a an invalid pet id, ids usually are int we will try with 1.1
         * to break it Expected message according to spec 'Invalid ID supplied' and code
         * 400 This test is failing due to error in the endpoint
         */

        get("/{Id}", "1.1").then().assertThat().statusCode(400).and().
        contentType(ContentType.JSON).body("message",
                equalTo("Invalid ID supplied"));
    }

    @Test
    public void createNewPetTest() {
        /**
         * This tests creates a new pet
         */

        Response newPet = createPet();
        JsonPath newPetJson = newPet.jsonPath();
        Long id = newPetJson.get("id");
        assertTrue("Id shall be greater than zero", id > 0);
        assertEquals("doggie", newPetJson.get("name"));
        assertEquals("available", newPetJson.get("status"));
    }

    @Test
    public void deletePetTest() {
       /**
         * This test creates a pet then deletes it and checks it doesnt exist
         * 
         */

        Response newPet = createPet();
        JsonPath newPetJson = newPet.jsonPath();
        Long id = newPetJson.get("id");
        LOGGER.info(String.format("Created pet id %s", id));
        given().accept("application/json").contentType("application/json").
        delete("/{id}", id).then().assertThat()
                .statusCode(200);
        LOGGER.info(String.format("Deleted pet id %s", id));
        get("/{Id}", id).then().assertThat().statusCode(404).and().
        contentType(ContentType.JSON).body("message",
                equalTo("Pet not found"));
    }

    @Test
    public void deletePetNotFoundTest() {
        /**
         * This test creates a pet then deletes it, and try to delete it again 
         * then double checkes the expected status code as 404 per documentation
         */

        Response newPet = createPet();
        JsonPath newPetJson = newPet.jsonPath();
        Long id = newPetJson.get("id");
        LOGGER.info(String.format("Created pet id %s", id));
        given().accept("application/json").contentType("application/json").
        delete("/{id}", id).then().assertThat()
                .statusCode(200);
        LOGGER.info(String.format("Deleted pet id %s", id));
        get("/{Id}", id).then().assertThat().statusCode(404).and().
        contentType(ContentType.JSON).body("message",
                equalTo("Pet not found"));
        LOGGER.info(String.format("Trying to delete a non existing pet"));
        given().accept("application/json").contentType("application/json").
        delete("/{id}", id).then().assertThat()
                .statusCode(404);
    }

}
