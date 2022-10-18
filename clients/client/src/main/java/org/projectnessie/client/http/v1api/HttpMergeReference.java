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

import org.projectnessie.apiv1.model.ImmutableMerge;
import org.projectnessie.client.api.MergeReferenceBuilder;
import org.projectnessie.client.http.NessieApiClient;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.MergeResponse;

final class HttpMergeReference extends BaseHttpOnBranchRequest<MergeReferenceBuilder>
    implements MergeReferenceBuilder {

  private final ImmutableMerge.Builder merge = ImmutableMerge.builder();

  HttpMergeReference(NessieApiClient client) {
    super(client);
  }

  @Override
  public MergeReferenceBuilder fromRefName(String fromRefName) {
    merge.fromRefName(fromRefName);
    return this;
  }

  @Override
  public MergeReferenceBuilder fromHash(String fromHash) {
    merge.fromHash(fromHash);
    return this;
  }

  @Override
  public MergeReferenceBuilder keepIndividualCommits(boolean keepIndividualCommits) {
    merge.keepIndividualCommits(keepIndividualCommits);
    return this;
  }

  @Override
  public MergeReferenceBuilder dryRun(boolean dryRun) {
    merge.isDryRun(dryRun);
    return this;
  }

  @Override
  public MergeReferenceBuilder fetchAdditionalInfo(boolean fetchAdditionalInfo) {
    merge.isFetchAdditionalInfo(fetchAdditionalInfo);
    return this;
  }

  @Override
  public MergeReferenceBuilder returnConflictAsResult(boolean returnConflictAsResult) {
    merge.isReturnConflictAsResult(returnConflictAsResult);
    return this;
  }

  @Override
  public MergeResponse merge() throws NessieNotFoundException, NessieConflictException {
    return client.getTreeApi().mergeRefIntoBranch(branchName, hash, merge.build());
  }
}
