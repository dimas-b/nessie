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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.projectnessie.error.BaseNessieClientServerException;
import org.projectnessie.error.NessieBadRequestException;
import org.projectnessie.error.NessieReferenceAlreadyExistsException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.IcebergTable;
import org.projectnessie.model.Operation.Put;
import org.projectnessie.model.Operation.Unchanged;

/** See {@link AbstractTestRest} for details about and reason for the inheritance model. */
public abstract class AbstractRestMisc extends AbstractRestMergeTransplant {

  @Test
  public void testSupportedApiVersions() {
    assertThat(getApi().getConfig().getMaxSupportedApiVersion()).isEqualTo(2);
  }

  @Test
  public void checkSpecialCharacterRoundTrip() throws BaseNessieClientServerException {
    Branch branch = createBranch("specialchar");
    // ContentKey k = ContentKey.of("/%国","国.国");
    ContentKey key = ContentKey.of("a.b", "c.txt");
    IcebergTable table = IcebergTable.of("path1", 42, 42, 42, 42);
    getApi()
        .commitMultipleOperations()
        .branch(branch)
        .operation(Put.of(key, table))
        .commitMeta(CommitMeta.fromMessage("commit 1"))
        .commit();

    assertThat(getApi().getContent().key(key).refName(branch.getName()).get())
        .containsKey(key)
        .hasEntrySatisfying(
            key,
            content ->
                assertThat(content)
                    .isEqualTo(IcebergTable.builder().from(table).id(content.getId()).build()));
  }

  @Test
  public void checkServerErrorPropagation() throws BaseNessieClientServerException {
    Branch branch = createBranch("bar");

    assertThatThrownBy(
            () -> getApi().createReference().sourceRefName("main").reference(branch).create())
        .isInstanceOf(NessieReferenceAlreadyExistsException.class)
        .hasMessageContaining("already exists");

    assertThatThrownBy(
            () ->
                getApi()
                    .commitMultipleOperations()
                    .branch(branch)
                    .commitMeta(
                        CommitMeta.builder()
                            .author("author")
                            .message("committed-by-test")
                            .committer("disallowed-client-side-committer")
                            .build())
                    .operation(Unchanged.of(ContentKey.of("table")))
                    .commit())
        .isInstanceOf(NessieBadRequestException.class)
        .hasMessageContaining("Cannot set the committer on the client side.");
  }

  @Test
  public void checkCelScriptFailureReporting() {
    assertThatThrownBy(() -> getApi().getEntries().refName("main").filter("invalid_script").get())
        .isInstanceOf(NessieBadRequestException.class)
        .hasMessageContaining("undeclared reference to 'invalid_script'");

    assertThatThrownBy(() -> getApi().getCommitLog().refName("main").filter("invalid_script").get())
        .isInstanceOf(NessieBadRequestException.class)
        .hasMessageContaining("undeclared reference to 'invalid_script'");
  }
}
