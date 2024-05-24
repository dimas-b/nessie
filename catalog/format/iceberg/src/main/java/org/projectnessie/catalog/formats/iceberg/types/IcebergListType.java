/*
 * Copyright (C) 2023 Dremio
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
package org.projectnessie.catalog.formats.iceberg.types;

import static org.projectnessie.catalog.formats.iceberg.manifest.Avro.avroNullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.apache.avro.Schema;
import org.immutables.value.Value;
import org.projectnessie.nessie.immutables.NessieImmutable;

@NessieImmutable
@JsonSerialize(as = ImmutableIcebergListType.class)
@JsonDeserialize(as = ImmutableIcebergListType.class)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(IcebergListType.TYPE_NAME)
public interface IcebergListType extends IcebergComplexType {
  String ELEMENT_ID_PROP = "element-id";
  String TYPE_NAME = "list";

  @Override
  @Value.Default
  default String type() {
    return TYPE_NAME;
  }

  int elementId();

  IcebergType element();

  boolean elementRequired();

  @Override
  @Value.NonAttribute
  default Schema avroSchema(int fieldId) {
    Schema elementSchema = element().avroSchema(fieldId);
    if (!elementRequired()) {
      elementSchema = avroNullable(elementSchema);
    }
    Schema schema = Schema.createArray(elementSchema);
    schema.addProp(ELEMENT_ID_PROP, elementId());
    return schema;
  }

  @SuppressWarnings("unused")
  interface Builder {
    @CanIgnoreReturnValue
    Builder clear();

    @CanIgnoreReturnValue
    Builder elementId(int elementId);

    @CanIgnoreReturnValue
    Builder element(IcebergType element);

    @CanIgnoreReturnValue
    Builder elementRequired(boolean elementRequired);

    org.projectnessie.catalog.formats.iceberg.types.IcebergListType build();
  }
}
