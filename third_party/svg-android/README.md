# Status: Updated to support strings in current Android AOSP.

### _

This is forked from:
https://github.com/japgolly/svg-android/

Which is forked from the awesome but unmaintained:
http://code.google.com/p/svg-android/

Changes
=======
* Updated to support new strings in current Android AOSP.

Maven
=====
Add this to your Android project's pom.xml:
```xml
<dependency>
    <groupId>com.github.jeffyhao.android</groupId>
    <artifactId>svg-android</artifactId>
    <version>2.0.7</version>
</dependency>

<repositories>
    <repository>
        <id>svg-android-mvn-repo</id>
        <url>https://raw.github.com/jeffyhao/svg-android/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

Usage
=====

Firstly, store your SVGs in `res/raw` or `assets`.

```java
// Load and parse a SVG
SVG svg = new SVGBuilder()
            .readFromResource(getResources(), R.raw.someSvgResource) // if svg in res/raw
            .readFromAsset(getAssets(), "somePicture.svg")           // if svg in assets
            // .setWhiteMode(true) // draw fills in white, doesn't draw strokes
            // .setColorSwap(0xFF008800, 0xFF33AAFF) // swap a single colour
            // .setColorFilter(filter) // run through a colour filter
            // .set[Stroke|Fill]ColorFilter(filter) // apply a colour filter to only the stroke or fill
            .build();

// Draw onto a canvas
canvas.drawPicture(svg.getPicture());

// Turn into a drawable
Drawable drawable = svg.createDrawable();
// drawable.draw(canvas);
// imageView.setImageDrawable(drawable);
```
