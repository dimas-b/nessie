/*
 * Copyright (C) 2020 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.server.error;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.net.URI;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.projectnessie.client.http.HttpClient;
import org.projectnessie.client.http.HttpClientException;
import org.projectnessie.client.rest.NessieHttpResponseFilter;
import org.projectnessie.client.rest.NessieInternalServerException;
import org.projectnessie.error.NessieBackendThrottledException;
import org.projectnessie.error.NessieBadRequestException;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.error.NessieUnsupportedMediaTypeException;
import org.projectnessie.quarkus.tests.profiles.QuarkusTestProfileInmemory;
import org.projectnessie.server.QuarkusNessieUriResolver;

/**
 * Test reported exceptions both for cases when {@code javax.validation} fails (when the Nessie
 * infra code isn't even run) and exceptions reported <em>by</em> Nessie.
 */
@QuarkusTest
@TestProfile(
    QuarkusTestProfileInmemory.class) // use the QuarkusTestProfileInmemory, as it can be reused
@ExtendWith(QuarkusNessieUriResolver.class)
class TestNessieError {

  private static HttpClient client;

  @BeforeAll
  static void setup(URI quarkusNessieUri) {
    ObjectMapper mapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    client =
        HttpClient.builder()
            .setBaseUri(quarkusNessieUri.resolve("nessieErrorTest"))
            .setObjectMapper(mapper)
            .addResponseFilter(new NessieHttpResponseFilter(mapper))
            .build();
  }

  @Test
  void nullParameterQueryGet() {
    assertThatThrownBy(() -> client.newRequest().path("nullParameterQueryGet").get())
        .isInstanceOf(NessieBadRequestException.class)
        .hasMessage("Bad Request (HTTP/400): nullParameterQueryGet.hash: must not be null");
  }

  @Test
  void nullParameterQueryPost() {
    assertThatThrownBy(() -> client.newRequest().path("nullParameterQueryPost").post(""))
        .isInstanceOf(NessieBadRequestException.class)
        .hasMessage("Bad Request (HTTP/400): nullParameterQueryPost.hash: must not be null");
  }

  @Test
  void emptyParameterQueryGet() {
    assertAll(
        () ->
            assertThatThrownBy(() -> client.newRequest().path("emptyParameterQueryGet").get())
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessage(
                    "Bad Request (HTTP/400): emptyParameterQueryGet.hash: must not be empty"),
        () ->
            assertThatThrownBy(
                    () ->
                        client
                            .newRequest()
                            .path("emptyParameterQueryGet")
                            .queryParam("hash", "")
                            .get())
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessage(
                    "Bad Request (HTTP/400): emptyParameterQueryGet.hash: must not be empty"));
  }

  @Test
  void blankParameterQueryGet() {
    assertAll(
        () ->
            assertThatThrownBy(() -> client.newRequest().path("blankParameterQueryGet").get())
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessage(
                    "Bad Request (HTTP/400): blankParameterQueryGet.hash: must not be blank"),
        () ->
            assertThatThrownBy(
                    () ->
                        client
                            .newRequest()
                            .path("blankParameterQueryGet")
                            .queryParam("hash", "")
                            .get())
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessage(
                    "Bad Request (HTTP/400): blankParameterQueryGet.hash: must not be blank"),
        () ->
            assertThatThrownBy(
                    () ->
                        client
                            .newRequest()
                            .path("blankParameterQueryGet")
                            .queryParam("hash", "   ")
                            .get())
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessage(
                    "Bad Request (HTTP/400): blankParameterQueryGet.hash: must not be blank"));
  }

  @Test
  void unsupportedMediaTypePut() {
    assertAll(
        () ->
            assertThatThrownBy(
                    () ->
                        unwrap(
                            () -> client.newRequest().path("unsupportedMediaTypePut").put("foo")))
                .isInstanceOf(NessieUnsupportedMediaTypeException.class)
                .hasMessage(
                    "Unsupported Media Type (HTTP/415): RESTEASY003065: Cannot consume content type"));
  }

