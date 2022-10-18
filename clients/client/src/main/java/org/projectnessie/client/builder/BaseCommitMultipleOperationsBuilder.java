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
import org.projectnessie.client.api.CommitMultipleOperationsBuilder;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.ImmutableOperations;
import org.projectnessie.model.Operation;

public abstract class BaseCommitMultipleOperationsBuilder
    extends BaseOnBranchRequest<CommitMultipleOperationsBuilder>
    implements CommitMultipleOperationsBuilder {

  protected final ImmutableOperations.Builder operations = ImmutableOperations.builder();

  @Override
  public CommitMultipleOperationsBuilder commitMeta(CommitMeta commitMeta) {
    operations.commitMeta(commitMeta);
    return this;
  }

  @Override
  public CommitMultipleOperationsBuilder operations(List<Operation> operations) {
    this.operations.addAllOperations(operations);
    return this;
  }

  @Override
  public CommitMultipleOperationsBuilder operation(Operation operation) {
    operations.addOperations(operation);
    return this;
  }
}
