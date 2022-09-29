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

import org.projectnessie.client.api.MergeReferenceBuilder;
import org.projectnessie.client.builder.BaseOnBranchRequest;

public abstract class BaseMergeReferenceBuilder extends BaseOnBranchRequest<MergeReferenceBuilder>
    implements MergeReferenceBuilder {

  protected String fromRefName;
  protected String fromHash;
  protected Boolean keepIndividualCommits;
  protected Boolean dryRun;
  protected Boolean returnConflictAsResult;
  protected Boolean fetchAdditionalInfo;

  @Override
  public MergeReferenceBuilder fromRefName(String fromRefName) {
    this.fromRefName = fromRefName;
    return this;
  }

  @Override
  public MergeReferenceBuilder fromHash(String fromHash) {
    this.fromHash = fromHash;
    return this;
  }

  @Override
  public MergeReferenceBuilder keepIndividualCommits(boolean keepIndividualCommits) {
    this.keepIndividualCommits = keepIndividualCommits;
    return this;
  }

  @Override
  public MergeReferenceBuilder dryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  @Override
  public MergeReferenceBuilder fetchAdditionalInfo(boolean fetchAdditionalInfo) {
    this.fetchAdditionalInfo = fetchAdditionalInfo;
    return this;
  }

  @Override
  public MergeReferenceBuilder returnConflictAsResult(boolean returnConflictAsResult) {
    this.returnConflictAsResult = returnConflictAsResult;
    return this;
  }
}
