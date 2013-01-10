/**
 * 
 */
package org.commcare.android.view;

import org.commcare.android.models.Entity;
import org.commcare.dalvik.R;
import org.commcare.android.util.DetailCalloutListener;
import org.commcare.suite.model.Detail;
import org.commcare.util.CommCareSession;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author ctsims
 *
 */
public class EntityDetailView extends FrameLayout {
	
	private TextView label;
	private TextView data;
	private TextView spacer;
	private Button callout;
	
	private View addressView;
	private Button addressButton;
	private TextView addressText;
	
	private View valuePane;
	
	private View currentView;
	private LinearLayout detailRow;
	
	private LinearLayout.LayoutParams origValue;
	private LinearLayout.LayoutParams origLabel;
	
	private LinearLayout.LayoutParams fill;
	
	int current = TEXT;
	private static final int TEXT = 0;
	private static final int PHONE = 1;
	private static final int ADDRESS = 2;
	
	
	DetailCalloutListener listener;

	public EntityDetailView(Context context, CommCareSession session, Detail d, Entity e, int index) {
		super(context);
		detailRow = (LinearLayout)View.inflate(context, R.layout.component_entity_detail_item, null);
		
        label = (TextView)detailRow.findViewById(R.id.detail_type_text);
        spacer = (TextView)detailRow.findViewById(R.id.entity_detail_spacer);
	    
	    data = (TextView)detailRow.findViewById(R.id.detail_value_text);
	    currentView = data;
	    
	    valuePane = detailRow.findViewById(R.id.detail_value_pane);
	    
	    callout = (Button)detailRow.findViewById(R.id.detail_value_phone);
	    //TODO: Still useful?
	    //callout.setInputType(InputType.TYPE_CLASS_PHONE);
	    
	    addressView = (View)detailRow.findViewById(R.id.detail_address_view);
	    addressText = (TextView)addressView.findViewById(R.id.detail_address_text);
	    addressButton = (Button)addressView.findViewById(R.id.detail_address_button);
	    
	    origLabel = (LinearLayout.LayoutParams)label.getLayoutParams();
	    origValue = (LinearLayout.LayoutParams)valuePane.getLayoutParams();
	    
	    fill = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	    
	    this.addView(detailRow, FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
	    setParams(session, d, e, index);
	}
	
	public void setCallListener(final DetailCalloutListener listener) {
		this.listener = listener;
	}

	public void setParams(CommCareSession session, Detail d, Entity e, int index) {
		String labelText = d.getFields()[index].getHeader().evaluate();
		label.setText(labelText);
		spacer.setText(labelText);
		
		boolean veryLong = false;
		
		if("phone".equals(d.getTemplateForms()[index])) {
			callout.setText(e.getField(index));
			if(current != PHONE) {
				callout.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						listener.callRequested(callout.getText().toString());				
					}
					
				});
				currentView.setVisibility(View.GONE);
				callout.setVisibility(View.VISIBLE);
				this.removeView(currentView);
				currentView = callout;
				current = PHONE;
			}
		} else if("address".equals(d.getTemplateForms()[index])) {
			final String address = e.getField(index);
			addressText.setText(address);
			if(current != ADDRESS) {
				addressButton.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						listener.addressRequested(address);
					}
					
				});
				
				currentView.setVisibility(View.GONE);
				addressView.setVisibility(View.VISIBLE);
				currentView = addressView;
				current = ADDRESS;
			}
		} else {
			String text = e.getField(index);
			data.setText(text);
			if(text != null && text.length() > this.getContext().getResources().getInteger(R.integer.detail_size_cutoff)) {
				veryLong = true;
			}
			if(current != TEXT) {
				currentView.setVisibility(View.GONE);
				data.setVisibility(View.VISIBLE);
				currentView = data;
				current = TEXT;
			}
		}
		
		if(veryLong) {
			
			detailRow.setOrientation(LinearLayout.VERTICAL);
			spacer.setVisibility(View.GONE);
			label.setLayoutParams(fill);
			valuePane.setLayoutParams(fill);
			
		} else {
			
			if(detailRow.getOrientation() != LinearLayout.HORIZONTAL) {
				
				detailRow.setOrientation(LinearLayout.HORIZONTAL);
				spacer.setVisibility(View.INVISIBLE);
				label.setLayoutParams(origLabel);
				valuePane.setLayoutParams(origValue);
			}
		}
	}
}
