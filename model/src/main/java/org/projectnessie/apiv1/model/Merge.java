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
package org.projectnessie.apiv1.model;

import static org.projectnessie.model.Validation.validateHash;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.immutables.value.Value;
import org.projectnessie.model.Validation;

@Schema(
    type = SchemaType.OBJECT,
    title = "Merge Operation",
    // Smallrye does neither support JsonFormat nor javax.validation.constraints.Pattern :(
    properties = {
      @SchemaProperty(name = "fromRefName", pattern = Validation.REF_NAME_REGEX),
      @SchemaProperty(name = "fromHash", pattern = Validation.HASH_REGEX)
    })
@Value.Immutable
@JsonSerialize(as = ImmutableMerge.class)
@JsonDeserialize(as = ImmutableMerge.class)
public interface Merge extends BaseMergeTransplant {

  @NotBlank
  @Pattern(regexp = Validation.HASH_REGEX, message = Validation.HASH_MESSAGE)
  String getFromHash();

  /**
   * Validation rule using {@link org.projectnessie.model.Validation#validateHash(String)}
   * (String)}.
   */
  @Value.Check
  default void checkHash() {
    String hash = getFromHash();
    if (hash != null) {
      validateHash(hash);
    }
  }
}
