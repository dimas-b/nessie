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
package org.projectnessie.client.http.v1api;

import org.projectnessie.client.api.AssignBranchBuilder;
import org.projectnessie.client.api.AssignTagBuilder;
import org.projectnessie.client.api.CommitMultipleOperationsBuilder;
import org.projectnessie.client.api.CreateNamespaceBuilder;
import org.projectnessie.client.api.CreateReferenceBuilder;
import org.projectnessie.client.api.DeleteBranchBuilder;
import org.projectnessie.client.api.DeleteNamespaceBuilder;
import org.projectnessie.client.api.DeleteTagBuilder;
import org.projectnessie.client.api.GetAllReferencesBuilder;
import org.projectnessie.client.api.GetCommitLogBuilder;
import org.projectnessie.client.api.GetContentBuilder;
import org.projectnessie.client.api.GetDiffBuilder;
import org.projectnessie.client.api.GetEntriesBuilder;
import org.projectnessie.client.api.GetMultipleNamespacesBuilder;
import org.projectnessie.client.api.GetNamespaceBuilder;
import org.projectnessie.client.api.GetRefLogBuilder;
import org.projectnessie.client.api.GetReferenceBuilder;
import org.projectnessie.client.api.MergeReferenceBuilder;
import org.projectnessie.client.api.NessieApiV1;
import org.projectnessie.client.api.TransplantCommitsBuilder;
import org.projectnessie.client.api.UpdateNamespaceBuilder;
import org.projectnessie.client.http.NessieApiClient;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.NessieConfiguration;

public final class HttpApiV1 implements NessieApiV1 {

  private final NessieApiClient client;

  public HttpApiV1(NessieApiClient client) {
    this.client = client;
  }

  @Override
  public void close() {
    client.close();
  }

  @Override
  public NessieConfiguration getConfig() {
    return client.getConfigApi().getConfig();
  }

  @Override
  public Branch getDefaultBranch() throws NessieNotFoundException {
    return client.getTreeApi().getDefaultBranch();
  }

  @Override
  public GetContentBuilder getContent() {
    return new HttpGetContent(client.getContentApi());
  }

  @Override
  public GetAllReferencesBuilder getAllReferences() {
    return new HttpGetAllReferences(client.getTreeApi());
  }

  @Override
  public GetReferenceBuilder getReference() {
    return new HttpGetReference(client.getTreeApi());
  }

  @Override
  public CreateReferenceBuilder createReference() {
    return new HttpCreateReference(client.getTreeApi());
  }

  @Override
  public GetEntriesBuilder getEntries() {
    return new HttpGetEntries(client.getTreeApi());
  }

  @Override
  public GetCommitLogBuilder getCommitLog() {
    return new HttpGetCommitLog(client.getTreeApi());
  }

  @Override
  public AssignTagBuilder assignTag() {
    return new HttpAssignTag(client.getTreeApi());
  }

  @Override
  public DeleteTagBuilder deleteTag() {
    return new HttpDeleteTag(client.getTreeApi());
  }

  @Override
  public AssignBranchBuilder assignBranch() {
    return new HttpAssignBranch(client.getTreeApi());
  }

  @Override
  public DeleteBranchBuilder deleteBranch() {
    return new HttpDeleteBranch(client.getTreeApi());
  }

  @Override
  public TransplantCommitsBuilder transplantCommitsIntoBranch() {
    return new HttpTransplantCommits(client.getTreeApi());
  }

  @Override
  public MergeReferenceBuilder mergeRefIntoBranch() {
    return new HttpMergeReference(client.getTreeApi());
  }

  @Override
  public CommitMultipleOperationsBuilder commitMultipleOperations() {
    return new HttpCommitMultipleOperations(client.getTreeApi());
  }

  @Override
  public GetDiffBuilder getDiff() {
    return new HttpGetDiff(client.getDiffApi());
  }

  @Override
  public GetRefLogBuilder getRefLog() {
    return new HttpGetRefLog(client.getRefLogApi());
  }

  @Override
  public GetNamespaceBuilder getNamespace() {
    return new HttpGetNamespace(client.getNamespaceApi());
  }

  @Override
  public GetMultipleNamespacesBuilder getMultipleNamespaces() {
    return new HttpGetMultipleNamespaces(client.getNamespaceApi());
  }

  @Override
  public CreateNamespaceBuilder createNamespace() {
    return new HttpCreateNamespace(client.getNamespaceApi());
  }

  @Override
  public DeleteNamespaceBuilder deleteNamespace() {
    return new HttpDeleteNamespace(client.getNamespaceApi());
  }

  @Override
  public UpdateNamespaceBuilder updateProperties() {
    return new HttpUpdateNamespace(client.getNamespaceApi());
  }
}
