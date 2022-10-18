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

import org.projectnessie.apiv1.http.HttpDiffApi;
import org.projectnessie.apiv1.params.DiffParams;
import org.projectnessie.apiv1.params.DiffParamsBuilder;
import org.projectnessie.client.builder.BaseGetDiffBuilder;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.DiffResponse;

final class HttpGetDiff extends BaseGetDiffBuilder {

  private final HttpDiffApi api;

  HttpGetDiff(HttpDiffApi api) {
    this.api = api;
  }

  @Override
  public DiffResponse get() throws NessieNotFoundException {
    DiffParamsBuilder builder =
        DiffParams.builder()
            .fromRef(fromRefName)
            .fromHashOnRef(fromHashOnRef)
            .toRef(toRefName)
            .toHashOnRef(toHashOnRef);
    return api.getDiff(builder.build());
  }
}
