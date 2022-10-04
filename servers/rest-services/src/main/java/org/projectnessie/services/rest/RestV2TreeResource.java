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
package org.projectnessie.services.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.projectnessie.api.http.HttpTreeApi;
import org.projectnessie.api.params.CommitLogParams;
import org.projectnessie.api.params.DiffParams;
import org.projectnessie.api.params.EntriesParams;
import org.projectnessie.api.params.GetReferenceParams;
import org.projectnessie.api.params.ReferencesParams;
import org.projectnessie.apiv1.ContentApi;
import org.projectnessie.apiv1.DiffApi;
import org.projectnessie.apiv1.TreeApi;
import org.projectnessie.apiv1.model.ImmutableMerge;
import org.projectnessie.apiv1.model.ImmutableTransplant;
import org.projectnessie.error.NessieConflictException;
import org.projectnessie.error.NessieNotFoundException;
import org.projectnessie.model.Branch;
import org.projectnessie.model.CommitResponse;
import org.projectnessie.model.Content;
import org.projectnessie.model.ContentKey;
import org.projectnessie.model.ContentResponse;
import org.projectnessie.model.DiffResponse;
import org.projectnessie.model.EntriesResponse;
import org.projectnessie.model.GetMultipleContentsRequest;
import org.projectnessie.model.GetMultipleContentsResponse;
import org.projectnessie.model.LogResponse;
import org.projectnessie.model.Merge;
import org.projectnessie.model.MergeResponse;
import org.projectnessie.model.Operations;
import org.projectnessie.model.Reference;
import org.projectnessie.model.ReferencesResponse;
import org.projectnessie.model.SingleReferenceResponse;
import org.projectnessie.model.Tag;
import org.projectnessie.model.Transplant;
import org.projectnessie.services.authz.Authorizer;
import org.projectnessie.services.config.ServerConfig;
import org.projectnessie.services.impl.ContentApiImplWithAuthorization;
import org.projectnessie.services.impl.DiffApiImplWithAuthorization;
import org.projectnessie.services.impl.TreeApiImplWithAuthorization;
import org.projectnessie.versioned.VersionStore;

/** REST endpoint for the tree-API. */
@RequestScoped
@Path("v2/trees")
public class RestV2TreeResource implements HttpTreeApi {

  private final ServerConfig config;
  private final VersionStore store;
  private final Authorizer authorizer;

  @Context SecurityContext securityContext;

  // Mandated by CDI 2.0
  public RestV2TreeResource() {
    this(null, null, null);
  }

  @Inject
  public RestV2TreeResource(ServerConfig config, VersionStore store, Authorizer authorizer) {
    this.config = config;
    this.store = store;
    this.authorizer = authorizer;
  }

  private TreeApi tree() {
    return new TreeApiImplWithAuthorization(
        config,
        store,
        authorizer,
        securityContext == null ? null : securityContext.getUserPrincipal());
  }

  private DiffApi diff() {
    return new DiffApiImplWithAuthorization(
        config,
        store,
        authorizer,
        securityContext == null ? null : securityContext.getUserPrincipal());
  }

  private ContentApi content() {
    return new ContentApiImplWithAuthorization(
        config,
        store,
        authorizer,
        securityContext == null ? null : securityContext.getUserPrincipal());
  }

  @Override
  public ReferencesResponse getAllReferences(ReferencesParams params) {
    return tree()
        .getAllReferences(
            org.projectnessie.apiv1.params.ReferencesParams.builder()
                .fetchOption(params.fetchOption())
                .filter(params.filter())
                .maxRecords(params.maxRecords())
                .pageToken(params.pageToken())
                .build());
  }

  @Override
  public SingleReferenceResponse createReference(
      String name, Reference.ReferenceType type, Reference reference)
      throws NessieNotFoundException, NessieConflictException {
    String fromRefName = null;
    String fromHash = null;
    if (reference != null) {
      fromRefName = reference.getName();
      fromHash = reference.getHash();
    }

    Reference toCreate;
    switch (type) {
      case BRANCH:
        toCreate = Branch.of(name, fromHash);
        break;
      case TAG:
        toCreate = Tag.of(name, fromHash);
        break;
      default:
        throw new IllegalArgumentException("Unsupported reference type: " + type);
    }

    Reference created = tree().createReference(fromRefName, toCreate);
    return SingleReferenceResponse.builder().reference(created).build();
  }

  @Override
  public SingleReferenceResponse getReferenceByName(GetReferenceParams params)
      throws NessieNotFoundException {
    return SingleReferenceResponse.builder()
        .reference(
            tree()
                .getReferenceByName(
                    org.projectnessie.apiv1.params.GetReferenceParams.builder()
                        .fetchOption(params.fetchOption())
                        .refName(params.getRefName())
                        .build()))
        .build();
  }