  @Test
  void entityValueViolation() {
    assertAll(
        () ->
            assertThatThrownBy(
                    () -> client.newRequest().path("basicEntity").put("not really valid json"))
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessageStartingWith(
                    "Bad Request (HTTP/400): Unrecognized token 'not': was expecting (JSON String, Number, "
                        + "Array, Object or token 'null', 'true' or 'false')\n"),
        () ->
            assertThatThrownBy(() -> client.newRequest().path("basicEntity").put("{}"))
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessageStartingWith(
                    "Bad Request (HTTP/400): Missing required creator property 'value' (index 0)\n"),
        () ->
            assertThatThrownBy(
                    () -> client.newRequest().path("basicEntity").put("{\"value\":null}"))
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessageStartingWith(
                    "Bad Request (HTTP/400): basicEntity.entity.value: must not be null"),
        () ->
            assertThatThrownBy(
                    () -> client.newRequest().path("basicEntity").put("{\"value\":1.234}"))
                .isInstanceOf(NessieBadRequestException.class)
                .hasMessage(
                    "Bad Request (HTTP/400): basicEntity.entity.value: must be greater than or equal to 3"));
  }

  @Test
  void brokenEntitySerialization() {
    // send something that cannot be deserialized
    assertThatThrownBy(
            () -> unwrap(() -> client.newRequest().path("basicEntity").put(new OtherEntity("bar"))))
        .isInstanceOf(NessieBadRequestException.class)
        .hasMessageStartingWith(
            "Bad Request (HTTP/400): Missing required creator property 'value' (index 0)\n");
  }

  @Test
  void nessieNotFoundException() {
    NessieNotFoundException ex =
        assertThrows(
            NessieNotFoundException.class,
            () -> unwrap(() -> client.newRequest().path("nessieNotFound").get()));
    assertAll(
        () -> assertEquals("not-there-message", ex.getMessage()),
        () -> assertNull(ex.getServerStackTrace()),
        () -> assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getStatus()));
  }

  @Test
  void nonConstraintValidationExceptions() {
    // Exceptions that trigger the "else-ish" part in ResteasyExceptionMapper.toResponse()

    assertAll(
        () ->
            assertThatThrownBy(
                    () ->
                        unwrap(
                            () -> client.newRequest().path("constraintDefinitionException").get()))
                .isInstanceOf(NessieInternalServerException.class)
                .hasMessage(
                    "Internal Server Error (HTTP/500): javax.validation.ConstraintDefinitionException: meep"),
        () ->
            assertThatThrownBy(
                    () ->
                        unwrap(
                            () -> client.newRequest().path("constraintDeclarationException").get()))
                .isInstanceOf(NessieInternalServerException.class)
                .hasMessage(
                    "Internal Server Error (HTTP/500): javax.validation.ConstraintDeclarationException: meep"),
        () ->
            assertThatThrownBy(
                    () -> unwrap(() -> client.newRequest().path("groupDefinitionException").get()))
                .isInstanceOf(NessieInternalServerException.class)
                .hasMessage(
                    "Internal Server Error (HTTP/500): javax.validation.GroupDefinitionException: meep"));
  }

  @Test
  void unhandledRuntimeExceptionInStore() {
    // see org.projectnessie.server.error.ErrorTestService.unhandledExceptionInTvsStore
    assertThatThrownBy(() -> client.newRequest().path("unhandledExceptionInTvsStore/runtime").get())
        .isInstanceOf(NessieInternalServerException.class)
        .hasMessage(
            "Internal Server Error (HTTP/500): java.lang.RuntimeException: Store.getValues-throwing");
  }

  @Test
  void backendThrottledExceptionInStore() {
    // see org.projectnessie.server.error.ErrorTestService.unhandledExceptionInTvsStore
    assertThatThrownBy(
            () -> client.newRequest().path("unhandledExceptionInTvsStore/throttle").get())
        .isInstanceOf(NessieBackendThrottledException.class)
        .hasMessage(
            "Too Many Requests (HTTP/429): Backend store refused to process the request: "
                + "org.projectnessie.versioned.BackendLimitExceededException: Store.getValues-throttled");
  }

  void unwrap(Executable exec) throws Throwable {
    try {
      exec.execute();
    } catch (Throwable targetException) {
      if (targetException instanceof HttpClientException) {
        if (targetException.getCause() instanceof NessieNotFoundException
            || targetException.getCause() instanceof NessieConflictException) {
          throw targetException.getCause();
        }
      }

      throw targetException;
    }
  }
}
