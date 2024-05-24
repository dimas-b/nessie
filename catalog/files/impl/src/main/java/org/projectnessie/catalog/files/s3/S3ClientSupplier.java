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
package org.projectnessie.catalog.files.s3;

import static com.google.common.base.Preconditions.checkArgument;
import static org.projectnessie.catalog.files.s3.S3Clients.awsCredentialsProvider;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import org.projectnessie.catalog.files.secrets.SecretsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFile.Type;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.DelegatingS3Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.model.S3Request.Builder;

public class S3ClientSupplier {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientSupplier.class);
  private static final ProfileFile EMPTY_PROFILE_FILE =
      ProfileFile.builder().content(InputStream.nullInputStream()).type(Type.CONFIGURATION).build();

  private final SdkHttpClient sdkClient;
  private final S3Config s3config;
  private final S3Options<?> s3options;
  private final SecretsProvider secretsProvider;
  private final S3Sessions sessions;

  public S3ClientSupplier(
      SdkHttpClient sdkClient,
      S3Config s3config,
      S3Options<?> s3options,
      SecretsProvider secretsProvider,
      S3Sessions sessions) {
    this.sdkClient = sdkClient;
    this.s3config = s3config;
    this.s3options = s3options;
    this.secretsProvider = secretsProvider;
    this.sessions = sessions;
  }

  public S3Config s3config() {
    return s3config;
  }

  public S3Options<?> s3options() {
    return s3options;
  }

  /**
   * Produces an S3 client for the set of S3 options and secrets. S3 options are retrieved from the
   * per-bucket config, which derives from the global config. References to the secrets that contain
   * the actual S3 access-key-ID and secret-access-key are present in the S3 options as well.
   */
  public S3Client getClient(URI location) {

    String scheme = location.getScheme();
    checkArgument("s3".equals(scheme), "Invalid S3 scheme: %s", location);
    String bucketName = location.getAuthority();

    // Supply an empty profile file

    S3BucketOptions bucketOptions = s3options.effectiveOptionsForBucket(Optional.of(bucketName));

    S3ClientBuilder builder =
        S3Client.builder()
            .httpClient(sdkClient)
            .credentialsProvider(awsCredentialsProvider(bucketOptions, secretsProvider, sessions))
            .overrideConfiguration(
                override -> override.defaultProfileFileSupplier(() -> EMPTY_PROFILE_FILE))
            .serviceConfiguration(
                serviceConfig -> serviceConfig.profileFile(() -> EMPTY_PROFILE_FILE));

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "Building S3-client for bucket {} using endpoint {} with {}",
          bucketName,
          bucketOptions.endpoint(),
          toLogString(bucketOptions));
    }

    bucketOptions.endpoint().ifPresent(builder::endpointOverride);
    bucketOptions.region().map(Region::of).ifPresent(builder::region);
    bucketOptions.pathStyleAccess().ifPresent(builder::forcePathStyle);
    bucketOptions
        .allowCrossRegionAccessPoint()
        .ifPresent(cr -> builder.disableMultiRegionAccessPoints(!cr));

    // https://cloud.google.com/storage/docs/aws-simple-migration#project-header
    bucketOptions
        .projectId()
        .ifPresent(
            prj ->
                builder.overrideConfiguration(
                    override -> override.putHeader("x-amz-project-id", prj)));

    S3Client s3Client = builder.build();

    if (bucketOptions.accessPoint().isPresent()) {
      String accessPoint = bucketOptions.accessPoint().get();
      s3Client = new AccessPointAwareS3Client(s3Client, accessPoint);
    }

    return s3Client;
  }

  private static String toLogString(S3BucketOptions options) {
    return "S3BucketOptions{"
        + "cloud="
        + options.cloud().map(Cloud::name).orElse("<undefined>")
        + ", endpoint="
        + options.endpoint().map(URI::toString).orElse("<undefined>")
        + ", region="
        + options.region().orElse("<undefined>")
        + ", projectId="
        + options.projectId().orElse("<undefined>")
        + ", accessKeyIdRef="
        + options.accessKeyIdRef().orElse("<undefined>")
        + ", secretAccessKeyRef="
        + options.secretAccessKeyRef().orElse("<undefined>")
        + "}";
  }

  private static class AccessPointAwareS3Client extends DelegatingS3Client {

    private final String accessPoint;

    public AccessPointAwareS3Client(S3Client s3Client, String accessPoint) {
      super(s3Client);
      this.accessPoint = accessPoint;
    }

    @Override
    protected <T extends S3Request, ReturnT> ReturnT invokeOperation(
        T request, Function<T, ReturnT> operation) {
      Optional<SdkField<?>> bucket =
          request.sdkFields().stream()
              .filter(f -> f.memberName().equalsIgnoreCase("Bucket"))
              .findFirst();
      if (bucket.isPresent()) {
        Builder builder = request.toBuilder();
        bucket.get().set(builder, accessPoint);
        @SuppressWarnings("unchecked")
        T modified = (T) builder.build();
        return super.invokeOperation(modified, operation);
      }
      return super.invokeOperation(request, operation);
    }
  }
}
