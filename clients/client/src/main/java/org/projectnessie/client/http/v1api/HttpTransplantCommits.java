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

import java.util.List;
import org.projectnessie.apiv1.model.ImmutableTransplant;
import org.projectnessie.client.api.TransplantCommitsBuilder;
import org.projectnessie.client.http.NessieApiClient;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.MergeResponse;

final class HttpTransplantCommits extends BaseHttpOnBranchRequest<TransplantCommitsBuilder>
    implements TransplantCommitsBuilder {

  private final ImmutableTransplant.Builder transplant = ImmutableTransplant.builder();
  private String message;

  HttpTransplantCommits(NessieApiClient client) {
    super(client);
  }

  @Override
  public TransplantCommitsBuilder message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public TransplantCommitsBuilder fromRefName(String fromRefName) {
    transplant.fromRefName(fromRefName);
    return this;
  }

  @Override
  public TransplantCommitsBuilder hashesToTransplant(List<String> hashesToTransplant) {
    transplant.addAllHashesToTransplant(hashesToTransplant);
    return this;
  }

  @Override
  public TransplantCommitsBuilder keepIndividualCommits(boolean keepIndividualCommits) {
    transplant.keepIndividualCommits(keepIndividualCommits);
    return this;
  }

  @Override
  public TransplantCommitsBuilder dryRun(boolean dryRun) {
    transplant.isDryRun(dryRun);
    return this;
  }

  @Override
  public TransplantCommitsBuilder fetchAdditionalInfo(boolean fetchAdditionalInfo) {
    transplant.isFetchAdditionalInfo(fetchAdditionalInfo);
    return this;
  }

  @Override
  public TransplantCommitsBuilder returnConflictAsResult(boolean returnConflictAsResult) {
    transplant.isReturnConflictAsResult(returnConflictAsResult);
    return this;
  }

  @Override
  public MergeResponse transplant() throws NessieNotFoundException, NessieConflictException {
    return client
        .getTreeApi()
        .transplantCommitsIntoBranch(branchName, hash, message, transplant.build());
  }
}
