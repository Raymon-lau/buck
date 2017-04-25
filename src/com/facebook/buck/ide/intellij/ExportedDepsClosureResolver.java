/*
 * Copyright 2015-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.ide.intellij;

import com.facebook.buck.android.AndroidLibraryDescription;
import com.facebook.buck.jvm.java.JavaLibraryDescription;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.rules.TargetNode;
import com.facebook.buck.util.MoreCollectors;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Calculates the transitive closure of exported deps for every node in a {@link TargetGraph}. */
public class ExportedDepsClosureResolver {

  private TargetGraph targetGraph;
  private Map<BuildTarget, ImmutableSet<BuildTarget>> index;

  public ExportedDepsClosureResolver(TargetGraph targetGraph) {
    this.targetGraph = targetGraph;
    index = new HashMap<>();
  }

  /**
   * @param buildTarget target to process.
   * @return the set of {@link BuildTarget}s that must be appended to the dependencies of a node Y
   *     if node Y depends on X.
   */
  public ImmutableSet<BuildTarget> getExportedDepsClosure(BuildTarget buildTarget) {
    if (index.containsKey(buildTarget)) {
      return index.get(buildTarget);
    }

    ImmutableSet<BuildTarget> exportedDeps = ImmutableSet.of();
    TargetNode<?, ?> targetNode = targetGraph.get(buildTarget);
    if (targetNode.getDescription() instanceof JavaLibraryDescription) {
      JavaLibraryDescription.Arg arg = (JavaLibraryDescription.Arg) targetNode.getConstructorArg();
      exportedDeps = arg.exportedDeps;
    } else if (targetNode.getDescription() instanceof AndroidLibraryDescription) {
      AndroidLibraryDescription.Arg arg =
          (AndroidLibraryDescription.Arg) targetNode.getConstructorArg();
      exportedDeps = arg.exportedDeps;
    }

    ImmutableSet<BuildTarget> exportedDepsClosure =
        exportedDeps
            .stream()
            .flatMap(
                target -> Stream.concat(Stream.of(target), getExportedDepsClosure(target).stream()))
            .collect(MoreCollectors.toImmutableSet());

    index.put(buildTarget, exportedDepsClosure);
    return exportedDepsClosure;
  }
}
