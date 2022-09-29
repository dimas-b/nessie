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
package org.projectnessie.client.http.v1api;

import org.projectnessie.apiv1.http.HttpTreeApi;
import org.projectnessie.apiv1.params.GetReferenceParams;
import org.projectnessie.client.builder.BaseGetReferenceBuilder;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Reference;

final class HttpGetReference extends BaseGetReferenceBuilder {
  private final HttpTreeApi api;

  HttpGetReference(HttpTreeApi api) {
    this.api = api;
  }

  @Override
  public Reference get() throws NessieNotFoundException {
    return api.getReferenceByName(
        GetReferenceParams.builder().refName(refName).fetchOption(fetchOption).build());
  }
}
