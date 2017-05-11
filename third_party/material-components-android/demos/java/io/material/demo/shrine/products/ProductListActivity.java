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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.volley.toolbox.NetworkImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.material.demo.shrine.filters.FiltersActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** Activity that displays a list of products. */
public class ProductListActivity extends AppCompatActivity {
  private static final String TAG = ProductListActivity.class.getSimpleName();
  private ImageRequester imageRequester;
  private List<ProductEntry> productEntryList;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.Theme_Shrine);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.shrine_product_activity);
    imageRequester = ImageRequester.getInstance(this);
    initProductEntryList();
    initCollapsingToolbar();
    initItemGrid();
  }

  private void initProductEntryList() {
    InputStream inputStream = getResources().openRawResource(R.raw.products);
    Writer writer = new StringWriter();
    char[] buffer = new char[1024];
    try {
      Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      int pointer;
      while ((pointer = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, pointer);
      }
    } catch (IOException exception) {
      Log.e(TAG, "Error writing/reading from the JSON file.", exception);
    } finally {
      try {
        inputStream.close();
      } catch (IOException exception) {
        Log.e(TAG, "Error closing the input stream.", exception);
      }
    }
    String jsonProductsString = writer.toString();
    Gson gson = new Gson();
    Type productListType = new TypeToken<ArrayList<ProductEntry>>() {}.getType();
    this.productEntryList = gson.fromJson(jsonProductsString, productListType);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.shrine_toolbar_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {
    if (menuItem.getItemId() == R.id.ShrineToolbarFilterIcon) {
      startActivity(new Intent(this, FiltersActivity.class));
      return true;
    }
    return false;
  }

  private void initCollapsingToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.AppBar);
    int collapsingToolbarImageIndex = 6;
    int collapsingImageToolbarSize = 1200;
    NetworkImageView collapsingToolbarImage =
        (NetworkImageView) findViewById(R.id.CollapsingToolbarImage);
    StringBuilder collapsingToolbarImageUrl =
        new StringBuilder(productEntryList.get(collapsingToolbarImageIndex).url);
    collapsingToolbarImageUrl.append("=s");
    collapsingToolbarImageUrl.append(collapsingImageToolbarSize);
    setSupportActionBar(toolbar);
    CollapsingToolbarLayout collapsingToolbarLayout =
        (CollapsingToolbarLayout) findViewById(R.id.CollapsingToolbarLayout);
    collapsingToolbarLayout.setTitle(toolbar.getTitle());
    collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.TextAppearance_Shrine_Logo);
    collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.TextAppearance_Shrine_Logo);
    imageRequester.setImageFromUrl(collapsingToolbarImage, collapsingToolbarImageUrl.toString());
    Point windowSize = new Point();
    getWindowManager().getDefaultDisplay().getSize(windowSize);
    int windowWidth = windowSize.x;
    collapsingToolbarImage.setX(collapsingToolbarImage.getX() - windowWidth / 4);
    collapsingToolbarLayout.setScrimVisibleHeightTrigger(
        (int) getResources().getDimension(R.dimen.shrine_tall_toolbar_height) / 2);
  }

  private void initItemGrid() {
    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.ProductGrid);
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    recyclerView.setAdapter(new ProductAdapter(this, productEntryList, imageRequester));
  }

  private static class ProductAdapter extends Adapter<ProductViewHolder> {
    private final Activity activity;
    private final List<ProductEntry> productEntries;
    public ImageRequester imageRequester;

    public ProductAdapter(
        Activity activity, List<ProductEntry> productEntries, ImageRequester imageRequester) {
      this.activity = activity;
      this.productEntries = productEntries;
      this.imageRequester = imageRequester;
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
      return new ProductViewHolder(activity, viewGroup);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder productViewHolder, int i) {
      productViewHolder.bind(activity, productEntries.get(i), imageRequester);
    }

    @Override
    public int getItemCount() {
      return productEntries.size();
    }
  }

  private static class ProductViewHolder extends ViewHolder {
    private final TextView productPriceView;
    private final NetworkImageView productImageView;
    private final TextView productShopNameView;
    private final CardView productEntryView;

    public ProductViewHolder(Context context, ViewGroup parent) {
      super(LayoutInflater.from(context).inflate(R.layout.shrine_product_entry, parent, false));
      ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
      itemView.setLayoutParams(layoutParams);
      productPriceView = (TextView) itemView.findViewById(R.id.ProductPrice);
      productImageView = (NetworkImageView) itemView.findViewById(R.id.ProductImage);
      productShopNameView = (TextView) itemView.findViewById(R.id.ProductShopName);
      productEntryView = (CardView) itemView.findViewById(R.id.ProductEntry);
      productEntryView.setOnClickListener(clickListener);
    }

    private final OnClickListener clickListener =
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            ItemActivity.createItemActivityIntent(v.getContext(), (ProductEntry) v.getTag());
          }
        };

    public void bind(Context context, ProductEntry productEntry, ImageRequester imageRequester) {
      productPriceView.setText(productEntry.price);
      imageRequester.setImageFromUrl(productImageView, productEntry.url);
      productShopNameView.setText(productEntry.title);
      productEntryView.setTag(productEntry);
    }
  }
}
