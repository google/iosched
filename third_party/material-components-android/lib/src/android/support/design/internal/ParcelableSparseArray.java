/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.design.internal;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;
import android.util.SparseArray;

/** @hide */
@RestrictTo(LIBRARY_GROUP)
public class ParcelableSparseArray extends SparseArray<Parcelable> implements Parcelable {

  public ParcelableSparseArray() {
    super();
  }

  public ParcelableSparseArray(Parcel source, ClassLoader loader) {
    super();
    int size = source.readInt();
    int[] keys = new int[size];
    source.readIntArray(keys);
    Parcelable[] values = source.readParcelableArray(loader);
    for (int i = 0; i < size; ++i) {
      put(keys[i], values[i]);
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    int size = size();
    int[] keys = new int[size];
    Parcelable[] values = new Parcelable[size];
    for (int i = 0; i < size; ++i) {
      keys[i] = keyAt(i);
      values[i] = valueAt(i);
    }
    parcel.writeInt(size);
    parcel.writeIntArray(keys);
    parcel.writeParcelableArray(values, flags);
  }

  public static final Creator<ParcelableSparseArray> CREATOR =
      new ClassLoaderCreator<ParcelableSparseArray>() {
        @Override
        public ParcelableSparseArray createFromParcel(Parcel source, ClassLoader loader) {
          return new ParcelableSparseArray(source, loader);
        }

        @Override
        public ParcelableSparseArray createFromParcel(Parcel source) {
          return new ParcelableSparseArray(source, null);
        }

        @Override
        public ParcelableSparseArray[] newArray(int size) {
          return new ParcelableSparseArray[size];
        }
      };
}
