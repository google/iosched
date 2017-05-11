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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.volley.toolbox.NetworkImageView;

/**
 * Activity that displays an individual product, including an image, name, and a short description.
 */
public class ItemActivity extends AppCompatActivity {
  private static final String PRODUCT_TITLE = "title";
  private static final String PRODUCT_URL = "url";
  private static final String PRODUCT_DESCRIPTION = "description";
  private static final String TAG = ItemActivity.class.getSimpleName();
  private String fabMessage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.shrine_item_activity);
    initToolbar();
    populateProductViews();
    initFab();
  }

  public static void createItemActivityIntent(Context context, ProductEntry productEntry) {
    Intent intent = new Intent(context, ItemActivity.class);
    intent.putExtra(PRODUCT_TITLE, productEntry.title);
    intent.putExtra(PRODUCT_URL, productEntry.url);
    intent.putExtra(PRODUCT_DESCRIPTION, productEntry.description);
    context.startActivity(intent);
  }

  private void populateProductViews() {
    Bundle bundle = getIntent().getExtras();
    if (bundle == null) {
      String error = "Error retrieving product information.";
      Log.e(TAG, error);
      Snackbar.make(findViewById(R.id.ProductTitle), error, Snackbar.LENGTH_SHORT).show();
      fabMessage = error;
    } else {
      TextView productTitle = (TextView) findViewById(R.id.ProductTitle);
      productTitle.setText(bundle.getString(PRODUCT_TITLE));
      TextView productDescription = (TextView) findViewById(R.id.ProductDescription);
      productDescription.setText(bundle.getString(PRODUCT_DESCRIPTION));
      initSpinner();
      NetworkImageView productImage = (NetworkImageView) findViewById(R.id.ProductImage);
      ImageRequester.getInstance(this).setImageFromUrl(productImage, bundle.getString(PRODUCT_URL));
      fabMessage = getString(R.string.shrine_product_added_message);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.shrine_toolbar_menu, menu);
    menu.findItem(R.id.ShrineToolbarFilterIcon).setVisible(false);
    return true;
  }

  private void initToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.AppBar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  private void initFab() {
    findViewById(R.id.FloatingActionButton).setOnClickListener(clickListener);
  }

  private final OnClickListener clickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      Snackbar.make(v, fabMessage, Snackbar.LENGTH_SHORT).show();
    }
  };

  private void initSpinner() {
    Spinner spinner = (Spinner) findViewById(R.id.QuantitySpinner);
    ArrayAdapter<CharSequence> adapter =
        ArrayAdapter.createFromResource(
            this, R.array.product_quantities, R.layout.shrine_spinner_item);
    spinner.setAdapter(adapter);
  }
}
