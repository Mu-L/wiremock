/*
 * Copyright (C) 2024-2025 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.http;

import static java.util.stream.Collectors.toList;

import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.Extensions;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public class HttpServerFactoryLoader {

  private final Options options;
  private final Extensions extensions;
  private final Supplier<List<HttpServerFactory>> serviceLoader;

  public HttpServerFactoryLoader(
      Options options, Extensions extensions, Supplier<List<HttpServerFactory>> serviceLoader) {
    this.options = options;
    this.extensions = extensions;
    this.serviceLoader = serviceLoader;
  }

  public HttpServerFactory load() {
    if (options.httpServerFactory() != null) {
      return options.httpServerFactory();
    }

    final List<HttpServerFactory> extensionCandidates =
        extensions.ofType(HttpServerFactory.class).values().stream().toList();
    if (!extensionCandidates.isEmpty()) {
      return pickMostAppropriateFrom(extensionCandidates);
    }

    final List<HttpServerFactory> serviceLoadedCandidates = serviceLoader.get();
    return pickMostAppropriateFrom(serviceLoadedCandidates);
  }

  public static Supplier<List<HttpServerFactory>> systemServiceLoader() {
    return () ->
        ServiceLoader.load(Extension.class).stream()
            .filter(extension -> HttpServerFactory.class.isAssignableFrom(extension.type()))
            .map(e -> (HttpServerFactory) e.get())
            .collect(toList());
  }

  private static HttpServerFactory pickMostAppropriateFrom(List<HttpServerFactory> candidates) {
    if (candidates.isEmpty()) {
      throw couldNotFindSuitableServerException();
    }

    return candidates.size() > 1
        ? candidates.stream()
            .filter(factory -> !DefaultFactory.class.isAssignableFrom(factory.getClass()))
            .findFirst()
            .orElseThrow(HttpServerFactoryLoader::couldNotFindSuitableServerException)
        : candidates.get(0);
  }

  private static FatalStartupException couldNotFindSuitableServerException() {
    return new FatalStartupException(
        "No suitable HttpServerFactory was found. Please ensure that the classpath includes a WireMock extension that provides an HttpServerFactory implementation. See https://wiremock.org/docs/extending-wiremock/ for more information.");
  }
}
