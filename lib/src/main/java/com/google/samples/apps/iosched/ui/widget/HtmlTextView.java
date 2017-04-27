package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.util.AttributeSet;

import org.apache.commons.lang3.StringEscapeUtils;

public class HtmlTextView extends AppCompatTextView {
    public HtmlTextView(Context context) {
        this(context, null);
    }

    public HtmlTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HtmlTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setHtmlText(String text) {
        if (text != null) {
            setText(Html.fromHtml(StringEscapeUtils.
                    unescapeJava(StringEscapeUtils.
                            unescapeHtml4(text))));
        } else {
            setText("");
        }
    }
}