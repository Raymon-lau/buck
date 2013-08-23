/*
 * Copyright 2013-present Facebook, Inc.
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

package com.facebook.buck.android;

import static org.junit.Assert.assertEquals;

import com.facebook.buck.graph.MutableDirectedGraph;
import com.facebook.buck.model.BuildTargetFactory;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.Buildable;
import com.facebook.buck.rules.DependencyGraph;
import com.facebook.buck.rules.FakeAbstractBuildRuleBuilderParams;
import com.facebook.buck.rules.FakeBuildableContext;
import com.facebook.buck.step.Step;
import com.facebook.buck.testutil.MoreAsserts;
import com.facebook.buck.util.BuckConstant;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.easymock.EasyMock;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Unit test for {@link AndroidManifestRule}.
 */
public class AndroidManifestRuleTest {

  /**
   * Tests the following methods:
   * <ul>
   *   <li>{@link AndroidManifestRule#getType()}
   *   <li>{@link Buildable#getInputsToCompareToOutput()}
   *   <li>{@link AndroidManifestRule#getPathToOutputFile()}
   * </ul>
   */
  @Test
  public void testSimpleObserverMethods() {
    AndroidManifestRule androidManifestRule = createSimpleAndroidManifestRule();

    assertEquals("android_manifest", androidManifestRule.getType().getName());
    assertEquals(
        ImmutableList.of("java/com/example/AndroidManifestSkeleton.xml"),
        androidManifestRule.getInputsToCompareToOutput());
    assertEquals(
        BuckConstant.GEN_DIR + "/java/com/example/AndroidManifest__manifest__.xml",
        androidManifestRule.getPathToOutputFile());
  }

  @Test
  public void testBuildInternal() throws IOException {
    AndroidManifestRule androidManifestRule = createSimpleAndroidManifestRule();

    // This is a simple dependency graph because our only rule has no deps.
    MutableDirectedGraph<BuildRule> graph = new MutableDirectedGraph<>();
    graph.addNode(androidManifestRule);
    DependencyGraph dependencyGraph = new DependencyGraph(graph);

    // Mock out a BuildContext whose DependencyGraph will be traversed.
    BuildContext buildContext = EasyMock.createMock(BuildContext.class);
    EasyMock.expect(buildContext.getDependencyGraph()).andReturn(dependencyGraph);
    EasyMock.replay(buildContext);

    List<Step> steps = androidManifestRule.getBuildSteps(buildContext, new FakeBuildableContext());
    MoreAsserts.assertListEquals(
        ImmutableList.of(
            new GenerateManifestStep(
                "java/com/example/AndroidManifestSkeleton.xml",
                /* libraryManifestPaths */ ImmutableSet.<String>of(),
                BuckConstant.GEN_DIR + "/java/com/example/AndroidManifest__manifest__.xml")),
        steps);

    EasyMock.verify(buildContext);
  }

  private AndroidManifestRule createSimpleAndroidManifestRule() {
    return AndroidManifestRule.newManifestMergeRuleBuilder(new FakeAbstractBuildRuleBuilderParams())
        .setBuildTarget(BuildTargetFactory.newInstance("//java/com/example:manifest"))
        .setSkeletonFile("java/com/example/AndroidManifestSkeleton.xml")
        .build(new BuildRuleResolver());
  }

  // TODO(user): Add another unit test that passes in a non-trivial DependencyGraph and verify that
  // the resulting set of libraryManifestPaths is computed correctly.
}
