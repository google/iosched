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

package io.material.demo.codelab.buildingbeautifulapps;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for Shrine that displays a listing of available products.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ProductAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shr_main);

        Toolbar appBar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(appBar);

        ArrayList<ProductEntry> products = readProductsList();
        ImageRequester imageRequester = ImageRequester.getInstance(this);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.product_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(products, imageRequester);
        recyclerView.setAdapter(adapter);
    }

    private ArrayList<ProductEntry> readProductsList() {
        InputStream inputStream = getResources().openRawResource(R.raw.products);
        Type productListType = new TypeToken<ArrayList<ProductEntry>>() {}.getType();
        try {
            return JsonReader.readJsonStream(inputStream, productListType);
        } catch (IOException e) {
            Log.e(TAG, "Error reading JSON product list", e);
            return new ArrayList<>();
        }
    }

    private static final class ProductAdapter extends RecyclerView.Adapter<ProductViewHolder> {
        private List<ProductEntry> products;
        private final ImageRequester imageRequester;

        ProductAdapter(List<ProductEntry> products, ImageRequester imageRequester) {
            this.products = products;
            this.imageRequester = imageRequester;
        }

        void setProducts(List<ProductEntry> products) {
            this.products = products;
            notifyDataSetChanged();
        }

        @Override
        public ProductViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ProductViewHolder(viewGroup);
        }

        @Override
        public void onBindViewHolder(ProductViewHolder viewHolder, int i) {
            viewHolder.bind(products.get(i), imageRequester);
        }

        @Override
        public int getItemCount() {
            return products.size();
        }
    }

    private static final class ProductViewHolder extends RecyclerView.ViewHolder {
        private final NetworkImageView imageView;
        private final TextView priceView;

        ProductViewHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.shr_product_entry, parent, false));
            imageView = (NetworkImageView) itemView.findViewById(R.id.image);
            priceView = (TextView) itemView.findViewById(R.id.price);
            itemView.setOnClickListener(clickListener);
        }

        private final View.OnClickListener clickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProductEntry product = (ProductEntry) v.getTag(R.id.tag_product_entry);
                        // TODO: show product details
                    }
                };

        void bind(ProductEntry product, ImageRequester imageRequester) {
            itemView.setTag(R.id.tag_product_entry, product);
            imageRequester.setImageFromUrl(imageView, product.url);
            priceView.setText(product.price);
        }
    }
}
