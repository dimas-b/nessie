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
package org.projectnessie.server.catalog.auth;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.rest.RESTCatalog;
import org.apache.iceberg.rest.auth.OAuth2Properties;

@QuarkusIntegrationTest
@TestProfile(ITOAuthProxyIcebergCatalog.Profile.class)
public class ITOAuthProxyIcebergCatalog extends AbstractAuthEnabledTests {

  @Override
  protected RESTCatalog catalog() {
    int catalogServerPort = Integer.getInteger("quarkus.http.port");
    RESTCatalog catalog = new RESTCatalog();
    catalog.setConf(new Configuration());
    catalog.initialize(
        "nessie-auth-iceberg-api",
        Map.of(
            CatalogProperties.URI,
            String.format("http://127.0.0.1:%d/iceberg/", catalogServerPort),
            OAuth2Properties.SCOPE,
            "email",
            OAuth2Properties.CREDENTIAL,
            clientId + ":" + clientSecret));
    catalogs.add(catalog);
    return catalog;
  }

  public static class Profile extends AbstractAuthEnabledTests.Profile {

    @Override
    public Map<String, String> getConfigOverrides() {
      Map<String, String> config = new HashMap<>(super.getConfigOverrides());
      config.put("nessie.catalog.default-warehouse.allow-auth-proxy", "true");
      return config;
    }
  }
}
