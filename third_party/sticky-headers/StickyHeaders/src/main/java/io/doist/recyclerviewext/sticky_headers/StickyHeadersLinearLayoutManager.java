package io.doist.recyclerviewext.sticky_headers;

import android.content.Context;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds sticky headers capabilities to your {@link RecyclerView.Adapter}. It must implement {@link StickyHeaders} to
 * indicate which items are headers.
 */
public class StickyHeadersLinearLayoutManager<T extends RecyclerView.Adapter & StickyHeaders>
        extends LinearLayoutManager {
    private T mAdapter;

    private float mTranslationX;
    private float mTranslationY;

    // Header positions for the currently displayed list and their observer.
    private List<Integer> mHeaderPositions = new ArrayList<>(0);
    private RecyclerView.AdapterDataObserver mHeaderPositionsObserver = new HeaderPositionsAdapterDataObserver();

    // Sticky header's ViewHolder and dirty state.
    private View mStickyHeader;
    private int mStickyHeaderPosition = RecyclerView.NO_POSITION;

    private int mPendingScrollPosition = RecyclerView.NO_POSITION;
    private int mPendingScrollOffset = 0;

    public StickyHeadersLinearLayoutManager(Context context) {
        super(context);
    }

    public StickyHeadersLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    /**
     * Offsets the vertical location of the sticky header relative to the its default position.
     */
    public void setStickyHeaderTranslationY(float translationY) {
        mTranslationY = translationY;
        requestLayout();
    }

    /**
     * Offsets the horizontal location of the sticky header relative to the its default position.
     */
    public void setStickyHeaderTranslationX(float translationX) {
        mTranslationX = translationX;
        requestLayout();
    }

    /**
     * Returns true if {@code view} is the current sticky header.
     */
    public boolean isStickyHeader(View view) {
        return view == mStickyHeader;
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        setAdapter(view.getAdapter());
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
        setAdapter(newAdapter);
    }

    @SuppressWarnings("unchecked")
    private void setAdapter(RecyclerView.Adapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mHeaderPositionsObserver);
        }

        if (adapter instanceof StickyHeaders) {
            mAdapter = (T) adapter;
            mAdapter.registerAdapterDataObserver(mHeaderPositionsObserver);
            mHeaderPositionsObserver.onChanged();
        } else {
            mAdapter = null;
            mHeaderPositions.clear();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState();
        ss.superState = super.onSaveInstanceState();
        ss.pendingScrollPosition = mPendingScrollPosition;
        ss.pendingScrollOffset = mPendingScrollOffset;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            mPendingScrollPosition = ss.pendingScrollPosition;
            mPendingScrollOffset = ss.pendingScrollOffset;
            state = ss.superState;
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachStickyHeader();
        int scrolled = super.scrollVerticallyBy(dy, recycler, state);
        attachStickyHeader();

        if (scrolled != 0) {
            updateStickyHeader(recycler, false);
        }

        return scrolled;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachStickyHeader();
        int scrolled = super.scrollHorizontallyBy(dx, recycler, state);
        attachStickyHeader();

        if (scrolled != 0) {
            updateStickyHeader(recycler, false);
        }

        return scrolled;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachStickyHeader();
        super.onLayoutChildren(recycler, state);
        attachStickyHeader();

        if (!state.isPreLayout()) {
            updateStickyHeader(recycler, true);
        }
    }

    @Override
    public void scrollToPosition(int position) {
        scrollToPositionWithOffset(position, INVALID_OFFSET);
    }

    @Override
    public void scrollToPositionWithOffset(int position, int offset) {
        scrollToPositionWithOffset(position, offset, true);
    }

    private void scrollToPositionWithOffset(int position, int offset, boolean adjustForStickyHeader) {
        // Reset pending scroll.
        setPendingScroll(RecyclerView.NO_POSITION, INVALID_OFFSET);

        // Adjusting is disabled.
        if (!adjustForStickyHeader) {
            super.scrollToPositionWithOffset(position, offset);
            return;
        }

        // There is no header above or the position is a header.
        int headerIndex = findHeaderIndexOrBefore(position);
        if (headerIndex == -1 || findHeaderIndex(position) != -1) {
            super.scrollToPositionWithOffset(position, offset);
            return;
        }

        // The position is right below a header, scroll to the header.
        if (findHeaderIndex(position - 1) != -1) {
            super.scrollToPositionWithOffset(position - 1, offset);
            return;
        }

        // Current sticky header is the same as at the position. Adjust the scroll offset and reset pending scroll.
        if (mStickyHeader != null && headerIndex == findHeaderIndex(mStickyHeaderPosition)) {
            int adjustedOffset = (offset != INVALID_OFFSET ? offset : 0) + mStickyHeader.getHeight();
            super.scrollToPositionWithOffset(position, adjustedOffset);
            return;
        }

        // Remember this position and offset and scroll to it to trigger creating the sticky header.
        setPendingScroll(position, offset);
        super.scrollToPositionWithOffset(position, offset);
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        detachStickyHeader();
        int extent = super.computeVerticalScrollExtent(state);
        attachStickyHeader();
        return extent;
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        detachStickyHeader();
        int offset = super.computeVerticalScrollOffset(state);
        attachStickyHeader();
        return offset;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        detachStickyHeader();
        int range = super.computeVerticalScrollRange(state);
        attachStickyHeader();
        return range;
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        detachStickyHeader();
        int extent = super.computeHorizontalScrollExtent(state);
        attachStickyHeader();
        return extent;
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        detachStickyHeader();
        int offset = super.computeHorizontalScrollOffset(state);
        attachStickyHeader();
        return offset;
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        detachStickyHeader();
        int range = super.computeHorizontalScrollRange(state);
        attachStickyHeader();
        return range;
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        detachStickyHeader();
        PointF vector = super.computeScrollVectorForPosition(targetPosition);
        attachStickyHeader();
        return vector;
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        detachStickyHeader();
        View view = super.onFocusSearchFailed(focused, focusDirection, recycler, state);
        attachStickyHeader();
        return view;
    }

    private void detachStickyHeader() {
        if (mStickyHeader != null) {
            detachView(mStickyHeader);
        }
    }

    private void attachStickyHeader() {
        if (mStickyHeader != null) {
            attachView(mStickyHeader);
        }
    }

    /**
     * Updates the sticky header state (creation, binding, display), to be called whenever there's a layout or scroll
     */
    private void updateStickyHeader(RecyclerView.Recycler recycler, boolean layout) {
        int headerCount = mHeaderPositions.size();
        int childCount = getChildCount();
        if (headerCount > 0 && childCount > 0) {
            // Find first valid child.
            View anchorView = null;
            int anchorIndex = -1;
            int anchorPos = -1;
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                if (isViewValidAnchor(child, params)) {
                    anchorView = child;
                    anchorIndex = i;
                    anchorPos = params.getViewAdapterPosition();
                    break;
                }
            }
            if (anchorView != null && anchorPos != -1) {
                int headerIndex = findHeaderIndexOrBefore(anchorPos);
                int headerPos = headerIndex != -1 ? mHeaderPositions.get(headerIndex) : -1;
                int nextHeaderPos = headerCount > headerIndex + 1 ? mHeaderPositions.get(headerIndex + 1) : -1;

                // Show sticky header if:
                // - There's one to show;
                // - It's on the edge or it's not the anchor view;
                // - Isn't followed by another sticky header;
                if (headerPos != -1
                        && (headerPos != anchorPos || isViewOnBoundary(anchorView))
                        && nextHeaderPos != headerPos + 1) {
                    // Ensure existing sticky header, if any, is of correct type.
                    if (mStickyHeader != null
                            && getItemViewType(mStickyHeader) != mAdapter.getItemViewType(headerPos)) {
                        // A sticky header was shown before but is not of the correct type. Scrap it.
                        scrapStickyHeader(recycler);
                    }

                    // Ensure sticky header is created, if absent, or bound, if being laid out or the position changed.
                    if (mStickyHeader == null) {
                        createStickyHeader(recycler, headerPos);
                    }
                    if (layout || getPosition(mStickyHeader) != headerPos) {
                        bindStickyHeader(recycler, headerPos);
                    }

                    // Draw the sticky header using translation values which depend on orientation, direction and
                    // position of the next header view.
                    View nextHeaderView = null;
                    if (nextHeaderPos != -1) {
                        nextHeaderView = getChildAt(anchorIndex + (nextHeaderPos - anchorPos));
                        // The header view itself is added to the RecyclerView. Discard it if it comes up.
                        if (nextHeaderView == mStickyHeader) {
                            nextHeaderView = null;
                        }
                    }
                    mStickyHeader.setTranslationX(getX(mStickyHeader, nextHeaderView));
                    mStickyHeader.setTranslationY(getY(mStickyHeader, nextHeaderView));
                    return;
                }
            }
        }

        if (mStickyHeader != null) {
            scrapStickyHeader(recycler);
        }
    }

    /**
     * Creates {@link RecyclerView.ViewHolder} for {@code position}, including measure / layout, and assigns it to
     * {@link #mStickyHeader}.
     */
    private void createStickyHeader(@NonNull RecyclerView.Recycler recycler, int position) {
        View stickyHeader = recycler.getViewForPosition(position);

        // Setup sticky header if the adapter requires it.
        if (mAdapter instanceof StickyHeaders.ViewSetup) {
            ((StickyHeaders.ViewSetup) mAdapter).setupStickyHeaderView(stickyHeader);
        }

        // Add sticky header as a child view, to be detached / reattached whenever LinearLayoutManager#fill() is called,
        // which happens on layout and scroll (see overrides).
        addView(stickyHeader);
        measureAndLayout(stickyHeader);

        // Ignore sticky header, as it's fully managed by this LayoutManager.
        ignoreView(stickyHeader);

        mStickyHeader = stickyHeader;
        mStickyHeaderPosition = position;
    }

    /**
     * Binds the {@link #mStickyHeader} for the given {@code position}.
     */
    private void bindStickyHeader(@NonNull RecyclerView.Recycler recycler, int position) {
        // Bind the sticky header.
        recycler.bindViewToPosition(mStickyHeader, position);
        mStickyHeaderPosition = position;
        measureAndLayout(mStickyHeader);

        // If we have a pending scroll wait until the end of layout and scroll again.
        if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
            final ViewTreeObserver vto = mStickyHeader.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    vto.removeOnGlobalLayoutListener(this);

                    if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
                        scrollToPositionWithOffset(mPendingScrollPosition, mPendingScrollOffset);
                        setPendingScroll(RecyclerView.NO_POSITION, INVALID_OFFSET);
                    }
                }
            });
        }
    }

    /**
     * Measures and lays out {@code stickyHeader}.
     */
    private void measureAndLayout(View stickyHeader) {
        measureChildWithMargins(stickyHeader, 0, 0);
        if (getOrientation() == VERTICAL) {
            stickyHeader.layout(getPaddingLeft(), 0, getWidth() - getPaddingRight(), stickyHeader.getMeasuredHeight());
        } else {
            stickyHeader.layout(0, getPaddingTop(), stickyHeader.getMeasuredWidth(), getHeight() - getPaddingBottom());
        }
    }

    /**
     * Returns {@link #mStickyHeader} to the {@link RecyclerView}'s {@link RecyclerView.RecycledViewPool}, assigning it
     * to {@code null}.
     *
     * @param recycler If passed, the sticky header will be returned to the recycled view pool.
     */
    private void scrapStickyHeader(@Nullable RecyclerView.Recycler recycler) {
        View stickyHeader = mStickyHeader;
        mStickyHeader = null;
        mStickyHeaderPosition = RecyclerView.NO_POSITION;

        // Revert translation values.
        stickyHeader.setTranslationX(0);
        stickyHeader.setTranslationY(0);

        // Teardown holder if the adapter requires it.
        if (mAdapter instanceof StickyHeaders.ViewSetup) {
            ((StickyHeaders.ViewSetup) mAdapter).teardownStickyHeaderView(stickyHeader);
        }

        // Stop ignoring sticky header so that it can be recycled.
        stopIgnoringView(stickyHeader);

        // Remove and recycle sticky header.
        removeView(stickyHeader);
        if (recycler != null) {
            recycler.recycleView(stickyHeader);
        }
    }

    /**
     * Returns true when {@code view} is a valid anchor, ie. the first view to be valid and visible.
     */
    private boolean isViewValidAnchor(View view, RecyclerView.LayoutParams params) {
        if (!params.isItemRemoved() && !params.isViewInvalid()) {
            if (getOrientation() == VERTICAL) {
                if (getReverseLayout()) {
                    return view.getTop() + view.getTranslationY() <= getHeight() + mTranslationY;
                } else {
                    return view.getBottom() - view.getTranslationY() >= mTranslationY;
                }
            } else {
                if (getReverseLayout()) {
                    return view.getLeft() + view.getTranslationX() <= getWidth() + mTranslationX;
                } else {
                    return view.getRight() - view.getTranslationX() >= mTranslationX;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Returns true when the {@code view} is at the edge of the parent {@link RecyclerView}.
     */
    private boolean isViewOnBoundary(View view) {
        if (getOrientation() == VERTICAL) {
            if (getReverseLayout()) {
                return view.getBottom() - view.getTranslationY() > getHeight() + mTranslationY;
            } else {
                return view.getTop() + view.getTranslationY() < mTranslationY;
            }
        } else {
            if (getReverseLayout()) {
                return view.getRight() - view.getTranslationX() > getWidth() + mTranslationX;
            } else {
                return view.getLeft() + view.getTranslationX() < mTranslationX;
            }
        }
    }

    /**
     * Returns the position in the Y axis to position the header appropriately, depending on orientation, direction and
     * {@link android.R.attr#clipToPadding}.
     */
    private float getY(View headerView, View nextHeaderView) {
        if (getOrientation() == VERTICAL) {
            float y = mTranslationY;
            if (getReverseLayout()) {
                y += getHeight() - headerView.getHeight();
            }
            if (nextHeaderView != null) {
                if (getReverseLayout()) {
                    y = Math.max(nextHeaderView.getBottom(), y);
                } else {
                    y = Math.min(nextHeaderView.getTop() - headerView.getHeight(), y);
                }
            }
            return y;
        } else {
            return mTranslationY;
        }
    }

    /**
     * Returns the position in the X axis to position the header appropriately, depending on orientation, direction and
     * {@link android.R.attr#clipToPadding}.
     */
    private float getX(View headerView, View nextHeaderView) {
        if (getOrientation() != VERTICAL) {
            float x = mTranslationX;
            if (getReverseLayout()) {
                x += getWidth() - headerView.getWidth();
            }
            if (nextHeaderView != null) {
                if (getReverseLayout()) {
                    x = Math.max(nextHeaderView.getRight(), x);
                } else {
                    x = Math.min(nextHeaderView.getLeft() - headerView.getWidth(), x);
                }
            }
            return x;
        } else {
            return mTranslationX;
        }
    }

    /**
     * Finds the header index of {@code position} in {@code mHeaderPositions}.
     */
    private int findHeaderIndex(int position) {
        int low = 0;
        int high = mHeaderPositions.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            if (mHeaderPositions.get(middle) > position) {
                high = middle - 1;
            } else if (mHeaderPositions.get(middle) < position) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Finds the header index of {@code position} or the one before it in {@code mHeaderPositions}.
     */
    private int findHeaderIndexOrBefore(int position) {
        int low = 0;
        int high = mHeaderPositions.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            if (mHeaderPositions.get(middle) > position) {
                high = middle - 1;
            } else if (middle < mHeaderPositions.size() - 1 && mHeaderPositions.get(middle + 1) <= position) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Finds the header index of {@code position} or the one next to it in {@code mHeaderPositions}.
     */
    private int findHeaderIndexOrNext(int position) {
        int low = 0;
        int high = mHeaderPositions.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            if (middle > 0 && mHeaderPositions.get(middle - 1) >= position) {
                high = middle - 1;
            } else if (mHeaderPositions.get(middle) < position) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    private void setPendingScroll(int position, int offset) {
        mPendingScrollPosition = position;
        mPendingScrollOffset = offset;
    }

    /**
     * Handles header positions while adapter changes occur.
     *
     * This is used in detriment of {@link RecyclerView.LayoutManager}'s callbacks to control when they're received.
     */
    private class HeaderPositionsAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            // There's no hint at what changed, so go through the adapter.
            mHeaderPositions.clear();
            int itemCount = mAdapter.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                if (mAdapter.isStickyHeader(i)) {
                    mHeaderPositions.add(i);
                }
            }

            // Remove sticky header immediately if the entry it represents has been removed. A layout will follow.
            if (mStickyHeader != null && !mHeaderPositions.contains(mStickyHeaderPosition)) {
                scrapStickyHeader(null);
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            // Shift headers below down.
            int headerCount = mHeaderPositions.size();
            if (headerCount > 0) {
                for (int i = findHeaderIndexOrNext(positionStart); i != -1 && i < headerCount; i++) {
                    mHeaderPositions.set(i, mHeaderPositions.get(i) + itemCount);
                }
            }

            // Add new headers.
            for (int i = positionStart; i < positionStart + itemCount; i++) {
                if (mAdapter.isStickyHeader(i)) {
                    int headerIndex = findHeaderIndexOrNext(i);
                    if (headerIndex != -1) {
                        mHeaderPositions.add(headerIndex, i);
                    } else {
                        mHeaderPositions.add(i);
                    }
                }
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            int headerCount = mHeaderPositions.size();
            if (headerCount > 0) {
                // Remove headers.
                for (int i = positionStart + itemCount - 1; i >= positionStart; i--) {
                    int index = findHeaderIndex(i);
                    if (index != -1) {
                        mHeaderPositions.remove(index);
                        headerCount--;
                    }
                }

                // Remove sticky header immediately if the entry it represents has been removed. A layout will follow.
                if (mStickyHeader != null && !mHeaderPositions.contains(mStickyHeaderPosition)) {
                    scrapStickyHeader(null);
                }

                // Shift headers below up.
                for (int i = findHeaderIndexOrNext(positionStart + itemCount); i != -1 && i < headerCount; i++) {
                    mHeaderPositions.set(i, mHeaderPositions.get(i) - itemCount);
                }
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            // Shift moved headers by toPosition - fromPosition.
            // Shift headers in-between by -itemCount (reverse if upwards).
            int headerCount = mHeaderPositions.size();
            if (headerCount > 0) {
                if (fromPosition < toPosition) {
                    for (int i = findHeaderIndexOrNext(fromPosition); i != -1 && i < headerCount; i++) {
                        int headerPos = mHeaderPositions.get(i);
                        if (headerPos >= fromPosition && headerPos < fromPosition + itemCount) {
                            mHeaderPositions.set(i, headerPos - (toPosition - fromPosition));
                            sortHeaderAtIndex(i);
                        } else if (headerPos >= fromPosition + itemCount && headerPos <= toPosition) {
                            mHeaderPositions.set(i, headerPos - itemCount);
                            sortHeaderAtIndex(i);
                        } else {
                            break;
                        }
                    }
                } else {
                    for (int i = findHeaderIndexOrNext(toPosition); i != -1 && i < headerCount; i++) {
                        int headerPos = mHeaderPositions.get(i);
                        if (headerPos >= fromPosition && headerPos < fromPosition + itemCount) {
                            mHeaderPositions.set(i, headerPos + (toPosition - fromPosition));
                            sortHeaderAtIndex(i);
                        } else if (headerPos >= toPosition && headerPos <= fromPosition) {
                            mHeaderPositions.set(i, headerPos + itemCount);
                            sortHeaderAtIndex(i);
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        private void sortHeaderAtIndex(int index) {
            int headerPos = mHeaderPositions.remove(index);
            int headerIndex = findHeaderIndexOrNext(headerPos);
            if (headerIndex != -1) {
                mHeaderPositions.add(headerIndex, headerPos);
            } else {
                mHeaderPositions.add(headerPos);
            }
        }
    }

    public static class SavedState implements Parcelable {
        private Parcelable superState;
        private int pendingScrollPosition;
        private int pendingScrollOffset;

        public SavedState() {
        }

        public SavedState(Parcel in) {
            superState = in.readParcelable(SavedState.class.getClassLoader());
            pendingScrollPosition = in.readInt();
            pendingScrollOffset = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeParcelable(superState, flags);
            dest.writeInt(pendingScrollPosition);
            dest.writeInt(pendingScrollOffset);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
