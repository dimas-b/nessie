/*
 * Copyright (C) 2022 Dremio
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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.immutables.builder.Builder.Constructor;
import org.projectnessie.model.Validation;

public class DiffParams {

  @NotNull
  @Pattern(regexp = Validation.REF_NAME_PATH_REGEX, message = Validation.REF_NAME_PATH_MESSAGE)
  @Parameter(ref = "refPathFromParameter")
  @PathParam("from-ref")
  private String fromRef;

  @NotNull
  @Pattern(regexp = Validation.REF_NAME_PATH_REGEX, message = Validation.REF_NAME_PATH_MESSAGE)
  @Parameter(ref = "refPathToParameter")
  @PathParam("to-ref")
  private String toRef;

  public DiffParams() {}

  @Constructor
  DiffParams(@NotNull String fromRef, @NotNull String toRef) {
    this.fromRef = fromRef;
    this.toRef = toRef;
  }

  public String getFromRef() {
    return fromRef;
  }

  public String getToRef() {
    return toRef;
  }

  public static DiffParamsBuilder builder() {
    return new DiffParamsBuilder();
  }
}
