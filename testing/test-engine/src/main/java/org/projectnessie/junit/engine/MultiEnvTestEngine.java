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
package org.projectnessie.junit.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a JUnit5 Test Engine that delegates test discovery to {@link JupiterTestEngine} and
 * replicates the discovered tests for execution in multiple test environments.
 *
 * <p>Actual test environments are expected to be managed by JUnit 5 extensions, implementing the
 * {@link MultiEnvTestExtension} interface.
 */
public class MultiEnvTestEngine implements TestEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiEnvTestEngine.class);

  public static final String ENGINE_ID = "nessie-multi-env";

  private static final MultiEnvExtensionRegistry registry = new MultiEnvExtensionRegistry();

  private final JupiterTestEngine delegate = new JupiterTestEngine();

  static MultiEnvExtensionRegistry registry() {
    return registry;
  }

  @Override
  public String getId() {
    return ENGINE_ID;
  }

  @Override
  public void execute(ExecutionRequest request) {
    delegate.execute(request);
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    try {
      // JupiterEngineDescriptor must be the root, that's what the JUnit Jupiter engine
      // implementation expects.
      JupiterConfiguration configuration =
          new CachingJupiterConfiguration(
              new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));
      JupiterEngineDescriptor engineDescriptor =
          new JupiterEngineDescriptor(uniqueId, configuration);

      TestDescriptor preliminaryResult = delegate.discover(discoveryRequest, uniqueId);

      // Scan for multi-env test extensions
      preliminaryResult.accept(
          descriptor -> {
            if (descriptor instanceof ClassBasedTestDescriptor) {
              Class<?> testClass = ((ClassBasedTestDescriptor) descriptor).getTestClass();
              registry.registerExtensions(testClass);
            }
          });

      registry.stream()
          .forEach(
              ext -> {
                for (String envId :
                    ext.allEnvironmentIds(discoveryRequest.getConfigurationParameters())) {
                  UniqueId segment = uniqueId.append(ext.segmentType(), envId);

                  MultiEnvTestDescriptor envRoot = new MultiEnvTestDescriptor(segment, envId);
                  engineDescriptor.addChild(envRoot);

                  TestDescriptor discoverResult = delegate.discover(discoveryRequest, segment);
                  List<? extends TestDescriptor> children =
                      new ArrayList<>(discoverResult.getChildren());
                  for (TestDescriptor child : children) {
                    // Note: this also removes the reference to parent from the child
                    discoverResult.removeChild(child);
                    envRoot.addChild(child);
                  }
                }
              });

      return engineDescriptor;
    } catch (Exception e) {
      LOGGER.error("Failed to discover tests", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<String> getGroupId() {
    return Optional.of("org.projectnessie");
  }

  @Override
  public Optional<String> getArtifactId() {
    return Optional.of("nessie-compatibility-tools");
  }
}
