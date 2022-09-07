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

import org.projectnessie.api.params.FetchOption;
import org.projectnessie.apiv1.params.GetReferenceParams;
import org.projectnessie.apiv1.params.GetReferenceParamsBuilder;
import org.projectnessie.client.api.GetReferenceBuilder;
import org.projectnessie.client.http.NessieApiClient;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Reference;

final class HttpGetReference extends BaseHttpRequest implements GetReferenceBuilder {
  private GetReferenceParamsBuilder builder = GetReferenceParams.builder();

  HttpGetReference(NessieApiClient client) {
    super(client);
  }

  @Override
  public GetReferenceBuilder refName(String refName) {
    builder.refName(refName);
    return this;
  }

  @Override
  public GetReferenceBuilder fetch(FetchOption fetchOption) {
    builder.fetchOption(fetchOption);
    return this;
  }

  @Override
  public Reference get() throws NessieNotFoundException {
    return client.getTreeApi().getReferenceByName(builder.build());
  }
}
