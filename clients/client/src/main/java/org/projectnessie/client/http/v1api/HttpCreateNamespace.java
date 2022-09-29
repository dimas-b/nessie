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

import org.projectnessie.apiv1.http.HttpNamespaceApi;
import org.projectnessie.apiv1.params.NamespaceParams;
import org.projectnessie.client.builder.BaseCreateNamespaceBuilder;
import org.projectnessie.error.NessieNamespaceAlreadyExistsException;
import org.projectnessie.error.NessieReferenceNotFoundException;
import org.projectnessie.model.ImmutableNamespace;
import org.projectnessie.model.Namespace;

final class HttpCreateNamespace extends BaseCreateNamespaceBuilder {

  private final HttpNamespaceApi api;

  HttpCreateNamespace(HttpNamespaceApi api) {
    this.api = api;
  }

  @Override
  public Namespace create()
      throws NessieNamespaceAlreadyExistsException, NessieReferenceNotFoundException {
    NamespaceParams params =
        NamespaceParams.builder()
            .namespace(namespace)
            .refName(refName)
            .hashOnRef(hashOnRef)
            .build();
    return api.createNamespace(
        params, ImmutableNamespace.builder().from(namespace).properties(properties).build());
  }
}
