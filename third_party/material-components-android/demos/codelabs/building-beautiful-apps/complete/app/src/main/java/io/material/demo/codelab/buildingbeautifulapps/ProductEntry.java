
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

/**
 * A product entry in the list of products.
 */
public class ProductEntry {
    public final String title;
    public final String url;
    public final String price;
    public final String description;

    public ProductEntry(String title, String url, String price, String description) {
        this.title = title;
        this.url = url;
        this.price = price;
        this.description = description;
    }
}
