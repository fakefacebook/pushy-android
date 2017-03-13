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

package com.weebly.opus1269.clipman.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.weebly.opus1269.clipman.R;

/**
 * Modal dialog to delete all items in the main clip list
 */
public class DeleteDialogFragment extends DialogFragment {

    // Flag to indicate if the favorite items should be deleted as well
    private Boolean mDeleteFavs = false;
    // Use this instance of the interface to deliver action events
    private DeleteDialogListener mListener = null;

    // Override the Fragment.onAttach() method to instantiate the listener
    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        Activity activity = getActivity();
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DeleteDialogListener so we can send events to the host
            mListener = (DeleteDialogListener) activity;
        } catch (final ClassCastException ignored) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity + " must implement DeleteDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete_all_question)
                // only has one item
                .setMultiChoiceItems(R.array.favs, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        mDeleteFavs = isChecked;
                    }
                })
                .setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // tell the listener to delete the items
                        mListener.onDeleteDialogPositiveClick(mDeleteFavs);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User cancelled the dialog
                        dialog.cancel();
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DeleteDialogListener {
        void onDeleteDialogPositiveClick(Boolean deleteFavs);
    }
}