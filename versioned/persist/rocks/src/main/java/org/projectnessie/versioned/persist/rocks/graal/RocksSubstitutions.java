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
package org.projectnessie.versioned.persist.rocks.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

// Inspired by
// https://github.com/quarkusio/quarkus/blob/main/extensions/kafka-streams/runtime/src/main/java/io/quarkus/kafka/streams/runtime/graal/KafkaStreamsSubstitutions.java

/**
 * Resets the {@code initialized} field, so that the native libs are loaded again at image runtime,
 * after they have been loaded once at build time via calls from static initializers.
 */
@TargetClass(className = "org.rocksdb.NativeLibraryLoader")
@SuppressWarnings({"checkstyle:TypeName", "checkstyle:OneTopLevelClass", "unused"})
final class Target_org_rocksdb_NativeLibraryLoader {

  @SuppressWarnings("FieldMayBeFinal")
  @Alias
  @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
  private static boolean initialized = false;
}

public final class RocksSubstitutions {}