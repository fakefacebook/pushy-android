/*
 *
 * Copyright 2017 Michael A Updike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.weebly.opus1269.clipman.ui.clipviewer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.weebly.opus1269.clipman.R;
import com.weebly.opus1269.clipman.app.AppUtils;
import com.weebly.opus1269.clipman.model.ClipItem;

import java.io.Serializable;
import java.text.Collator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fragment containing a view of a ClipItem's text.
 *
 */
public class ClipViewerFragment extends Fragment implements View.OnClickListener {
    public static final String ARG_CLIP_ITEM = AppUtils.PACKAGE_PATH + "ARG_CLIP_ITEM";
    public static final String ARG_HIGHLIGHT = AppUtils.PACKAGE_PATH + "ARG_HIGHLIGHT";

    private static final String STATE_CLIP_ITEM = "clip";
    private static final String STATE_CLIP_VIEWABLE = "viewable";
    private static final String STATE_CLIP_HIGHLIGHT = "highlight";

    // Activities implement this to get notified of clip changes
    public interface OnClipChanged {
        void onClipChanged(ClipItem clipItem);
    }

    private OnClipChanged mOnClipChanged = null;

    /**
     * saved instance state
     */

    // The clip we are viewing
    private ClipItem mClipItem = null;

    // The text to be highlighted
    private String mHighlightText = "";

    // Flag to indicate if we are viewable (as opposed to recreated from a savedInstanceState
    // but will not be seen.
    private boolean mIsViewable = true;


    /** factory method to create new fragment
     *
     * @param item ClipItem to view
     * @param highlight text to highlight
     * @return new ClipViewerFragment
     */
    public static ClipViewerFragment newInstance(Serializable item, String highlight) {
        final ClipViewerFragment fragment = new ClipViewerFragment();

        final Bundle args = new Bundle();
        args.putSerializable(ARG_CLIP_ITEM, item);
        args.putString(ARG_HIGHLIGHT, highlight);

        fragment.setArguments(args);

        return fragment;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Superclass overrides
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            mClipItem = (ClipItem) savedInstanceState.getSerializable(STATE_CLIP_ITEM);
            mIsViewable = savedInstanceState.getBoolean(STATE_CLIP_VIEWABLE);
            mHighlightText = savedInstanceState.getString(STATE_CLIP_HIGHLIGHT);
        } else {
            mClipItem = new ClipItem();
            mHighlightText = "";
            mIsViewable = true;
        }
        mOnClipChanged.onClipChanged(mClipItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            mIsViewable = false;
            setHasOptionsMenu(false);
            return null;
        }

        final View rootView = inflater.inflate(R.layout.fragment_clip_viewer, container, false);

        setText(mClipItem.getText());

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final Activity activity = getActivity();
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mOnClipChanged = (OnClipChanged) activity;
        } catch (final ClassCastException ignore) {
            throw new ClassCastException(activity.getLocalClassName() + " must implement OnClipChanged");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments().containsKey(ARG_CLIP_ITEM)) {
            final ClipItem clipItem = (ClipItem) getArguments().getSerializable(ARG_CLIP_ITEM);
            setClipItem(clipItem);
        }

        if (getArguments().containsKey(ARG_HIGHLIGHT)) {
            final String highlightText = getArguments().getString(ARG_HIGHLIGHT);
            setHighlightText(highlightText);
        }

        setupRemoteDevice();

        final Activity activity = getActivity();
        final FloatingActionButton fab = (FloatingActionButton) activity.findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_CLIP_ITEM, mClipItem);
        outState.putString(STATE_CLIP_HIGHLIGHT, mHighlightText);
        outState.putBoolean(STATE_CLIP_VIEWABLE, mIsViewable);

        super.onSaveInstanceState(outState);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implement View.OnClickListener
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onClick(View v) {
        mClipItem.doShare(v);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public methods
    ///////////////////////////////////////////////////////////////////////////

    public ClipItem getClipItemClone() {
        return new ClipItem(mClipItem);
    }

    public void setClipItem(ClipItem clipItem) {

        if (!Collator.getInstance().equals(clipItem.getText(), mClipItem.getText())) {
            // skip repaint if text is same
            final TextView textView = (TextView) getActivity().findViewById(R.id.clipViewerText);
            if (textView != null) {
                //force layout change animation
                textView.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                setText(clipItem.getText());
            }
        }

        mClipItem = clipItem;

        setupRemoteDevice();

        mOnClipChanged.onClipChanged(mClipItem);
    }

    Boolean copyToClipboard() {
        Boolean ret = false;

        if (!TextUtils.isEmpty(mClipItem.getText())) {
            mClipItem.setRemote(false);
            setClipItem(mClipItem);
            mClipItem.copyToClipboard();
            ret = true;
            View view = getView();
            if (view != null) {
                Snackbar.make(view, R.string.clipboard_copy, Snackbar.LENGTH_SHORT).show();
            }
        }

        return ret;
    }

    /**
     * Highlight all occurrences of the given String
     *
     * @param highlightText the text to highlight (case insensitive)
     */
    public void setHighlightText(String highlightText) {
        mHighlightText = highlightText;

        final TextView textView = (TextView) getActivity().findViewById(R.id.clipViewerText);
        if (textView == null) {
            return;
        }

        if (TextUtils.isEmpty(highlightText)) {
            // make sure to reset spans
            setText(mClipItem.getText());
        } else {
            final String text = mClipItem.getText();
            final Spannable spanText = Spannable.Factory.getInstance().newSpannable(text);
            final int color = ContextCompat.getColor(getContext(), R.color.search_highlight);
            final Pattern p = Pattern.compile(highlightText, Pattern.CASE_INSENSITIVE);
            final Matcher m = p.matcher(text);
            final int length = highlightText.length();
            while (m.find()) {
                final int start = m.start();
                final int stop = start + length;
                //noinspection ObjectAllocationInLoop
                spanText.setSpan(new BackgroundColorSpan(color), start, stop,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            setText(spanText);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private void setupRemoteDevice() {
        if(!mIsViewable) {
            return;
        }

        final TextView textView =
                (TextView) getActivity().findViewById(R.id.remoteDevice);
        final View divider =
                getActivity().findViewById(R.id.remoteDivider);

        if (mClipItem.isRemote() && !AppUtils.isDualPane()) {
            textView.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            textView.setText(getString(R.string.remote_device_fmt, mClipItem.getDevice()));
        } else {
            textView.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
    }

    /**
     * Set the TextView text
     *
     */
    private void setText(String text) {
        final TextView textView =
                (TextView) getActivity().findViewById(R.id.clipViewerText);
        if (textView == null) {
            return;
        }

        textView.setText(text);
        linkifyTextView(textView);
    }

    /**
     * Set the TextView text
     *
     */
    private void setText(Spannable text) {
        final TextView textView =
                (TextView) getActivity().findViewById(R.id.clipViewerText);
        if (textView == null) {
            return;
        }

        textView.setText(text);
        linkifyTextView(textView);
    }

    /**
     * Setup selectable links for TextView
     * autoLink is pretty buggy
     *
     */
    private void linkifyTextView(TextView textView) {
        // http://stackoverflow.com/a/16003280/4468645
        Linkify.addLinks(textView, Linkify.ALL);
        textView.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        textView.setTextIsSelectable(true);
    }
}
