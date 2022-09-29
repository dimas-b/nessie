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
package org.projectnessie.client.builder;

import java.util.List;
import org.projectnessie.client.api.TransplantCommitsBuilder;

public abstract class BaseTransplantCommitsBuilder
    extends BaseOnBranchRequest<TransplantCommitsBuilder> implements TransplantCommitsBuilder {

  protected String message;
  protected String fromRefName;
  protected List<String> hashesToTransplant;
  protected Boolean keepIndividualCommits;
  protected Boolean dryRun;
  protected Boolean returnConflictAsResult;
  protected Boolean fetchAdditionalInfo;

  @Override
  public TransplantCommitsBuilder message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public TransplantCommitsBuilder fromRefName(String fromRefName) {
    this.fromRefName = fromRefName;
    return this;
  }

  @Override
  public TransplantCommitsBuilder hashesToTransplant(List<String> hashesToTransplant) {
    this.hashesToTransplant = hashesToTransplant;
    return this;
  }

  @Override
  public TransplantCommitsBuilder keepIndividualCommits(boolean keepIndividualCommits) {
    this.keepIndividualCommits = keepIndividualCommits;
    return this;
  }

  @Override
  public TransplantCommitsBuilder dryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  @Override
  public TransplantCommitsBuilder fetchAdditionalInfo(boolean fetchAdditionalInfo) {
    this.fetchAdditionalInfo = fetchAdditionalInfo;
    return this;
  }

  @Override
  public TransplantCommitsBuilder returnConflictAsResult(boolean returnConflictAsResult) {
    this.returnConflictAsResult = returnConflictAsResult;
    return this;
  }
}
