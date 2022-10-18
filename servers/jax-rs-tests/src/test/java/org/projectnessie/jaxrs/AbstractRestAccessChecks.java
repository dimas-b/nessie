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
package org.projectnessie.jaxrs;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.projectnessie.api.params.FetchOption;
import org.projectnessie.apiv1.TreeApi;
import org.projectnessie.apiv1.params.CommitLogParams;
import org.projectnessie.apiv1.params.EntriesParams;
import org.projectnessie.error.NessieForbiddenException;
import org.projectnessie.jaxrs.ext.NessieAccessChecker;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.Detached;
import org.projectnessie.model.EntriesResponse;
import org.projectnessie.model.IcebergTable;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.Operation;
import org.projectnessie.model.Operation.Put;
import org.projectnessie.model.Tag;
import org.projectnessie.services.authz.AbstractBatchAccessChecker;
import org.projectnessie.services.authz.AccessContext;
import org.projectnessie.services.authz.BatchAccessChecker;
import org.projectnessie.services.authz.Check;
import org.projectnessie.services.authz.Check.CheckType;
import org.projectnessie.versioned.DetachedRef;

/** See {@link AbstractTestRest} for details about and reason for the inheritance model. */
public abstract class AbstractRestAccessChecks extends AbstractTestRest {

  private static final String VIEW_MSG = "Must not view detached references";
  private static final String COMMITS_MSG = "Must not list from detached references";
  private static final String READ_MSG = "Must not read from detached references";
  private static final String ENTITIES_MSG = "Must not get entities from detached references";

  private static final Map<CheckType, String> CHECK_TYPE_MSG =
      ImmutableMap.of(
          CheckType.VIEW_REFERENCE, VIEW_MSG,
          CheckType.LIST_COMMIT_LOG, COMMITS_MSG,
          CheckType.READ_ENTITY_VALUE, ENTITIES_MSG,
          CheckType.READ_ENTRIES, READ_MSG);

  /**
   * Verify that response filtering for {@link TreeApi#getCommitLog(String, CommitLogParams)} and
   * {@link TreeApi#getEntries(String, EntriesParams)} does not return disallowed commit-log entries
   * / commit-operations.
   */
  @Test
  public void forbiddenContentKeys(
      @NessieAccessChecker
          Consumer<Function<AccessContext, BatchAccessChecker>> accessCheckerConsumer)
      throws Exception {
    Branch main = createBranch("forbiddenContentKeys");

    ContentKey keyForbidden1 = ContentKey.of("forbidden_1");
    ContentKey keyForbidden2 = ContentKey.of("forbidden_2");
    ContentKey idForbidden1 = ContentKey.of("id_forbidden_1");
    ContentKey idForbidden2 = ContentKey.of("id_forbidden_2");
    ContentKey keyAllowed1 = ContentKey.of("allowed_1");
    ContentKey keyAllowed2 = ContentKey.of("allowed_2");

    String contentIdForbidden1 = UUID.randomUUID().toString();
    String contentIdForbidden2 = UUID.randomUUID().toString();

    Branch commit =
        getApi()
            .commitMultipleOperations()
            .branchName(main.getName())
            .hash(main.getHash())
            .commitMeta(CommitMeta.builder().message("no security context").build())
            .operation(
                Put.of(keyForbidden1, IcebergTable.of(keyForbidden1.getName(), 42, 42, 42, 42)))
            .operation(
                Put.of(
                    idForbidden1,
                    IcebergTable.of(idForbidden1.getName(), 42, 42, 42, 42, contentIdForbidden1)))
            .operation(Put.of(keyAllowed1, IcebergTable.of(keyAllowed1.getName(), 42, 42, 42, 42)))
            .operation(
                Put.of(keyForbidden2, IcebergTable.of(keyForbidden2.getName(), 42, 42, 42, 42)))
            .operation(
                Put.of(
                    idForbidden2,
                    IcebergTable.of(idForbidden2.getName(), 42, 42, 42, 42, contentIdForbidden2)))
            .operation(Put.of(keyAllowed2, IcebergTable.of(keyAllowed2.getName(), 42, 42, 42, 42)))
            .commit();

    ThrowingConsumer<Collection<ContentKey>> assertKeys =
        expectedKeys -> {
          assertThat(getApi().getEntries().reference(commit).get().getEntries())
              .extracting(EntriesResponse.Entry::getName)
              .containsExactlyInAnyOrderElementsOf(expectedKeys);
          assertThat(
                  getApi()
                      .getCommitLog()
                      .reference(commit)
                      .fetch(FetchOption.ALL)
                      .get()
                      .getLogEntries())
              .hasSize(1)
              .element(0)
              .extracting(LogResponse.LogEntry::getOperations)
              .asInstanceOf(InstanceOfAssertFactories.list(Operation.class))
              .map(Operation::getKey)
              .containsExactlyInAnyOrderElementsOf(expectedKeys);
        };

    assertKeys.accept(
        Arrays.asList(
            keyAllowed1, keyAllowed2, keyForbidden1, keyForbidden2, idForbidden1, idForbidden2));

    accessCheckerConsumer.accept(
        x ->
            new AbstractBatchAccessChecker() {
              @Override
              public Map<Check, String> check() {
                return getChecks().stream()
                    .filter(c -> c.type() == CheckType.READ_CONTENT_KEY)
                    .filter(
                        c ->
                            // forbid all content-keys starting with "forbidden"
                            c.key().getName().startsWith("forbidden")
                                // forbid the two content-ids
                                || c.contentId().equals(contentIdForbidden1)
                                || c.contentId().equals(contentIdForbidden2))
                    .collect(
                        Collectors.toMap(
                            Function.identity(), c -> "Forbidden key " + c.key().getName()));
              }
            });

    assertKeys.accept(Arrays.asList(keyAllowed1, keyAllowed2));
  }

