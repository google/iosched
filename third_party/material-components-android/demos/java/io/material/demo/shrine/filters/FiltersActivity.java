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

package io.material.demo.shrine.filters;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Activity that displays a list of filters the user can set to refine the list of products.
 */
public class FiltersActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.shrine_filters_activity);
    initToolbar();
  }

  private void initToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.AppBar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.shrine_filter_page_title));
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }
}
