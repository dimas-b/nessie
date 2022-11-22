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
package org.projectnessie.client.util.v2api;

import java.util.Map;
import java.util.Optional;
import org.projectnessie.client.api.NessieApiV2;
import org.projectnessie.client.builder.BaseCreateNamespaceBuilder;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNamespaceAlreadyExistsException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.error.NessieReferenceNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitMeta;
import org.projectnessie.model.Content;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.ImmutableNamespace;
import org.projectnessie.model.Namespace;
import org.projectnessie.model.Operation;

/**
 * Supports previous "create namespace" functionality of the java client over Nessie API v2.
 *
 * <p>API v2 does not have methods dedicated to manging namespaces. Namespaces are expected to be
 * managed as ordinary content objects.
 */
public final class ClientSideCreateNamespace extends BaseCreateNamespaceBuilder {
  private final NessieApiV2 api;

  public ClientSideCreateNamespace(NessieApiV2 api) {
    this.api = api;
  }

  @Override
  public Namespace create()
      throws NessieReferenceNotFoundException, NessieNamespaceAlreadyExistsException {
    if (namespace.isEmpty()) {
      throw new IllegalArgumentException("Creating empty namespaces is not supported");
    }

    ImmutableNamespace content =
        ImmutableNamespace.builder().from(namespace).properties(properties).build();
    ContentKey key = ContentKey.of(namespace.getElements());

    Map<ContentKey, Content> contentMap;
    try {
      contentMap = api.getContent().refName(refName).hashOnRef(hashOnRef).key(key).get();
    } catch (NessieNotFoundException e) {
      throw new NessieReferenceNotFoundException(e.getMessage(), e);
    }

    if (contentMap.containsKey(key)) {
      if (contentMap.get(key) instanceof Namespace) {
        throw new NessieNamespaceAlreadyExistsException(
            String.format("Namespace '%s' already exists", key.toPathString()));
      } else {
        throw new NessieNamespaceAlreadyExistsException(
            String.format(
                "Another content object with name '%s' already exists", key.toPathString()));
      }
    }

    try {
      Branch branch =
          api.commitMultipleOperations()
              .commitMeta(CommitMeta.fromMessage("create namespace " + namespace.name()))
              .branchName(refName)
              .hash(hashOnRef)
              .operation(Operation.Put.of(key, content))
              .commit();

      contentMap = api.getContent().reference(branch).key(key).get();
    } catch (NessieNotFoundException | NessieConflictException e) {
      throw new NessieReferenceNotFoundException(e.getMessage(), e);
    }

    Optional<Content> result = Optional.ofNullable(contentMap.get(key));
    return result
        .flatMap(r -> r.unwrap(Namespace.class))
        .orElseThrow(
            () ->
                new NessieReferenceNotFoundException(
                    String.format("Namespace '%s' not found", key)));
  }
}
