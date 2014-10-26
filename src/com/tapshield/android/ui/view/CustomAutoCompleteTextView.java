package com.tapshield.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class CustomAutoCompleteTextView extends AutoCompleteTextView {

	private SelectionConverter mSelectionConverter;
	
	public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setSelectionConverter(SelectionConverter selectionConverter) {
		mSelectionConverter = selectionConverter;
	}

	@Override
	protected CharSequence convertSelectionToString(Object selectedItem) {
		if (mSelectionConverter == null) {
			throw new NullPointerException("SelectionConverter object is not set." +
					" Call setSelectionConverter(SelectionConverter) to set it.");
		} else {
			return mSelectionConverter.toString(selectedItem);
		}
	}
	
	public interface SelectionConverter {
		String toString(Object selectedItem);
	}
}
