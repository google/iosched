/*
 * Copyright (C) 2017 The Android Open Source Project
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

package io.material.demo.shrine.products;

import static com.google.common.truth.Truth.assertThat;

import android.support.v7.widget.RecyclerView;
import org.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Unit tests for {@link ProductListActivity}. */
@RunWith(RobolectricTestRunner.class)
@Config(
  manifest =
      "//third_party/java_src/android_libs/material_components/demos/java/io/material/demo/shrine/products" + ":products/AndroidManifest.xml"
)
public class ProductListActivityTest {

  private ProductListActivity productListActivity;
  private RecyclerView productsRecyclerView;

  @Before
  public void setUp() {
    productListActivity = Robolectric.setupActivity(ProductListActivity.class);
    productsRecyclerView = (RecyclerView) productListActivity.findViewById(R.id.ProductGrid);
  }

  @Test
  public void testProductListIsNotNull() {
    assertThat(productsRecyclerView).isNotNull();
  }

  @Test
  public void testProductListHasProducts() {
    int productCount = productsRecyclerView.getChildCount();
    assertThat(productCount).isAtLeast(1);
  }
}
