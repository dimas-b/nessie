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
package org.projectnessie.versioned.persist.adapter;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.projectnessie.versioned.ContentAttachment;
import org.projectnessie.versioned.Key;

/** API helper method to encapsulate parameters for {@link DatabaseAdapter#commit(CommitParams)}. */
@Value.Immutable
public interface CommitParams extends ToBranchParams {

  /**
   * Mapping of content-ids to expected global content-state (think: Iceberg table-metadata), coming
   * from the "expected-state" property of a {@code PutGlobal} commit operation.
   */
  Map<ContentId, Optional<ByteString>> getExpectedStates();

  /**
   * List of all {@code Put} operations, with their keys, content-types and serialized {@code
   * Content}.
   */
  List<KeyWithBytes> getPuts();

  /** The content attachments for the put operations. */
  List<ContentAttachment> getAttachments();

  /** List of "unchanged" keys, from {@code Unchanged} commit operations. */
  List<Key> getUnchanged();

  /** List of "unchanged" keys, from {@code Delete} commit operations. */
  List<Key> getDeletes();

  /** Serialized commit-metadata. */
  ByteString getCommitMetaSerialized();

  @Nullable
  @jakarta.annotation.Nullable
  Callable<Void> getValidator();
}