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
package org.projectnessie.catalog.service.rest;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Clock.systemUTC;
import static org.projectnessie.catalog.files.api.StorageLocations.storageLocations;
import static org.projectnessie.catalog.files.s3.S3Utils.normalizeS3Scheme;
import static org.projectnessie.catalog.service.rest.AccessDelegation.REMOTE_SIGNING;
import static org.projectnessie.catalog.service.rest.AccessDelegation.VENDED_CREDENTIALS;
import static org.projectnessie.catalog.service.rest.AccessDelegation.accessDelegationPredicate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.projectnessie.catalog.files.api.ObjectIO;
import org.projectnessie.catalog.files.api.StorageLocations;
import org.projectnessie.catalog.files.config.S3BucketOptions;
import org.projectnessie.catalog.files.s3.S3Utils;
import org.projectnessie.catalog.formats.iceberg.meta.IcebergTableMetadata;
import org.projectnessie.catalog.model.snapshot.NessieEntitySnapshot;
import org.projectnessie.catalog.service.api.SignerKeysService;
import org.projectnessie.catalog.service.config.LakehouseConfig;
import org.projectnessie.catalog.service.config.WarehouseConfig;
import org.projectnessie.catalog.service.objtypes.SignerKey;
import org.projectnessie.model.ContentKey;
import org.projectnessie.services.config.ServerConfig;
import org.projectnessie.storage.uri.StorageUri;

@SuppressWarnings("CdiInjectionPointsInspection")
@RequestScoped
public class IcebergConfigurer {

  static final String ICEBERG_WAREHOUSE_LOCATION = "warehouse";
  static final String ICEBERG_PREFIX = "prefix";

  static final String METRICS_REPORTING_ENABLED = "rest-metrics-reporting-enabled";

  /** Base URI of the signer endpoint, defaults to {@code uri}. */
  static final String S3_SIGNER_URI = "s3.signer.uri";

  /** Path of the signer endpoint. */
  static final String S3_SIGNER_ENDPOINT = "s3.signer.endpoint";

  @Inject ServerConfig serverConfig;
  @Inject LakehouseConfig lakehouseConfig;
  @Inject ObjectIO objectIO;
  @Inject SignerKeysService signerKeysService;

  @Inject
  @ConfigProperty(name = "nessie.server.authentication.enabled")
  boolean authnEnabled;

  @Context ExternalBaseUri uriInfo;

