/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.design.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class DirectedAcyclicGraphTest {

  private DirectedAcyclicGraph<TestNode> mGraph;

  @Before
  public void setup() {
    mGraph = new DirectedAcyclicGraph<>();
  }

  @Test
  public void test_addNode() {
    final TestNode node = new TestNode("node");
    mGraph.addNode(node);
    assertEquals(1, mGraph.size());
    assertTrue(mGraph.contains(node));
  }

  @Test
  public void test_addNodeAgain() {
    final TestNode node = new TestNode("node");
    mGraph.addNode(node);
    mGraph.addNode(node);

    assertEquals(1, mGraph.size());
    assertTrue(mGraph.contains(node));
  }

  @Test
  public void test_addEdge() {
    final TestNode node = new TestNode("node");
    final TestNode edge = new TestNode("edge");

    mGraph.addNode(node);
    mGraph.addNode(edge);
    mGraph.addEdge(node, edge);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_addEdgeWithNotAddedEdgeNode() {
    final TestNode node = new TestNode("node");
    final TestNode edge = new TestNode("edge");

    // Add the node, but not the edge node
    mGraph.addNode(node);

    // Now add the link
    mGraph.addEdge(node, edge);
  }

  @Test
  public void test_getIncomingEdges() {
    final TestNode node = new TestNode("node");
    final TestNode edge = new TestNode("edge");
    mGraph.addNode(node);
    mGraph.addNode(edge);
    mGraph.addEdge(node, edge);

    final List<TestNode> incomingEdges = mGraph.getIncomingEdges(node);
    assertNotNull(incomingEdges);
    assertEquals(1, incomingEdges.size());
    assertEquals(edge, incomingEdges.get(0));
  }

  @Test
  public void test_getOutgoingEdges() {
    final TestNode node = new TestNode("node");
    final TestNode edge = new TestNode("edge");
    mGraph.addNode(node);
    mGraph.addNode(edge);
    mGraph.addEdge(node, edge);

    // Now assert the getOutgoingEdges returns a list which has one element (node)
    final List<TestNode> outgoingEdges = mGraph.getOutgoingEdges(edge);
    assertNotNull(outgoingEdges);
    assertEquals(1, outgoingEdges.size());
    assertTrue(outgoingEdges.contains(node));
  }

  @Test
  public void test_getOutgoingEdgesMultiple() {
    final TestNode node1 = new TestNode("1");
    final TestNode node2 = new TestNode("2");
    final TestNode edge = new TestNode("edge");
    mGraph.addNode(node1);
    mGraph.addNode(node2);
    mGraph.addNode(edge);

    mGraph.addEdge(node1, edge);
    mGraph.addEdge(node2, edge);

    // Now assert the getOutgoingEdges returns a list which has 2 elements (node1 & node2)
    final List<TestNode> outgoingEdges = mGraph.getOutgoingEdges(edge);
    assertNotNull(outgoingEdges);
    assertEquals(2, outgoingEdges.size());
    assertTrue(outgoingEdges.contains(node1));
    assertTrue(outgoingEdges.contains(node2));
  }

  @Test
  public void test_hasOutgoingEdges() {
    final TestNode node = new TestNode("node");
    final TestNode edge = new TestNode("edge");
    mGraph.addNode(node);
    mGraph.addNode(edge);

    // There is no edge currently and assert that fact
    assertFalse(mGraph.hasOutgoingEdges(edge));
    // Now add the edge
    mGraph.addEdge(node, edge);
    // and assert that the methods returns true;
    assertTrue(mGraph.hasOutgoingEdges(edge));
  }

  @Test
  public void test_clear() {
    final TestNode node1 = new TestNode("1");
    final TestNode node2 = new TestNode("2");
    final TestNode edge = new TestNode("edge");
    mGraph.addNode(node1);
    mGraph.addNode(node2);
    mGraph.addNode(edge);

    // Now clear the graph
    mGraph.clear();

    // Now assert the graph is empty and that contains returns false
    assertEquals(0, mGraph.size());
    assertFalse(mGraph.contains(node1));
    assertFalse(mGraph.contains(node2));
    assertFalse(mGraph.contains(edge));
  }

  @Test
  public void test_getSortedList() {
    final TestNode node1 = new TestNode("A");
    final TestNode node2 = new TestNode("B");
    final TestNode node3 = new TestNode("C");
    final TestNode node4 = new TestNode("D");

    // Now we'll add the nodes
    mGraph.addNode(node1);
    mGraph.addNode(node2);
    mGraph.addNode(node3);
    mGraph.addNode(node4);

    // Now we'll add edges so that 4 <- 2, 2 <- 3, 3 <- 1  (where <- denotes a dependency)
    mGraph.addEdge(node4, node2);
    mGraph.addEdge(node2, node3);
    mGraph.addEdge(node3, node1);

    final List<TestNode> sorted = mGraph.getSortedList();
    // Assert that it is the correct size
    assertEquals(4, sorted.size());
    // Assert that all of the nodes are present and in their sorted order
    assertEquals(node1, sorted.get(0));
    assertEquals(node3, sorted.get(1));
    assertEquals(node2, sorted.get(2));
    assertEquals(node4, sorted.get(3));
  }

  private static class TestNode {
    private final String mLabel;

    TestNode(@NonNull String label) {
      mLabel = label;
    }

    @Override
    public String toString() {
      return "TestNode: " + mLabel;
    }
  }
}
