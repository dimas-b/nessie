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

import org.projectnessie.apiv1.http.HttpTreeApi;
import org.projectnessie.apiv1.model.ImmutableMerge;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.MergeResponse;

final class HttpMergeReference extends BaseMergeReferenceBuilder {

  private final HttpTreeApi api;

  HttpMergeReference(HttpTreeApi api) {
    this.api = api;
  }

  @Override
  public MergeResponse merge() throws NessieNotFoundException, NessieConflictException {
    ImmutableMerge.Builder merge =
        ImmutableMerge.builder()
            .fromHash(fromHash)
            .fromRefName(fromRefName)
            .isDryRun(dryRun)
            .isReturnConflictAsResult(returnConflictAsResult)
            .isFetchAdditionalInfo(fetchAdditionalInfo)
            .keepIndividualCommits(keepIndividualCommits);
    return api.mergeRefIntoBranch(branchName, hash, merge.build());
  }
}
