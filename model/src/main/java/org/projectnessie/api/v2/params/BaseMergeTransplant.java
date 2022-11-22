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
package org.projectnessie.api.v2.params;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.projectnessie.model.MergeBehavior;
import org.projectnessie.model.MergeKeyBehavior;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Validation;

public interface BaseMergeTransplant {

  @Size(min = 1)
  @JsonInclude(NON_NULL)
  String getMessage();

  @NotBlank
  @Pattern(regexp = Validation.REF_NAME_REGEX, message = Validation.REF_NAME_MESSAGE)
  String getFromRefName();

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
   * When set to {@code true}, the {@code merge} and {@code transplant} operations will return
   * {@link MergeResponse} objects when a content based conflict cannot be resolved, instead of
   * throwing a {@link org.projectnessie.error.NessieReferenceConflictException}.
   */
  @Nullable
  @JsonInclude(Include.NON_NULL)
  Boolean isReturnConflictAsResult();
}
