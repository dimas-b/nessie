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
package org.projectnessie.catalog.formats.iceberg.rest;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.projectnessie.catalog.formats.iceberg.meta.IcebergTableMetadata;

public interface IcebergLoadTableResult extends IcebergBaseTableResult {

  @Nullable
  @Override
  String metadataLocation();

  @Override
  IcebergTableMetadata metadata();

  Map<String, String> config();

  @SuppressWarnings("unused")
  interface Builder<R extends IcebergLoadTableResult, B extends Builder<R, B>>
      extends IcebergBaseTableResult.Builder<R, B> {
    @CanIgnoreReturnValue
    B from(IcebergLoadTableResult instance);

    @CanIgnoreReturnValue
    B metadataLocation(@Nullable String metadataLocation);

    @CanIgnoreReturnValue
    B metadata(IcebergTableMetadata metadata);

    @CanIgnoreReturnValue
    B putConfig(String key, String value);

    @CanIgnoreReturnValue
    B putConfig(Map.Entry<String, ? extends String> entry);

    @CanIgnoreReturnValue
    B config(Map<String, ? extends String> entries);

    @CanIgnoreReturnValue
    B putAllConfig(Map<String, ? extends String> entries);

    R build();
  }
}
