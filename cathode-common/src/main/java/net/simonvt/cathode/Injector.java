/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode;

import dagger.ObjectGraph;

public final class Injector {

  private Injector() {
  }

  private static ObjectGraph objectGraph;

  public static void install(ObjectGraph graph) {
    objectGraph = graph;
  }

  public static <T> T inject(T target) {
    if (objectGraph == null) {
      throw new RuntimeException("ObjectGraph not yet initialized");
    }

    return objectGraph.inject(target);
  }
}