  public Response trinoConfig(String reference, String warehouse, String format) {

    WarehouseConfig warehouseConfig = lakehouseConfig.catalog().getWarehouse(warehouse);

    StorageUri location = StorageUri.of(warehouseConfig.location());

    Map<String, String> config = new HashMap<>();
    icebergWarehouseConfig(reference, warehouse, (k, v) -> {}, config::put);
    objectIO.configureIcebergWarehouse(
        StorageUri.of(warehouseConfig.location()), config::put, config::put);

    Properties properties = new Properties();

    properties.put("connector.name", "iceberg");
    properties.put("iceberg.catalog.type", "rest");
    properties.put("iceberg.rest-catalog.uri", uriInfo.icebergBaseURI().toString());

    properties.put("iceberg.rest-catalog.security", authnEnabled ? "OAUTH2" : "NONE");
    if (authnEnabled) {
      properties.put(
          "iceberg.rest-catalog.oauth2.token", "fill-in-your-oauth-token or use .credential");
      properties.put(
          "iceberg.rest-catalog.oauth2.credential", "fill-in-your-oauth-credentials or use .token");
    }

    objectIO.trinoSampleConfig(location, config, properties::put);

    List<String> header =
        List.of(
            "Example Trino starter configuration properties for warehouse " + location,
            "generated by Nessie to be placed for example in",
            "/etc/trino/catalogs/nessie.properties within a Trino container/pod when.",
            "using Trino 'static' configurations.",
            "",
            "This starter configuration must be inspected and verified to validate that",
            "all options and values match your specific needs and no mandatory options",
            "are missing or superfluous options are present.",
            "",
            "When using OAuth2, you have to supply the 'iceberg.rest-catalog.oauth2.token'",
            "configuration.",
            "",
            "WARNING! Trino lacks functionality to configure the oauth endpoint and is therefore",
            "unable to work with any Iceberg REST catalog implementation and demands a standard",
            "OAuth2 server like Keycloak or Authelia. If you feel you need client-ID/secret flow,",
            "please report an issue against Trino.",
            "",
            "No guarantees that this configuration works for your specific needs.",
            "Use at your own risk!",
            "Do not distribute the contents as those may contain sensitive information!");
    format = format == null ? "properties" : format.toLowerCase(Locale.ROOT).trim();
    switch (format) {
      case "sql":
      case "ddl":
      case "dynamic":
        String sql =
            properties.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .map(e -> format("    \"%s\" = '%s'", e.getKey(), e.getValue()))
                .collect(
                    Collectors.joining(
                        ",\n", "CREATE CATALOG nessie\n  USING iceberg\n  WITH (\n", "\n  );\n"));

        return Response.ok(
                header.stream().collect(Collectors.joining("\n * ", "/*\n * ", "\n */\n")) + sql,
                "application/sql")
            .header("Content-Disposition", "attachment; filename=\"create-catalog-nessie.sql\"")
            .build();
      case "properties":
      case "static":
      default:
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
          try {
            pw.println("#\n# " + String.join("\n# ", header) + "\n#\n");
            properties.store(pw, "");
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        return Response.ok(sw.toString(), "text/plain")
            .header("Content-Disposition", "attachment; filename=\"nessie.properties\"")
            .build();
    }
  }

  public void icebergWarehouseConfig(
      String reference,
      String warehouse,
      BiConsumer<String, String> configDefault,
      BiConsumer<String, String> configOverride) {
    boolean hasWarehouse = warehouse != null && !warehouse.isEmpty();
    WarehouseConfig warehouseConfig = lakehouseConfig.catalog().getWarehouse(warehouse);

    // defaults

    String branch = defaultBranchName(reference);
    // Not fully implemented yet
    configDefault.accept(METRICS_REPORTING_ENABLED, "false");
    configDefault.accept(ICEBERG_WAREHOUSE_LOCATION, warehouseConfig.location());
    uriInfo.icebergConfigDefaults(configDefault);
    // allow users to override the 'rest-page-size' in the Nessie configuration
    configDefault.accept("rest-page-size", "200");
    lakehouseConfig.catalog().icebergConfigDefaults().forEach(configDefault);
    warehouseConfig.icebergConfigDefaults().forEach(configDefault);
    // Set the "default" prefix
    if (!hasWarehouse && lakehouseConfig.catalog().defaultWarehouse().isPresent()) {
      configDefault.accept(ICEBERG_PREFIX, encode(branch, UTF_8));
    } else {
      configDefault.accept(
          ICEBERG_PREFIX,
          encode(branch + "|" + lakehouseConfig.catalog().resolveWarehouseName(warehouse), UTF_8));
    }

    // overrides
    uriInfo.icebergConfigOverrides(configOverride);
    lakehouseConfig.catalog().icebergConfigOverrides().forEach(configOverride);
    warehouseConfig.icebergConfigOverrides().forEach(configOverride);
    // Marker property telling clients that the backend is a Nessie Catalog.
    configOverride.accept("nessie.is-nessie-catalog", "true");
    // 'prefix-pattern' is just for information at the moment...
    configOverride.accept("nessie.prefix-pattern", "{ref}|{warehouse}");
    // The following properties are passed back to clients to automatically configure their Nessie
    // client. These properties are _not_ user configurable properties.
    configOverride.accept("nessie.default-branch.name", branch);
  }

  IcebergTableConfig icebergConfigPerTable(
      NessieEntitySnapshot<?> nessieSnapshot,
      String warehouseLocation,
      IcebergTableMetadata tableMetadata,
      String prefix,
      ContentKey contentKey,
      String dataAccess,
      boolean writeAccessGranted) {
    ImmutableIcebergTableConfig.Builder tableConfig = ImmutableIcebergTableConfig.builder();

    Set<StorageUri> writeable = new HashSet<>();
    Set<StorageUri> readOnly = new HashSet<>();
    Set<StorageUri> maybeWriteable = writeAccessGranted ? writeable : readOnly;
    StorageUri locationUri = StorageUri.of(tableMetadata.location());
    (tableMetadata.location().startsWith(warehouseLocation) ? maybeWriteable : readOnly)
        .add(locationUri);

    if (!icebergWriteObjectStorage(tableConfig, tableMetadata.properties(), warehouseLocation)) {
      String writeLocation = icebergWriteLocation(tableMetadata.properties());
      if (writeLocation != null && !writeLocation.startsWith(tableMetadata.location())) {
        (writeLocation.startsWith(warehouseLocation) ? maybeWriteable : readOnly)
            .add(StorageUri.of(writeLocation));
      }
    }

    for (String additionalKnownLocation : nessieSnapshot.additionalKnownLocations()) {
      StorageUri old = StorageUri.of(additionalKnownLocation);
      if (!writeable.contains(old)) {
        readOnly.add(old);
      }
    }

    StorageLocations locations =
        storageLocations(StorageUri.of(warehouseLocation), writeable, readOnly);

    Predicate<AccessDelegation> accessDelegationPredicate = accessDelegationPredicate(dataAccess);

    Map<String, String> config = new HashMap<>();

    objectIO.configureIcebergTable(
        locations,
        config::put,
        signUrlExpiration ->
            configureS3RequestSigningForTable(
                signUrlExpiration,
                locations,
                accessDelegationPredicate,
                prefix,
                contentKey,
                config::put),
        accessDelegationPredicate.test(VENDED_CREDENTIALS));

    return tableConfig.config(config).build();
  }

  /**
   * Handle S3 request signing "specialties" here. This function is called only if the S3 bucket has
   * {@linkplain S3BucketOptions#effectiveRequestSigningEnabled() request signing enabled} and
   * returns whether request signing is possible and has been enabled.
   *
   * <p>Parameters that are needed to configure S3 request signing are specific to the current table
   * and need URI related information from the current REST/HTTP request and the S3 signer service.
   * Having this functionality and especially the dependencies in leak through {@link ObjectIO} is
   * not worth the trouble.
   */
  private boolean configureS3RequestSigningForTable(
      Duration signUrlExpiration,
      StorageLocations locations,
      Predicate<AccessDelegation> accessDelegationPredicate,
      String prefix,
      ContentKey contentKey,
      BiConsumer<String, String> config) {
    if (!accessDelegationPredicate.test(REMOTE_SIGNING)) {
      return false;
    }
    if (!Stream.concat(
            locations.writeableLocations().stream(), locations.readonlyLocations().stream())
        .map(StorageUri::scheme)
        .allMatch(S3Utils::isS3scheme)) {
      return false;
    }

    // Handling for S3 signing is very much integrated w/ request-URI/context, so that
    // functionality stays here and is not handled in S3ObjectIO.

    String normalizedWarehouseLocation =
        normalizeS3Scheme(locations.warehouseLocation().toString());

    // Must use both 's3.signer.uri' and 's3.signer.endpoint', because Iceberg before 1.5.0
    // does not handle full URIs passed via 's3.signer.endpoint'. This was changed via
    // https://github.com/apache/iceberg/pull/8976/files#diff-1f7498b6989fffc169f7791292ed2ccb35b305f6a547fd832f6724057c8aca8bR213-R216,
    // first released in Iceberg 1.5.0. It's unclear how other language implementations deal
    // with this.
    config.accept(S3_SIGNER_URI, uriInfo.icebergBaseURI().toString());

    List<String> normalizedWriteLocations = new ArrayList<>();
    List<String> normalizedReadLocations = new ArrayList<>();
    for (StorageUri loc : locations.writeableLocations()) {
      String locStr = normalizeS3Scheme(loc.toString());
      if (locStr.startsWith(normalizedWarehouseLocation)) {
        normalizedWriteLocations.add(locStr);
      }
    }
    for (StorageUri loc : locations.readonlyLocations()) {
      String locStr = normalizeS3Scheme(loc.toString());
      normalizedReadLocations.add(locStr);
    }

    SignerKey signerKey = signerKeysService.currentSignerKey();

    long expirationTimestamp = systemUTC().instant().plus(signUrlExpiration).getEpochSecond();

    String contentKeyPathString = contentKey.toPathStringEscaped();
    String pathParam =
        SignerSignature.builder()
            .expirationTimestamp(expirationTimestamp)
            .prefix(prefix)
            .identifier(contentKeyPathString)
            .warehouseLocation(normalizedWarehouseLocation)
            .writeLocations(normalizedWriteLocations)
            .readLocations(normalizedReadLocations)
            .build()
            .toPathParam(signerKey);

    config.accept(S3_SIGNER_ENDPOINT, uriInfo.icebergS3SignerPathWithPath(prefix, pathParam));

    return true;
  }

  static boolean icebergWriteObjectStorage(
      ImmutableIcebergTableConfig.Builder config,
      Map<String, String> metadataProperties,
      String bucketLocation) {
    if (!Boolean.parseBoolean(
        metadataProperties.getOrDefault("write.object-storage.enabled", "false"))) {
      return false;
    }

    Map<String, String> updated = new HashMap<>(metadataProperties);

    updated.put("write.data.path", bucketLocation);
    updated.remove("write.object-storage.path");
    updated.remove("write.folder-storage.path");

    config.updatedMetadataProperties(updated);

    return true;
  }

  static String icebergWriteLocation(Map<String, String> properties) {
    String dataLocation = properties.get("write.data.path");
    if (dataLocation == null) {
      dataLocation = properties.get("write.object-storage.path");
      if (dataLocation == null) {
        dataLocation = properties.get("write.folder-storage.path");
      }
    }
    return dataLocation;
  }

  private String defaultBranchName(String reference) {
    String branch = reference;
    if (branch == null) {
      branch = serverConfig.getDefaultBranch();
    }
    if (branch == null) {
      branch = "main";
    }
    return branch;
  }
}
