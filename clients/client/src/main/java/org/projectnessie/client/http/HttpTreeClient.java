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
package org.projectnessie.client.http;

import java.util.Locale;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.projectnessie.api.params.FetchOption;
import org.projectnessie.apiv1.http.HttpTreeApi;
import org.projectnessie.apiv1.model.Merge;
import org.projectnessie.apiv1.model.Transplant;
import org.projectnessie.apiv1.params.CommitLogParams;
import org.projectnessie.apiv1.params.EntriesParams;
import org.projectnessie.apiv1.params.GetReferenceParams;
import org.projectnessie.apiv1.params.ReferencesParams;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.EntriesResponse;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Operations;
import org.projectnessie.model.Reference;
import org.projectnessie.model.ReferencesResponse;

class HttpTreeClient implements HttpTreeApi {

  private final HttpClient client;

  public HttpTreeClient(HttpClient client) {
    this.client = client;
  }

  @Override
  public ReferencesResponse getAllReferences(ReferencesParams params) {
    return client
        .newRequest()
        .path("trees")
        .queryParam("maxRecords", params.maxRecords())
        .queryParam("pageToken", params.pageToken())
        .queryParam("fetch", FetchOption.getFetchOptionName(params.fetchOption()))
        .queryParam("filter", params.filter())
        .get()
        .readEntity(ReferencesResponse.class);
  }

  @Override
  public Reference createReference(String sourceRefName, @NotNull Reference reference)
      throws NessieNotFoundException, NessieConflictException {
    return client
        .newRequest()
        .path("trees/tree")
        .queryParam("sourceRefName", sourceRefName)
        .post(reference)
        .readEntity(Reference.class);
  }

  @Override
  public Reference getReferenceByName(@NotNull GetReferenceParams params)
      throws NessieNotFoundException {
    return client
        .newRequest()
        .path("trees/tree/{ref}")
        .queryParam("fetch", FetchOption.getFetchOptionName(params.fetchOption()))
        .resolveTemplate("ref", params.getRefName())
        .get()
        .readEntity(Reference.class);
  }

  @Override
  public void assignReference(
      @NotNull Reference.ReferenceType referenceType,
      @NotNull String referenceName,
      @NotNull String expectedHash,
      @Valid @NotNull Reference assignTo)
      throws NessieNotFoundException, NessieConflictException {
    client
        .newRequest()
        .path("trees/{referenceType}/{referenceName}")
        .resolveTemplate("referenceType", referenceType.name().toLowerCase(Locale.ROOT))
        .resolveTemplate("referenceName", referenceName)
        .queryParam("expectedHash", expectedHash)
        .put(assignTo);
  }

  @Override
  public void deleteReference(
      @NotNull Reference.ReferenceType referenceType,
      @NotNull String referenceName,
      @NotNull String expectedHash)
      throws NessieConflictException, NessieNotFoundException {
    client
        .newRequest()
        .path("trees/{referenceType}/{referenceName}")
        .resolveTemplate("referenceType", referenceType.name().toLowerCase(Locale.ROOT))
        .resolveTemplate("referenceName", referenceName)
        .queryParam("expectedHash", expectedHash)
        .delete();
  }

  @Override
  public Branch getDefaultBranch() {
    return client.newRequest().path("trees/tree").get().readEntity(Branch.class);
  }

  @Override
  public LogResponse getCommitLog(@NotNull String ref, @NotNull CommitLogParams params)
      throws NessieNotFoundException {
    HttpRequest builder =
        client.newRequest().path("trees/tree/{ref}/log").resolveTemplate("ref", ref);
    return builder
        .queryParam("maxRecords", params.maxRecords())
        .queryParam("pageToken", params.pageToken())
        .queryParam("filter", params.filter())
        .queryParam("startHash", params.startHash())
        .queryParam("endHash", params.endHash())
        .queryParam("fetch", FetchOption.getFetchOptionName(params.fetchOption()))
        .get()
        .readEntity(LogResponse.class);
  }

  @Override
  public MergeResponse transplantCommitsIntoBranch(
      @NotNull String branchName,
      @NotNull String expectedHash,
      String message,
      @Valid Transplant transplant)
      throws NessieNotFoundException, NessieConflictException {
    return client
        .newRequest()
        .path("trees/branch/{branchName}/transplant")
        .resolveTemplate("branchName", branchName)
        .queryParam("expectedHash", expectedHash)
        .queryParam("message", message)
        .post(transplant)
        .readEntity(MergeResponse.class);
  }

  @Override
  public MergeResponse mergeRefIntoBranch(
      @NotNull String branchName, @NotNull String expectedHash, @NotNull @Valid Merge merge)
      throws NessieNotFoundException, NessieConflictException {
    return client
        .newRequest()
        .path("trees/branch/{branchName}/merge")
        .resolveTemplate("branchName", branchName)
        .queryParam("expectedHash", expectedHash)
        .post(merge)
        .readEntity(MergeResponse.class);
  }

  @Override
  public EntriesResponse getEntries(@NotNull String refName, @NotNull EntriesParams params)
      throws NessieNotFoundException {
    HttpRequest builder =
        client.newRequest().path("trees/tree/{ref}/entries").resolveTemplate("ref", refName);
    return builder
        .queryParam("maxRecords", params.maxRecords())
        .queryParam("pageToken", params.pageToken())
        .queryParam("filter", params.filter())
        .queryParam("hashOnRef", params.hashOnRef())
        .queryParam(
            "namespaceDepth",
            params.namespaceDepth() == null ? null : String.valueOf(params.namespaceDepth()))
        .get()
        .readEntity(EntriesResponse.class);
  }

  @Override
  public Branch commitMultipleOperations(
      String branch, @NotNull String expectedHash, @NotNull Operations operations)
      throws NessieNotFoundException, NessieConflictException {
    return client
        .newRequest()
        .path("trees/branch/{branchName}/commit")
        .resolveTemplate("branchName", branch)
        .queryParam("expectedHash", expectedHash)
        .post(operations)
        .readEntity(Branch.class);
  }
}
