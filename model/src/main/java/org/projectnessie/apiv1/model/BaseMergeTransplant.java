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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.immutables.value.Value;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.MergeBehavior;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Validation;

public interface BaseMergeTransplant {

  @NotBlank
  @Pattern(regexp = Validation.REF_NAME_REGEX, message = Validation.REF_NAME_MESSAGE)
  String getFromRefName();

  @Nullable
  @JsonInclude(Include.NON_NULL)
  Boolean keepIndividualCommits();

  @Nullable
  @JsonInclude(Include.NON_NULL)
  List<MergeKeyBehavior> getKeyMergeModes();

  @Nullable
  @JsonInclude(Include.NON_NULL)
  MergeBehavior getDefaultKeyMergeMode();

  @Nullable
  @JsonInclude(Include.NON_NULL)
  Boolean isDryRun();

  @Nullable
  @JsonInclude(Include.NON_NULL)
  Boolean isFetchAdditionalInfo();

  /**
   * When set to {@code true}, the {@link org.projectnessie.api.TreeApi#mergeRefIntoBranch(String,
   * String, Merge)} and {@link org.projectnessie.api.TreeApi#transplantCommitsIntoBranch(String,
   * String, String, Transplant)} operations will return {@link MergeResponse} object when a content
   * based conflict cannot be resolved, instead of throwing a {@link
   * org.projectnessie.error.NessieReferenceConflictException}.
   */
  @Nullable
  @JsonInclude(Include.NON_NULL)
  Boolean isReturnConflictAsResult();

  @Value.Immutable
  @JsonSerialize(as = ImmutableMergeKeyBehavior.class)
  @JsonDeserialize(as = ImmutableMergeKeyBehavior.class)
  interface MergeKeyBehavior {
    ContentKey getKey();

    MergeBehavior getMergeBehavior();

    static ImmutableMergeKeyBehavior.Builder builder() {
      return ImmutableMergeKeyBehavior.builder();
    }

    static MergeKeyBehavior of(ContentKey key, MergeBehavior mergeBehavior) {
      return builder().key(key).mergeBehavior(mergeBehavior).build();
    }
  }
}