  @Test
  public void detachedRefAccessChecks(
      @NessieAccessChecker
          Consumer<Function<AccessContext, BatchAccessChecker>> accessCheckerConsumer)
      throws Exception {

    BatchAccessChecker accessChecker =
        new AbstractBatchAccessChecker() {
          @Override
          public Map<Check, String> check() {
            Map<Check, String> failed = new LinkedHashMap<>();
            getChecks()
                .forEach(
                    check -> {
                      String msg = CHECK_TYPE_MSG.get(check.type());
                      if (msg != null) {
                        if (check.ref() instanceof DetachedRef) {
                          failed.put(check, msg);
                        } else {
                          assertThat(check.ref().getName()).isNotEqualTo(DetachedRef.REF_NAME);
                        }
                      }
                    });
            return failed;
          }
        };

    accessCheckerConsumer.accept(x -> accessChecker);

    Branch main = createBranch("committerAndAuthor");
    Branch merge = createBranch("committerAndAuthorMerge");
    Branch transplant = createBranch("committerAndAuthorTransplant");

    IcebergTable meta1 = IcebergTable.of("meep", 42, 42, 42, 42);
    ContentKey key = ContentKey.of("meep");
    Branch mainCommit =
        getApi()
            .commitMultipleOperations()
            .branchName(main.getName())
            .hash(main.getHash())
            .commitMeta(CommitMeta.builder().message("no security context").build())
            .operation(Put.of(key, meta1))
            .commit();

    Branch detachedAsBranch = Branch.of(Detached.REF_NAME, mainCommit.getHash());
    Tag detachedAsTag = Tag.of(Detached.REF_NAME, mainCommit.getHash());
    Detached detached = Detached.of(mainCommit.getHash());

    assertThat(Stream.of(detached, detachedAsBranch, detachedAsTag))
        .allSatisfy(
            ref ->
                assertAll(
                    () ->
                        assertThatThrownBy(() -> getApi().getCommitLog().reference(ref).get())
                            .describedAs("ref='%s', getCommitLog", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(COMMITS_MSG),
                    () ->
                        assertThatThrownBy(
                                () ->
                                    getApi()
                                        .mergeRefIntoBranch()
                                        .fromRef(ref)
                                        .branch(merge)
                                        .merge())
                            .describedAs("ref='%s', mergeRefIntoBranch", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(VIEW_MSG),
                    () ->
                        assertThatThrownBy(
                                () ->
                                    getApi()
                                        .transplantCommitsIntoBranch()
                                        .fromRefName(ref.getName())
                                        .hashesToTransplant(singletonList(ref.getHash()))
                                        .branch(transplant)
                                        .transplant())
                            .describedAs("ref='%s', transplantCommitsIntoBranch", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(VIEW_MSG),
                    () ->
                        assertThatThrownBy(() -> getApi().getEntries().reference(ref).get())
                            .describedAs("ref='%s', getEntries", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(READ_MSG),
                    () ->
                        assertThatThrownBy(
                                () -> getApi().getContent().reference(ref).key(key).get())
                            .describedAs("ref='%s', getContent", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(ENTITIES_MSG),
                    () ->
                        assertThatThrownBy(() -> getApi().getDiff().fromRef(ref).toRef(main).get())
                            .describedAs("ref='%s', getDiff1", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(VIEW_MSG),
                    () ->
                        assertThatThrownBy(() -> getApi().getDiff().fromRef(main).toRef(ref).get())
                            .describedAs("ref='%s', getDiff2", ref)
                            .isInstanceOf(NessieForbiddenException.class)
                            .hasMessageContaining(VIEW_MSG)));
  }
}
