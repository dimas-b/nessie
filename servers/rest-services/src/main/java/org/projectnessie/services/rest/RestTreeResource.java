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
package org.projectnessie.services.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.projectnessie.apiv1.TreeApi;
import org.projectnessie.apiv1.http.HttpTreeApi;
import org.projectnessie.apiv1.model.Merge;
import org.projectnessie.apiv1.model.Transplant;
import org.projectnessie.apiv1.params.CommitLogParams;
import org.projectnessie.apiv1.params.EntriesParams;
import org.projectnessie.apiv1.params.GetReferenceParams;
import org.projectnessie.apiv1.params.ReferencesParams;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.EntriesResponse;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Operations;
import org.projectnessie.model.Reference;
import org.projectnessie.model.ReferencesResponse;
import org.projectnessie.services.authz.Authorizer;
import org.projectnessie.services.config.ServerConfig;
import org.projectnessie.services.impl.TreeApiImplWithAuthorization;
import org.projectnessie.versioned.VersionStore;

/** REST endpoint for the tree-API. */
@RequestScoped
@Path("v1/trees")
public class RestTreeResource implements HttpTreeApi {
  // Cannot extend the TreeApiImplWithAuthz class, because then CDI gets confused
  // about which interface to use - either HttpTreeApi or the plain TreeApi. This can lead
  // to various symptoms: complaints about varying validation-constraints in HttpTreeApi + TreeAPi,
  // empty resources (no REST methods defined) and potentially other.

  private final ServerConfig config;
  private final VersionStore store;
  private final Authorizer authorizer;

  @Context SecurityContext securityContext;

  // Mandated by CDI 2.0
  public RestTreeResource() {
    this(null, null, null);
  }

  @Inject
  public RestTreeResource(ServerConfig config, VersionStore store, Authorizer authorizer) {
    this.config = config;
    this.store = store;
    this.authorizer = authorizer;
  }

  private TreeApi resource() {
    return new TreeApiImplWithAuthorization(
        config,
        store,
        authorizer,
        securityContext == null ? null : securityContext.getUserPrincipal());
  }

  @Override
  public ReferencesResponse getAllReferences(ReferencesParams params) {
    return resource().getAllReferences(params);
  }

  @Override
  public Branch getDefaultBranch() throws NessieNotFoundException {
    return resource().getDefaultBranch();
  }

  @Override
  public Reference createReference(String sourceRefName, Reference reference)
      throws NessieNotFoundException, NessieConflictException {
    return resource().createReference(sourceRefName, reference);
  }

  @Override
  public Reference getReferenceByName(GetReferenceParams params) throws NessieNotFoundException {
    return resource().getReferenceByName(params);
  }

  @Override
  public EntriesResponse getEntries(String refName, EntriesParams params)
      throws NessieNotFoundException {
    return resource().getEntries(refName, params);
  }

  @Override
  public LogResponse getCommitLog(String ref, CommitLogParams params)
      throws NessieNotFoundException {
    return resource().getCommitLog(ref, params);
  }

  @Override
  public void assignReference(
      Reference.ReferenceType referenceType,
      String referenceName,
      String expectedHash,
      Reference assignTo)
      throws NessieNotFoundException, NessieConflictException {
    resource().assignReference(referenceType, referenceName, expectedHash, assignTo);
  }

  @Override
  public void deleteReference(
      Reference.ReferenceType referenceType, String referenceName, String expectedHash)
      throws NessieConflictException, NessieNotFoundException {
    resource().deleteReference(referenceType, referenceName, expectedHash);
  }

  @Override
  public MergeResponse transplantCommitsIntoBranch(
      String branchName, String expectedHash, String message, Transplant transplant)
      throws NessieNotFoundException, NessieConflictException {
    return resource().transplantCommitsIntoBranch(branchName, expectedHash, message, transplant);
  }

  @Override
  public MergeResponse mergeRefIntoBranch(String branchName, String expectedHash, Merge merge)
      throws NessieNotFoundException, NessieConflictException {
    return resource().mergeRefIntoBranch(branchName, expectedHash, merge);
  }

  @Override
  public Branch commitMultipleOperations(
      String branchName, String expectedHash, Operations operations)
      throws NessieNotFoundException, NessieConflictException {
    return resource().commitMultipleOperations(branchName, expectedHash, operations);
  }
}
