package com.STPL.samcomidynamoutility;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class CardOptionSelectionDialog extends DialogFragment implements View.OnClickListener {
    private static final String DIALOG_OPEN = "CardOptionSelectionDialog";
    private TextView textViewOptionInsert, textViewOptionSwipe, textViewMessageChip, textViewCancel;
    private CardTypeSelectionCallback cardTypeSelectionCallback;

    public static CardOptionSelectionDialog newInstance() {
        CardOptionSelectionDialog fragment = new CardOptionSelectionDialog();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.card_option_selection_dialog, container, false);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            getDialog().getWindow().setGravity(Gravity.CENTER);
            getDialog().setCanceledOnTouchOutside(false);
        }
        return view;
    }


    public void setCalBacks(CardTypeSelectionCallback cardTypeSelectionCallback) {
        this.cardTypeSelectionCallback = cardTypeSelectionCallback;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }


    public void init(View view) {
        textViewOptionInsert = view.findViewById(R.id.textViewOptionInsert);
        textViewOptionSwipe = view.findViewById(R.id.textViewOptionSwipe);
        textViewMessageChip = view.findViewById(R.id.textViewMessageChip);
        textViewCancel = view.findViewById(R.id.textViewCancel);
        setOnClickListener();
    }

    public void setOnClickListener() {
        textViewOptionInsert.setOnClickListener(this);
        textViewOptionSwipe.setOnClickListener(this);
        textViewMessageChip.setOnClickListener(this);
        textViewCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.textViewOptionInsert) {
            cardTypeSelectionCallback.cardType("CHIP");
            getDialog().dismiss();
        } else if (id == R.id.textViewOptionSwipe) {
            cardTypeSelectionCallback.cardType("SWIPE");
            getDialog().dismiss();
        } else if (id == R.id.textViewMessageChip) {
            cardTypeSelectionCallback.cardType("TAP");
            getDialog().dismiss();
        } else if (id == R.id.textViewCancel) {
            getDialog().dismiss();
        }
    }

    public interface CardTypeSelectionCallback {
        void cardType(String types);
    }
}
