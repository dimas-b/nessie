/*
 * Copyright (C) 2022 Dremio
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
package org.projectnessie.jaxrs.tests;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.projectnessie.model.Validation.REF_NAME_MESSAGE;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.util.UUID;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.projectnessie.client.ext.NessieApiVersion;
import org.projectnessie.client.ext.NessieApiVersions;
import org.projectnessie.error.NessieError;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.Content;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.DiffResponse;
import org.projectnessie.model.DiffResponse.DiffEntry;
import org.projectnessie.model.IcebergTable;
import org.projectnessie.model.ImmutableBranch;
import org.projectnessie.model.ImmutableOperations;
import org.projectnessie.model.ImmutablePut;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.Namespace;
import org.projectnessie.model.Operation.Put;
import org.projectnessie.model.Operations;
import org.projectnessie.model.RefLogResponse;
import org.projectnessie.model.Reference;
import org.projectnessie.model.ReferencesResponse;
import org.projectnessie.model.Tag;

// Resteasy requests in these tests are crafted for v1 REST paths.
@NessieApiVersions(versions = {NessieApiVersion.V1})
public abstract class AbstractResteasyV1Test {

  protected static String basePath = "/api/v1/";

  @BeforeEach
  public void enableLogging() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Test
  public void testBasic() {
    int preSize =
        rest()
            .get("trees")
            .then()
            .statusCode(200)
            .extract()
            .as(ReferencesResponse.class)
            .getReferences()
            .size();

    rest().get("trees/tree/mainx").then().statusCode(404);
    rest().body(Branch.of("mainx", null)).post("trees/tree").then().statusCode(200);

    ReferencesResponse references =
        rest().get("trees").then().statusCode(200).extract().as(ReferencesResponse.class);
    Assertions.assertEquals(preSize + 1, references.getReferences().size());

    Reference reference =
        rest().get("trees/tree/mainx").then().statusCode(200).extract().as(Reference.class);
    assertEquals("mainx", reference.getName());

    Branch newReference = ImmutableBranch.builder().hash(reference.getHash()).name("test").build();
    rest()
        .queryParam("expectedHash", reference.getHash())
        .body(Branch.of("test", null))
        .post("trees/tree")
        .then()
        .statusCode(200);
    assertEquals(
        newReference,
        rest().get("trees/tree/test").then().statusCode(200).extract().as(Branch.class));

    IcebergTable initialTable = IcebergTable.of("/the/directory/over/there", 42, 43, 44, 45);

    Branch commitResponse =
        rest()
            .body(
                ImmutableOperations.builder()
                    .addOperations(
                        ImmutablePut.builder()
                            .key(ContentKey.of("xxx", "test"))
                            .content(initialTable)
                            .build())
                    .commitMeta(CommitMeta.fromMessage(""))
                    .build())
            .queryParam("expectedHash", newReference.getHash())
            .post("trees/branch/{branch}/commit", newReference.getName())
            .then()
            .statusCode(200)
            .extract()
            .as(Branch.class);
    Assertions.assertNotEquals(newReference.getHash(), commitResponse.getHash());

    // fetch the content
    IcebergTable table =
        rest()
            .queryParam("ref", "test")
            .get("contents/xxx.test")
            .then()
            .statusCode(200)
            .extract()
            .as(IcebergTable.class);
    assertThat(table)
        .extracting(
            IcebergTable::getMetadataLocation,
            IcebergTable::getSnapshotId,
            IcebergTable::getSchemaId,
            IcebergTable::getSpecId,
            IcebergTable::getSortOrderId)
        .containsExactly(
            initialTable.getMetadataLocation(),
            initialTable.getSnapshotId(),
            initialTable.getSchemaId(),
            initialTable.getSpecId(),
            initialTable.getSortOrderId());

    Put[] updates = new Put[11];
    for (int i = 0; i < 10; i++) {
      updates[i] =
          ImmutablePut.builder()
              .key(ContentKey.of("item", Integer.toString(i)))
              .content(IcebergTable.of("/the/directory/over/there/" + i, 42, 42, 42, 42))
              .build();
    }
    updates[10] =
        ImmutablePut.builder()
            .key(ContentKey.of("xxx", "test"))
            .content(IcebergTable.of("/the/directory/over/there/has/been/moved", 42, 42, 42, 42))
            .build();

    Reference branch = rest().get("trees/tree/test").as(Reference.class);
    Operations contents =
        ImmutableOperations.builder()
            .addOperations(updates)
            .commitMeta(CommitMeta.fromMessage(""))
            .build();

    commitResponse =
        rest()
            .body(contents)
            .queryParam("expectedHash", branch.getHash())
            .post("trees/branch/{branch}/commit", branch.getName())
            .then()
            .statusCode(200)
            .extract()
            .as(Branch.class);
    Assertions.assertNotEquals(branch.getHash(), commitResponse.getHash());

    Response res =
        rest().queryParam("ref", "test").get("contents/xxx.test").then().extract().response();
    Assertions.assertEquals(updates[10].getContent(), withoutId(res.body().as(Content.class)));

    IcebergTable currentTable = table;
    table =
        IcebergTable.of(
            "/the/directory/over/there/has/been/moved/again", 42, 42, 42, 42, table.getId());

    Branch b2 = rest().get("trees/tree/test").as(Branch.class);
    rest()
        .body(
            ImmutableOperations.builder()
                .addOperations(
                    ImmutablePut.builder()
                        .key(ContentKey.of("xxx", "test"))
                        .content(table)
                        .expectedContent(currentTable)
                        .build())
                .commitMeta(CommitMeta.fromMessage(""))
                .build())
        .queryParam("expectedHash", b2.getHash())
        .post("trees/branch/{branch}/commit", b2.getName())
        .then()
        .statusCode(200)
        .extract()
        .as(Branch.class);
    Content returned =
        rest()
            .queryParam("ref", "test")
            .get("contents/xxx.test")
            .then()
            .statusCode(200)
            .extract()
            .as(Content.class);
    Assertions.assertEquals(table, returned);

    Branch b3 = rest().get("trees/tree/test").as(Branch.class);
    rest()
        .body(Tag.of("tagtest", b3.getHash()))
        .queryParam("sourceRefName", b3.getName())
        .post("trees/tree")
        .then()
        .statusCode(200);

    assertThat(
            rest()
                .get("trees/tree/tagtest")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Tag.class)
                .getHash())
        .isEqualTo(b3.getHash());

    rest()
        .queryParam(
            "expectedHash",
            "0011223344556677889900112233445566778899001122334455667788990011"
                .substring(0, b2.getHash().length()))
        .delete("trees/tag/tagtest")
        .then()
        .statusCode(409);

    rest()
        .queryParam("expectedHash", b3.getHash())
        .delete("trees/tag/tagtest")
        .then()
        .statusCode(204);

    LogResponse log =
        rest().get("trees/tree/test/log").then().statusCode(200).extract().as(LogResponse.class);
    Assertions.assertEquals(3, log.getLogEntries().size());

    Branch b1 = rest().get("trees/tree/test").as(Branch.class);
    rest()
        .queryParam("expectedHash", b1.getHash())
        .delete("trees/branch/test")
        .then()
        .statusCode(204);
    Branch bx = rest().get("trees/tree/mainx").as(Branch.class);
    rest()
        .queryParam("expectedHash", bx.getHash())
        .delete("trees/branch/mainx")
        .then()
        .statusCode(204);
  }

  private static Content withoutId(Content content) {
    if (content instanceof IcebergTable) {
      return IcebergTable.builder().from(content).id(null).build();
    }
    throw new IllegalArgumentException("Expected IcebergTable, got " + content);
  }

  private static RequestSpecification rest() {
    return given()
        .when()
        .baseUri(RestAssured.baseURI)
        .basePath(basePath)
        .contentType(ContentType.JSON);
  }

  private Branch commit(String contentId, Branch branch, String contentKey, String metadataUrl) {
    return commit(contentId, branch, contentKey, metadataUrl, "nessieAuthor", null);
  }

  private Branch commit(
      String contentId,
      Branch branch,
      String contentKey,
      String metadataUrl,
      String author,
      String expectedMetadataUrl) {
    Operations contents =
        ImmutableOperations.builder()
            .addOperations(
                (expectedMetadataUrl != null)
                    ? Put.of(
                        ContentKey.of(contentKey),
                        IcebergTable.of(metadataUrl, 42, 42, 42, 42, contentId),
                        IcebergTable.of(expectedMetadataUrl, 42, 42, 42, 42, contentId))
                    : Put.of(
                        ContentKey.of(contentKey),
                        IcebergTable.of(metadataUrl, 42, 42, 42, 42, contentId)))
            .commitMeta(CommitMeta.builder().author(author).message("").build())
            .build();
    return rest()
        .body(contents)
        .queryParam("expectedHash", branch.getHash())
        .post("trees/branch/{branch}/commit", branch.getName())
        .then()
        .statusCode(200)
        .extract()
        .as(Branch.class);
  }

  private Branch commit(ContentKey contentKey, Content content, Branch branch, String author) {
    Operations contents =
        ImmutableOperations.builder()
            .addOperations(Put.of(contentKey, content))
            .commitMeta(CommitMeta.builder().author(author).message("").build())
            .build();
    return rest()
        .body(contents)
        .queryParam("expectedHash", branch.getHash())
        .post("trees/branch/{branch}/commit", branch.getName())
        .then()
        .statusCode(200)
        .extract()
        .as(Branch.class);
  }

  private Branch getBranch(String name) {
    return rest().get("trees/tree/{name}", name).then().statusCode(200).extract().as(Branch.class);
  }

  private Branch makeBranch(String name) {
    Branch test = ImmutableBranch.builder().name(name).build();
    return rest().body(test).post("trees/tree").then().statusCode(200).extract().as(Branch.class);
  }

  @Test
  public void testOptimisticLocking() {
    makeBranch("test3");
    Branch b1 = getBranch("test3");
    String contentId = "cid-test-opt-lock";
    String newHash = commit(contentId, b1, "xxx.test", "/the/directory/over/there").getHash();
    Assertions.assertNotEquals(b1.getHash(), newHash);

    Branch b2 = getBranch("test3");
    newHash =
        commit(
                contentId,
                b2,
                "xxx.test",
                "/the/directory/over/there/has/been/moved",
                "i",
                "/the/directory/over/there")
            .getHash();
    Assertions.assertNotEquals(b2.getHash(), newHash);

    Branch b3 = getBranch("test3");
    newHash =
        commit(
                contentId,
                b3,
                "xxx.test",
                "/the/directory/over/there/has/been/moved/again",
                "me",
                "/the/directory/over/there/has/been/moved")
            .getHash();
    Assertions.assertNotEquals(b3.getHash(), newHash);
  }

  @Test
  public void testLogFiltering() {
    String branchName = "logFiltering";
    makeBranch(branchName);
    Branch branch = getBranch(branchName);
    int numCommits = 3;
    String contentId = "cid-test-log-filtering";
    for (int i = 0; i < numCommits; i++) {
      String newHash =
          commit(
                  contentId,
                  branch,
                  "xxx.test",
                  "/the/directory/over/there",
                  "author-" + i,
                  i > 0 ? "/the/directory/over/there" : null)
              .getHash();
      assertThat(newHash).isNotEqualTo(branch.getHash());
      branch = getBranch(branchName);
    }
    LogResponse log =
        rest()
            .get(String.format("trees/tree/%s/log", branchName))
            .then()
            .statusCode(200)
            .extract()
            .as(LogResponse.class);
    assertThat(log.getLogEntries()).hasSize(numCommits);
    Instant firstCommitTime =
        log.getLogEntries().get(log.getLogEntries().size() - 1).getCommitMeta().getCommitTime();
    Instant lastCommitTime = log.getLogEntries().get(0).getCommitMeta().getCommitTime();
    assertThat(firstCommitTime).isNotNull();
    assertThat(lastCommitTime).isNotNull();

    String author = "author-1";
    log =
        rest()
            .queryParam("filter", String.format("commit.author=='%s'", author))
            .get(String.format("trees/tree/%s/log", branchName))
            .then()
            .statusCode(200)
            .extract()
            .as(LogResponse.class);
    assertThat(log.getLogEntries()).hasSize(1);
    assertThat(log.getLogEntries().get(0).getCommitMeta().getAuthor()).isEqualTo(author);

    log =
        rest()
            .queryParam(
                "filter",
                String.format("timestamp(commit.commitTime) > timestamp('%s')", firstCommitTime))
            .get(String.format("trees/tree/%s/log", branchName))
            .then()
            .statusCode(200)
            .extract()
            .as(LogResponse.class);
    assertThat(log.getLogEntries()).hasSize(numCommits - 1);
    log.getLogEntries()
        .forEach(
            commit -> assertThat(commit.getCommitMeta().getCommitTime()).isAfter(firstCommitTime));

    log =
        rest()
            .queryParam(
                "filter",
                String.format("timestamp(commit.commitTime) < timestamp('%s')", lastCommitTime))
            .get(String.format("trees/tree/%s/log", branchName))
            .then()
            .statusCode(200)
            .extract()
            .as(LogResponse.class);
    assertThat(log.getLogEntries()).hasSize(numCommits - 1);
    log.getLogEntries()
        .forEach(
            commit -> assertThat(commit.getCommitMeta().getCommitTime()).isBefore(lastCommitTime));

    log =
        rest()
            .queryParam(
                "filter",
                String.format(
                    "timestamp(commit.commitTime) > timestamp('%s') && timestamp(commit.commitTime) < timestamp('%s')",
                    firstCommitTime, lastCommitTime))
            .get(String.format("trees/tree/%s/log", branchName))
            .then()
            .statusCode(200)
            .extract()
            .as(LogResponse.class);
    assertThat(log.getLogEntries()).hasSize(1);
    assertThat(log.getLogEntries().get(0).getCommitMeta().getCommitTime())
        .isBefore(lastCommitTime)
        .isAfter(firstCommitTime);
  }

  @ParameterizedTest
  @CsvSource({
    "simple,name",
    "simple,dotted.txt",
    "dotted.prefix,name",
    "dotted.prefix,dotted.txt",
  })
  public void testGetContent(String ns, String name) {
    Branch branch = makeBranch("content-test-" + UUID.randomUUID());
    IcebergTable table = IcebergTable.of("content-table1", 42, 42, 42, 42);

    ContentKey key = ContentKey.of(ns, name);
    branch = commit(key, table, branch, "test author");

    Content content =
        rest()
            .queryParam("ref", branch.getName())
            .queryParam("hashOnRef", branch.getHash())
            .get(String.format("contents/%s", key.toPathString()))
            .then()
            .statusCode(200)
            .extract()
            .as(Content.class);

    assertThat(withoutId(content)).isEqualTo(table);
  }

  @Test
  public void testGetDiff() {
    Branch fromBranch = makeBranch("getdiff-test-from");
    Branch toBranch = makeBranch("getdiff-test-to");
    IcebergTable fromTable = IcebergTable.of("content-table", 42, 42, 42, 42);
    IcebergTable toTable = IcebergTable.of("content-table", 43, 43, 43, 43);

    ContentKey contentKey = ContentKey.of("key1");
    commit(contentKey, fromTable, fromBranch, "diffAuthor");
    commit(contentKey, toTable, toBranch, "diffAuthor2");

    DiffResponse diffResponse =
        rest()
            .get(String.format("diffs/%s...%s", fromBranch.getName(), toBranch.getName()))
            .then()
            .statusCode(200)
            .extract()
            .as(DiffResponse.class);

    assertThat(diffResponse).isNotNull();
    assertThat(diffResponse.getDiffs()).hasSize(1);
    DiffEntry diff = diffResponse.getDiffs().get(0);
    assertThat(diff.getKey()).isEqualTo(contentKey);
    assertThat(withoutId(diff.getFrom())).isEqualTo(fromTable);
    assertThat(withoutId(diff.getTo())).isEqualTo(toTable);
  }

  @Test
  public void testGetRefLog() {
    Branch branch = makeBranch("branch-temp");
    IcebergTable table = IcebergTable.of("content-table", 42, 42, 42, 42);

    ContentKey contentKey = ContentKey.of("key1");
    commit(contentKey, table, branch, "code");

    RefLogResponse refLogResponse =
        rest().get("reflogs").then().statusCode(200).extract().as(RefLogResponse.class);

    assertThat(refLogResponse.getLogEntries().get(0).getOperation()).isEqualTo("CREATE_REFERENCE");
    assertThat(refLogResponse.getLogEntries().get(0).getRefName()).isEqualTo("branch-temp");

    RefLogResponse refLogResponse1 =
        rest()
            .queryParam("endHash", refLogResponse.getLogEntries().get(0).getRefLogId())
            .get("reflogs")
            .then()
            .statusCode(200)
            .extract()
            .as(RefLogResponse.class);
    assertThat(refLogResponse1.getLogEntries().get(0).getRefLogId())
        .isEqualTo(refLogResponse.getLogEntries().get(0).getRefLogId());
  }

  @Test
  public void testGetUnknownContentType() {
    String nsName = "foo";
    String branchName = "unknown-content-type";
    String path = String.format("namespaces/namespace/%s/%s", branchName, nsName);

    makeBranch(branchName);
    Namespace ns = Namespace.of("id");

    // Elicit 415 when PUTting a namespace with a content type not consumed by the server
    rest()
        .body(ns)
        .contentType(ContentType.TEXT)
        .put(path)
        .then()
        .statusCode(415)
        .statusLine(StringContains.containsStringIgnoringCase("Unsupported Media Type"));

    // Rerun the request, but with a supported content type (text/json)
    rest().body(ns).put(path).then().statusCode(200);
  }

  @Test
  public void testNullMergePayload() {
    NessieError response =
        rest()
            .post("trees/branch/main/merge")
            .then()
            .statusCode(400)
            .extract()
            .as(NessieError.class);
    assertThat(response.getMessage()).contains(".merge: must not be null");
  }

  @Test
  public void testNullTagPayload() {
    NessieError response =
        rest().put("trees/tag/newTag").then().statusCode(400).extract().as(NessieError.class);
    assertThat(response.getMessage()).contains(".assignTo: must not be null");
    assertThat(response.getMessage()).contains(".expectedHash: must not be null");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "abc'",
        ".foo",
        "abc'def'..'blah",
        "abc'de..blah",
        "abc'de@{blah",
      })
  public void testInvalidTag(String invalidTagName) {
    String validHash = "0011223344556677";
    String validRefName = "hello";

    assertAll(
        () ->
            assertThat(
                    rest()
                        // Need the string-ified JSON representation of `Tag` here, because `Tag`
                        // itself performs validation.
                        .body(
                            "{\"type\": \"TAG\", \"name\": \""
                                + invalidTagName
                                + "\", \"hash\": \""
                                + validHash
                                + "\"}")
                        .put("trees/tag/" + validRefName)
                        .then()
                        .statusCode(400)
                        .extract()
                        .as(NessieError.class)
                        .getMessage())
                .contains(
                    "Cannot construct instance of `org.projectnessie.model.ImmutableTag`, problem: "
                        + REF_NAME_MESSAGE
                        + " - but was: "
                        + invalidTagName
                        + "\n"),
        () ->
            assertThat(
                    rest()
                        .body(
                            "{\"type\": \"TAG\", \"name\": \""
                                + invalidTagName
                                + "\", \"hash\": \""
                                + validHash
                                + "\"}")
                        .put("trees/tag/" + validRefName)
                        .then()
                        .statusCode(400)
                        .extract()
                        .as(NessieError.class)
                        .getMessage())
                .contains(
                    "Cannot construct instance of `org.projectnessie.model.ImmutableTag`, problem: "
                        + REF_NAME_MESSAGE),
        () ->
            assertThat(
                    rest()
                        .body(
                            "{\"type\": \"FOOBAR\", \"name\": \""
                                + validRefName
                                + "\", \"hash\": \""
                                + validHash
                                + "\"}")
                        .put("trees/tag/" + validRefName)
                        .then()
                        .statusCode(400)
                        .extract()
                        .as(NessieError.class)
                        .getMessage())
                .contains(
                    "Could not resolve type id 'FOOBAR' as a subtype of `org.projectnessie.model.Reference`"));
  }
}
