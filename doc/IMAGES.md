    Copyright 2014 Google Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


# Image resizing in IOSched

Minimizing data traffic is an important requirement for any mobile app, in
special those used in an environment where Internet access is limited, like
a packed conference. But, on the other hand, rich session and speaker images
can make an app like IOSched easier to navigate and more beautiful, and even
help the conference attendees to identify speakers on the conference floor.

To satisfy those conflicting requirements, we implemented a scheme to serve
images in multiple sizes (defined by width, maintaining the aspect ratio).
One server-side script compresses, optimizes and resizes images to specific
buckets, saving them on computable variants of the original URL. On the
client side, an image loader detects a specially crafted pattern in the
image URL and requests the best image depending on the width of the
container where it will be rendered.


## Decoupling

To avoid high coupling between the image resizing scheme on the server and
the Android app, the image URL is crafted in a way that the information
about the available buckets is carried in it, allowing the server to
dynamically change the buckets of images without requiring changes to the
Android side. Also, if the client has no knowledge of this special URL, for
example an old version of the Android app, the URL without any special
handling should serve the full size image.


## Adaptive image URL format

Any image URL that matches the following (simplified) regexp is considered
an adaptive image URL: `.*__w(-\d+)+__.*`

For example:

<pre>
myserver.com/images/<b>__w-200-400-600-800-1000__</b>/session1.jpg
</pre>

As mentioned before, by convention this encoded URL will serve the full size
image. To get the image restricted to width 200px, replace `__w-...__` by
"**w200**":

<pre>
myserver.com/images/<b>w200</b>/session1.jpg
</pre>

And for 400px of width:

<pre>
myserver.com/images/<b>w400</b>/session1.jpg
</pre>

As one can guess, the widths that an image has been resized to are the numbers
delimited by dashes between `__w-` and `__`. The URL

<pre>
myserver.com/images/<b>__w-200-400-600-800-1000__</b>/session1.jpg
</pre>

Means that this image can be fetched at any of the following URLs:

URL | Image size
--- | ----------
myserver.com/images/**__w-200-400-600-800-1000__**/session1.jpg | original
myserver.com/images/w200/session1.jpg | 200px wide
myserver.com/images/w400/session1.jpg | 400px wide
myserver.com/images/w600/session1.jpg | 600px wide
myserver.com/images/w800/session1.jpg | 800px wide
myserver.com/images/w1000/session1.jpg | 1000px wide


## On the server side

We decided to process the images offline and serve them statically, in order
to improve scalability and reliability, but a dynamic server would also
work.

Every new or changed session has its image converted to JPG, compressed at
0.8 quality factor, resized to the appropriate buckets and then saved to
Cloud Storage. This is all achieved by a simple Bash script running as a
cron job.  The result is a directory structure like:

    /__w-200-400-600__/session1.jpg
    /__w-200-400-600__/session2.jpg
    /__w-200-400-600__/session3.jpg
    /w200/session1.jpg
    /w200/session2.jpg
    /w200/session3.jpg
    /w400/session1.jpg
    /w400/session2.jpg
    /w400/session3.jpg
    /w600/session1.jpg
    /w600/session2.jpg
    /w600/session3.jpg

We save this directory structure to Google Cloud Storage and update whenever
a session or a speaker changes its photo.

# On the Android app

Every remote image in IOSched is loaded through a special library, [Glide](https://github.com/bumptech/glide),
that appropriately handles asynchronous loading and caching. We extended
Glide with a custom [ImageLoader](../android/src/main/java/com/google/samples/apps/iosched/util/ImageLoader.java) that understands our
adaptive URL format. In particular, the inner class VariableWidthImageLoader
contains the following snippet:

```Java
private static final Pattern PATTERN =
      Pattern.compile("__w-((?:-?\\d+)+)__");

@Override
protected String getUrl(String model, int width, int height) {
    Matcher m = PATTERN.matcher(model);
    int bestBucket = 0;
    if (m.find()) {
        String[] found = m.group(1).split("-");
        for (String bucketStr : found) {
            bestBucket = Integer.parseInt(bucketStr);
            if (bestBucket >= width) {
                // the best bucket is the first immediately
                // bigger than the requested width
                break;
            }
        }
        if (bestBucket > 0) {
            model = m.replaceFirst("w"+bestBucket);
        }
    }
    return model;
}
```

The logic is very simple: whenever an image is about to
be loaded, this code checks if the image URL contains the specific pattern.
If so, it is broken into pieces to find the available bucket widths that the
image is available on, and the first bucket bigger than or equals to the
container width (represented by the parameter `width`) is used. The URL
is changed to the w<bucket> version and the async loading continues as usual.

