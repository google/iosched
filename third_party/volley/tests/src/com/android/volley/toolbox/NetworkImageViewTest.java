package com.android.volley.toolbox;

import android.test.InstrumentationTestCase;
import android.view.ViewGroup.LayoutParams;

public class NetworkImageViewTest extends InstrumentationTestCase {
    private NetworkImageView mNIV;
    private MockImageLoader mMockImageLoader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMockImageLoader = new MockImageLoader();
        mNIV = new NetworkImageView(getInstrumentation().getContext());
    }

    public void testSetImageUrl_requestsImage() {
        mNIV.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mNIV.setImageUrl("http://foo", mMockImageLoader);
        assertEquals("http://foo", mMockImageLoader.lastRequestUrl);
        assertEquals(0, mMockImageLoader.lastMaxWidth);
        assertEquals(0, mMockImageLoader.lastMaxHeight);
    }

    // public void testSetImageUrl_setsMaxSize() {
    // // TODO: Not sure how to make getWidth() return something from an
    // // instrumentation test. Write this test once it's figured out.
    // }

    private class MockImageLoader extends ImageLoader {
        public MockImageLoader() {
            super(null, null);
        }

        public String lastRequestUrl;
        public int lastMaxWidth;
        public int lastMaxHeight;

        public ImageContainer get(String requestUrl, ImageListener imageListener, int maxWidth,
                int maxHeight) {
            lastRequestUrl = requestUrl;
            lastMaxWidth = maxWidth;
            lastMaxHeight = maxHeight;
            return null;
        }
    }
}
