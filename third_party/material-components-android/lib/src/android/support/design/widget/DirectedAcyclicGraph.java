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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.support.v4.util.SimpleArrayMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** A class which represents a simple directed acyclic graph. */
final class DirectedAcyclicGraph<T> {
  private final Pools.Pool<ArrayList<T>> mListPool = new Pools.SimplePool<>(10);
  private final SimpleArrayMap<T, ArrayList<T>> mGraph = new SimpleArrayMap<>();

  private final ArrayList<T> mSortResult = new ArrayList<>();
  private final HashSet<T> mSortTmpMarked = new HashSet<>();

  /**
   * Add a node to the graph.
   *
   * <p>If the node already exists in the graph then this method is a no-op.
   *
   * @param node the node to add
   */
  void addNode(@NonNull T node) {
    if (!mGraph.containsKey(node)) {
      mGraph.put(node, null);
    }
  }

  /** Returns true if the node is already present in the graph, false otherwise. */
  boolean contains(@NonNull T node) {
    return mGraph.containsKey(node);
  }

  /**
   * Add an edge to the graph.
   *
   * <p>Both the given nodes should already have been added to the graph through {@link
   * #addNode(Object)}.
   *
   * @param node the parent node
   * @param incomingEdge the node which has is an incoming edge to {@code node}
   */
  void addEdge(@NonNull T node, @NonNull T incomingEdge) {
    if (!mGraph.containsKey(node) || !mGraph.containsKey(incomingEdge)) {
      throw new IllegalArgumentException(
          "All nodes must be present in the graph before" + " being added as an edge");
    }

    ArrayList<T> edges = mGraph.get(node);
    if (edges == null) {
      // If edges is null, we should try and get one from the pool and add it to the graph
      edges = getEmptyList();
      mGraph.put(node, edges);
    }
    // Finally add the edge to the list
    edges.add(incomingEdge);
  }

  /**
   * Get any incoming edges from the given node.
   *
   * @return a list containing any incoming edges, or null if there are none.
   */
  @Nullable
  List<T> getIncomingEdges(@NonNull T node) {
    return mGraph.get(node);
  }

  /**
   * Get any outgoing edges for the given node (i.e. nodes which have an incoming edge from the
   * given node).
   *
   * @return a list containing any outgoing edges, or null if there are none.
   */
  @Nullable
  List<T> getOutgoingEdges(@NonNull T node) {
    ArrayList<T> result = null;
    for (int i = 0, size = mGraph.size(); i < size; i++) {
      ArrayList<T> edges = mGraph.valueAt(i);
      if (edges != null && edges.contains(node)) {
        if (result == null) {
          result = new ArrayList<>();
        }
        result.add(mGraph.keyAt(i));
      }
    }
    return result;
  }

  boolean hasOutgoingEdges(@NonNull T node) {
    for (int i = 0, size = mGraph.size(); i < size; i++) {
      ArrayList<T> edges = mGraph.valueAt(i);
      if (edges != null && edges.contains(node)) {
        return true;
      }
    }
    return false;
  }

  /** Clears the internal graph, and releases resources to pools. */
  void clear() {
    for (int i = 0, size = mGraph.size(); i < size; i++) {
      ArrayList<T> edges = mGraph.valueAt(i);
      if (edges != null) {
        poolList(edges);
      }
    }
    mGraph.clear();
  }

  /**
   * Returns a topologically sorted list of the nodes in this graph. This uses the DFS algorithm as
   * described by Cormen et al. (2001). If this graph contains cyclic dependencies then this method
   * will throw a {@link RuntimeException}.
   *
   * <p>The resulting list will be ordered such that index 0 will contain the node at the bottom of
   * the graph. The node at the end of the list will have no dependencies on other nodes.
   */
  @NonNull
  ArrayList<T> getSortedList() {
    mSortResult.clear();
    mSortTmpMarked.clear();

    // Start a DFS from each node in the graph
    for (int i = 0, size = mGraph.size(); i < size; i++) {
      dfs(mGraph.keyAt(i), mSortResult, mSortTmpMarked);
    }

    return mSortResult;
  }

  private void dfs(final T node, final ArrayList<T> result, final HashSet<T> tmpMarked) {
    if (result.contains(node)) {
      // We've already seen and added the node to the result list, skip...
      return;
    }
    if (tmpMarked.contains(node)) {
      throw new RuntimeException("This graph contains cyclic dependencies");
    }
    // Temporarily mark the node
    tmpMarked.add(node);
    // Recursively dfs all of the node's edges
    final ArrayList<T> edges = mGraph.get(node);
    if (edges != null) {
      for (int i = 0, size = edges.size(); i < size; i++) {
        dfs(edges.get(i), result, tmpMarked);
      }
    }
    // Unmark the node from the temporary list
    tmpMarked.remove(node);
    // Finally add it to the result list
    result.add(node);
  }

  /** Returns the size of the graph */
  int size() {
    return mGraph.size();
  }

  @NonNull
  private ArrayList<T> getEmptyList() {
    ArrayList<T> list = mListPool.acquire();
    if (list == null) {
      list = new ArrayList<>();
    }
    return list;
  }

  private void poolList(@NonNull ArrayList<T> list) {
    list.clear();
    mListPool.release(list);
  }
}
