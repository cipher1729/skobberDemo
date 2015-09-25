package com.routecar.UI;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

import java.net.Authenticator;
import java.util.HashMap;

/**
 * Created by chandanj on 9/24/2015.
 */
public class CustomAutoCompleteTextView extends AutoCompleteTextView{

    public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Returns the place description corresponding to the selected item */
    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        /** Each item in the autocompetetextview suggestion list is a hashmap object */
        HashMap<String, String> hm = (HashMap<String, String>) selectedItem;
        return hm.get("description");
    }
}
