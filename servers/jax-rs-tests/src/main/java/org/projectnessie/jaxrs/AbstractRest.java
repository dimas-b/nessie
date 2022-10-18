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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.projectnessie.client.api.NessieApiV1;
import org.projectnessie.client.api.NessieApiV2;
import org.projectnessie.client.http.HttpClientBuilder;
import org.projectnessie.error.BaseNessieClientServerException;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.Detached;
import org.projectnessie.model.IcebergTable;
import org.projectnessie.model.Operation.Put;
import org.projectnessie.model.Reference;
import org.projectnessie.model.Tag;

/** See {@link AbstractTestRest} for details about and reason for the inheritance model. */
public abstract class AbstractRest {

  private NessieApiV1 api;

  static {
    // Note: REST tests validate some locale-specific error messages, but expect on the messages to
    // be in ENGLISH. However, the JRE's startup classes (in particular class loaders) may cause the
    // default Locale to be initialized before Maven is able to override the user.language system
    // property. Therefore, we explicitly set the default Locale to ENGLISH here to match tests'
    // expectations.
    Locale.setDefault(Locale.ENGLISH);
  }

  protected void initApi(URI nessieApiUri) {
    NessieApiV1 api =
        HttpClientBuilder.builder().withUri(nessieApiUri.resolve("v2")).build(NessieApiV2.class);
    initApi(api);
  }

  protected void initApi(NessieApiV1 api) {
    this.api = api;
  }

  public NessieApiV1 getApi() {
    return Objects.requireNonNull(api, "Tests need to call initApi in @BeforeEach");
  }

  @AfterEach
  public void tearDown() throws Exception {
    Branch defaultBranch = api.getDefaultBranch();
    api.getAllReferences().stream()
        .forEach(
            ref -> {
              try {
                if (ref instanceof Branch && !ref.getName().equals(defaultBranch.getName())) {
                  api.deleteBranch().branch((Branch) ref).delete();
                } else if (ref instanceof Tag) {
                  api.deleteTag().tag((Tag) ref).delete();
                }
              } catch (NessieConflictException | NessieNotFoundException e) {
                throw new RuntimeException(e);
              }
            });
    api.close();
  }

  public void assumeApiV1() {
    Assumptions.assumeFalse(getApi() instanceof NessieApiV2);
  }

  protected String createCommits(
      Reference branch, int numAuthors, int commitsPerAuthor, String currentHash)
      throws BaseNessieClientServerException {
    for (int j = 0; j < numAuthors; j++) {
      String author = "author-" + j;
      for (int i = 0; i < commitsPerAuthor; i++) {
        IcebergTable meta = IcebergTable.of("some-file-" + i, 42, 42, 42, 42);
        String nextHash =
            getApi()
                .commitMultipleOperations()
                .branchName(branch.getName())
                .hash(currentHash)
                .commitMeta(
                    CommitMeta.builder()
                        .author(author)
                        .message("committed-by-" + author)
                        .properties(ImmutableMap.of("prop1", "val1", "prop2", "val2"))
                        .build())
                .operation(Put.of(ContentKey.of("table" + i), meta))
                .commit()
                .getHash();
        assertThat(currentHash).isNotEqualTo(nextHash);
        currentHash = nextHash;
      }
    }
    return currentHash;
  }

  protected Branch createBranch(String name, Branch from) throws BaseNessieClientServerException {
    Branch expectedBranch;
    String srcBranchName;
    if (from == null) {
      Branch main = getApi().getDefaultBranch();
      expectedBranch = Branch.of(name, main.getHash());
      srcBranchName = "main";
    } else {
      expectedBranch = Branch.of(name, from.getHash());
      srcBranchName = from.getName();
    }
    Reference created =
        getApi()
            .createReference()
            .sourceRefName(srcBranchName)
            .reference(Branch.of(name, expectedBranch.getHash()))
            .create();
    assertThat(created).isEqualTo(expectedBranch);
    return expectedBranch;
  }

  protected Branch createBranch(String name) throws BaseNessieClientServerException {
    return createBranch(name, null);
  }

  protected static void getOrCreateEmptyBranch(NessieApiV1 api, String gcBranchName) {
    try {
      api.getReference().refName(gcBranchName).get();
    } catch (NessieNotFoundException e) {
      // create a reference pointing to NO_ANCESTOR hash.
      try {
        api.createReference().reference(Branch.of(gcBranchName, null)).create();
      } catch (NessieNotFoundException | NessieConflictException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  protected void deleteBranch(String name, String hash) throws BaseNessieClientServerException {
    getApi().deleteBranch().branchName(name).hash(hash).delete();
  }

  /**
   * Used by parameterized tests to return the {@value Detached#REF_NAME}, if {@code
   * withDetachedCommit} is {@code true} or the {@link Reference#getName() reference name} from the
   * given {@code ref}.
   */
  protected static String maybeAsDetachedName(boolean withDetachedCommit, Reference ref) {
    return withDetachedCommit ? Detached.REF_NAME : ref.getName();
  }

  /**
   * Enum intended to be used a test method parameter to transform a {@link Reference} in multiple
   * ways.
   */
  enum ReferenceMode {
    /** Removes the {@link Reference#getHash()} from the reference. */
    NAME_ONLY {
      @Override
      Reference transform(Reference ref) {
        switch (ref.getType()) {
          case TAG:
            return Tag.of(ref.getName(), null);
          case BRANCH:
            return Branch.of(ref.getName(), null);
          default:
            throw new IllegalArgumentException(ref.toString());
        }
      }
    },
    /** Keep the reference unchanged. */
    UNCHANGED {
      @Override
      Reference transform(Reference ref) {
        return ref;
      }
    },
    /**
     * Make the reference a {@link Detached} with its {@link Detached#getHash()} using the hash of
     * the given reference.
     */
    DETACHED {
      @Override
      Reference transform(Reference ref) {
        return Detached.of(ref.getHash());
      }
    };

    abstract Reference transform(Reference ref);
  }
}
