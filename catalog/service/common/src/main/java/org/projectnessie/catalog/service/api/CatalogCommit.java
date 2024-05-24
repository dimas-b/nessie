/*
 * Copyright (C) 2024 Dremio
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
package org.projectnessie.catalog.service.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import org.immutables.value.Value;
import org.projectnessie.catalog.model.ops.CatalogOperation;
import org.projectnessie.nessie.immutables.NessieImmutable;

@NessieImmutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableCatalogCommit.class)
@JsonDeserialize(as = ImmutableCatalogCommit.class)
public interface CatalogCommit {

  List<CatalogOperation> getOperations();

  static Builder builder() {
    return ImmutableCatalogCommit.builder();
  }

  interface Builder {
    @CanIgnoreReturnValue
    Builder from(CatalogCommit instance);

    @CanIgnoreReturnValue
    Builder addOperations(CatalogOperation element);

    @CanIgnoreReturnValue
    Builder addOperations(CatalogOperation... elements);

    @CanIgnoreReturnValue
    @JsonProperty("operations")
    Builder operations(Iterable<? extends CatalogOperation> elements);

    @CanIgnoreReturnValue
    Builder addAllOperations(Iterable<? extends CatalogOperation> elements);

    CatalogCommit build();
  }
}