  @Override
  public EntriesResponse getEntries(String ref, EntriesParams params)
      throws NessieNotFoundException {
    Reference reference = Reference.fromPathString(ref, Reference.ReferenceType.BRANCH);
    return tree()
        .getEntries(
            reference.getName(),
            org.projectnessie.apiv1.params.EntriesParams.builder()
                .hashOnRef(reference.getHash())
                .maxRecords(params.maxRecords())
                .filter(params.filter())
                .pageToken(params.pageToken())
                .build());
  }

  @Override
  public LogResponse getCommitLog(String ref, CommitLogParams params)
      throws NessieNotFoundException {
    Reference reference = Reference.fromPathString(ref, Reference.ReferenceType.BRANCH);
    return tree()
        .getCommitLog(
            reference.getName(),
            org.projectnessie.apiv1.params.CommitLogParams.builder()
                .endHash(reference.getHash())
                .startHash(params.startHash())
                .fetchOption(params.fetchOption())
                .maxRecords(params.maxRecords())
                .pageToken(params.pageToken())
                .filter(params.filter())
                .build());
  }

  @Override
  public DiffResponse getDiff(DiffParams params) throws NessieNotFoundException {
    Reference from = Reference.fromPathString(params.getFromRef(), Reference.ReferenceType.BRANCH);
    Reference to = Reference.fromPathString(params.getToRef(), Reference.ReferenceType.BRANCH);
    return diff()
        .getDiff(
            org.projectnessie.apiv1.params.DiffParams.builder()
                .fromRef(from.getName())
                .fromHashOnRef(from.getHash())
                .toRef(to.getName())
                .toHashOnRef(to.getHash())
                .build());
  }

  @Override
  public SingleReferenceResponse assignReference(
      Reference.ReferenceType type, String ref, Reference assignTo)
      throws NessieNotFoundException, NessieConflictException {
    Reference reference = Reference.fromPathString(ref, type);
    tree().assignReference(type, reference.getName(), reference.getHash(), assignTo);
    return SingleReferenceResponse.builder().reference(reference).build();
  }

  @Override
  public SingleReferenceResponse deleteReference(Reference.ReferenceType type, String ref)
      throws NessieConflictException, NessieNotFoundException {
    Reference reference = Reference.fromPathString(ref, type);
    tree().deleteReference(type, reference.getName(), reference.getHash());
    return SingleReferenceResponse.builder().reference(reference).build();
  }

  @Override
  public ContentResponse getContent(ContentKey key, String ref) throws NessieNotFoundException {
    Reference reference = Reference.fromPathString(ref, Reference.ReferenceType.BRANCH);
    Content content = content().getContent(key, reference.getName(), reference.getHash());
    return ContentResponse.builder().content(content).build();
  }

  @Override
  public GetMultipleContentsResponse getMultipleContents(
      String ref, GetMultipleContentsRequest request) throws NessieNotFoundException {
    Reference reference = Reference.fromPathString(ref, Reference.ReferenceType.BRANCH);
    return content().getMultipleContents(reference.getName(), reference.getHash(), request);
  }

  @Override
  public MergeResponse transplantCommitsIntoBranch(String branch, Transplant transplant)
      throws NessieNotFoundException, NessieConflictException {
    Reference ref = Reference.fromPathString(branch, Reference.ReferenceType.BRANCH);
    return tree()
        .transplantCommitsIntoBranch(
            ref.getName(),
            ref.getHash(),
            transplant.getMessage(),
            ImmutableTransplant.builder()
                .fromRefName(transplant.getFromRefName())
                .hashesToTransplant(transplant.getHashesToTransplant())
                .keepIndividualCommits(true)
                .isDryRun(transplant.isDryRun())
                .isFetchAdditionalInfo(transplant.isFetchAdditionalInfo())
                .isReturnConflictAsResult(transplant.isReturnConflictAsResult())
                .build());
  }

  @Override
  public MergeResponse mergeRefIntoBranch(String branch, Merge merge)
      throws NessieNotFoundException, NessieConflictException {
    Reference ref = Reference.fromPathString(branch, Reference.ReferenceType.BRANCH);
    return tree()
        .mergeRefIntoBranch(
            ref.getName(),
            ref.getHash(),
            ImmutableMerge.builder()
                .fromHash(merge.getFromHash())
                .fromRefName(merge.getFromRefName())
                .keepIndividualCommits(false)
                .isReturnConflictAsResult(merge.isReturnConflictAsResult())
                .isFetchAdditionalInfo(merge.isFetchAdditionalInfo())
                .isDryRun(merge.isDryRun())
                .build());
  }

  @Override
  public CommitResponse commitMultipleOperations(String branch, Operations operations)
      throws NessieNotFoundException, NessieConflictException {
    Reference ref = Reference.fromPathString(branch, Reference.ReferenceType.BRANCH);
    Branch head = tree().commitMultipleOperations(ref.getName(), ref.getHash(), operations);
    return CommitResponse.builder().targetBranch(head).build();
  }
}
