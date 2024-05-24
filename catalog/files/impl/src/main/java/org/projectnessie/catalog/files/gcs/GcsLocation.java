/*
 * Copyright (C) 2024 Dremio
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
package org.projectnessie.catalog.files.gcs;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.net.URI;

public final class GcsLocation {
  private final String bucket;
  private final String path;

  private GcsLocation(String bucket, String path) {
    this.bucket = requireNonNull(bucket);
    this.path = requireNonNull(path);
  }

  public static GcsLocation gcsLocation(String bucket, String path) {
    return new GcsLocation(bucket, path);
  }

  public static GcsLocation gcsLocation(URI location) {
    checkArgument(location != null, "Invalid location: null");
    String scheme = location.getScheme();
    checkArgument("gs".equals(scheme), "Invalid GCS scheme: %s", location);

    String bucket = location.getAuthority();

    String path = location.getPath();
    path = path == null ? "" : path.startsWith("/") ? path.substring(1) : path;

    return new GcsLocation(bucket, path);
  }

  public String bucket() {
    return bucket;
  }

  public String path() {
    return path;
  }
}
