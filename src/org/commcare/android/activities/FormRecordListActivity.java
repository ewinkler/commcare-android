/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.commcare.android.activities;

import org.commcare.android.R;
import org.commcare.android.adapters.IncompleteFormListAdapter;
import org.commcare.android.application.CommCareApplication;
import org.commcare.android.models.FormRecord;
import org.commcare.android.models.User;
import org.commcare.android.tasks.DataPullListener;
import org.commcare.android.tasks.DataPullTask;
import org.commcare.android.tasks.FormRecordCleanupTask;
import org.commcare.android.util.AndroidCommCarePlatform;
import org.commcare.android.util.SessionUnavailableException;
import org.commcare.android.view.IncompleteFormRecordView;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


public class FormRecordListActivity extends ListActivity {
	
	private static final int DIALOG_PROCESS = 1;
	private ProgressDialog mProgressDialog;
	
	private static final int OPEN_RECORD = Menu.FIRST;
	private static final int DELETE_RECORD = Menu.FIRST  + 1;
	
	private static final int DOWNLOAD_FORMS = Menu.FIRST;
	
	private AndroidCommCarePlatform platform;
	
	private IncompleteFormListAdapter adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
	        platform = CommCareApplication._().getCommCarePlatform();
	        setContentView(R.layout.suite_menu_layout);
	
	        adapter = new IncompleteFormListAdapter(this, platform);
	        
	        String statusFilter = null;
	        
	        if(this.getIntent().hasExtra(FormRecord.META_STATUS)) {
	        	statusFilter = this.getIntent().getStringExtra(FormRecord.META_STATUS);
	        	if(statusFilter.equals(FormRecord.STATUS_INCOMPLETE)) {
	        		setTitle(getString(R.string.app_name) + " > " + "Incomplete Forms");
	        	} else {
	        		setTitle(getString(R.string.app_name) + " > " + "Saved Forms");
	        	}
	        } else {
	        	setTitle(getString(R.string.app_name) + " > " + "Saved Forms");
	        }
	        
	        if(statusFilter != null) {
	        	adapter.setFormFilter(statusFilter);
	        }
	        this.registerForContextMenu(this.getListView());
	        refreshView();
        } catch(SessionUnavailableException sue) {
        	//TODO: session is dead, login and return
        }
    }


    /**
     * Get form list from database and insert into view.
     */
    private void refreshView() {
    	adapter.resetRecords();
    	setListAdapter(adapter);
    }


    /**
     * Stores the path of selected form and finishes.
     */
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
    	returnItem(position);
    }
    
    private void returnItem(int position) {
    	FormRecord value = (FormRecord)getListAdapter().getItem(position);

    	if(FormRecord.STATUS_SAVED.equals(adapter.getFilter())) {
    		Intent i = new Intent("org.odk.collect.android.action.FormEntry");
    		i.putExtra("formpath", CommCareApplication._().getCommCarePlatform().getFormContentUri(value.getFormNamespace()));
    		
    		i.putExtra("instancepath", value.getPath());
    		i.putExtra("encryptionkey", value.getAesKey());
    		i.putExtra("encryptionkeyalgo", "AES");
    		
    		i.putExtra("readonlyform", true);
    		
    		startActivity(i);
    		return;
    	} else {
	        // We want to actually launch an interactive form entry.
	        Intent i = new Intent();
	        i.putExtra(FormRecord.STORAGE_KEY, value.getID());
	        setResult(RESULT_OK, i);
	
	        finish();
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        IncompleteFormRecordView ifrv = (IncompleteFormRecordView)adapter.getView(info.position, null, null);
        menu.setHeaderTitle(ifrv.mPrimaryTextView.getText() + " (" + ifrv.mRightTextView.getText() + ")");
        
        menu.add(Menu.NONE, OPEN_RECORD, OPEN_RECORD, "Open");
        menu.add(Menu.NONE, DELETE_RECORD, DELETE_RECORD, "Delete Entry");        
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	try {
	      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	      switch(item.getItemId()) {
	      case OPEN_RECORD:
	    	  returnItem(info.position);
	    	  return true;
	      case DELETE_RECORD:
	    	  new FormRecordCleanupTask(this).wipeRecord(CommCareApplication._().getStorage(FormRecord.STORAGE_KEY, FormRecord.class).read((int)info.id));
	    	  this.getListView().post(new Runnable() { public void run() {adapter.notifyDataSetChanged();}});
	      }
	      
	      return true;
    	} catch(SessionUnavailableException sue) {
    		//TODO: Login and try again
    		return true;
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean parent = super.onCreateOptionsMenu(menu);
        if(!FormRecord.STATUS_INCOMPLETE.equals(adapter.getFilter())) {
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    		String source = prefs.getString("form-record-url", this.getString(R.string.form_record_url));
    		
    		//If there's nowhere to fetch forms from, we can't really go fetch them
    		if(!(source == null || source.equals(""))) {
    			menu.add(0, DOWNLOAD_FORMS, 0, "Fetch Forms from Server").setIcon(android.R.drawable.ic_menu_rotate);
    		}
	        return true;
        }
        return parent;
    }

    TextToSpeech mTts;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DOWNLOAD_FORMS:
            	this.showDialog(DIALOG_PROCESS);
            	
        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            	
            	User u = CommCareApplication._().getSession().getLoggedInUser();
            	
            	String source = prefs.getString("form-record-url", this.getString(R.string.form_record_url));
            	
            	//We should go digest auth this user on the server and see whether to pull them
				//down.
            	DataPullTask pull = new DataPullTask(u.getUsername(),u.getCachedPwd(), source, "", this);
            	pull.setPullListener(new DataPullListener() {

					public void finished(int status) {
						switch(status) {
						case DataPullTask.DOWNLOAD_SUCCESS:
							mProgressDialog.setMessage("Forms downloaded. Processing...");
							FormRecordCleanupTask task = new FormRecordCleanupTask(FormRecordListActivity.this) {
	
								@Override
								protected void onPostExecute(Integer result) {
									super.onPostExecute(result);
									unlock();
									mProgressDialog.setMessage("Forms Processed.");
									FormRecordListActivity.this.dismissDialog(DIALOG_PROCESS);
									FormRecordListActivity.this.refreshView();
								}
								
								@Override
								protected void onProgressUpdate(Integer ... values) {
									if(values[0] < 0) {
										if(values[0] == FormRecordCleanupTask.STATUS_CLEANUP) {
											mProgressDialog.setMessage("Forms Processed. Cleaning up form records...");
										}
									}
									else {
										mProgressDialog.setMessage("Forms downloaded. Processing " + values[0] + " of " + values[1] +"...");
									}
								}
								
								
							};
							task.execute();
							break;
						case DataPullTask.UNKNOWN_FAILURE:
							unlock();
							Toast.makeText(FormRecordListActivity.this, "Failure retrieving or processing data, please try again later...", Toast.LENGTH_LONG).show();
							FormRecordListActivity.this.dismissDialog(DIALOG_PROCESS);
							break;
						case DataPullTask.AUTH_FAILED:
							unlock();
							Toast.makeText(FormRecordListActivity.this, "Authentication failure. Please logout and resync with the server and try again.", Toast.LENGTH_LONG).show();
							FormRecordListActivity.this.dismissDialog(DIALOG_PROCESS);
							break;
						case DataPullTask.BAD_DATA:
							unlock();
							Toast.makeText(FormRecordListActivity.this, "Bad data from server. Please talk with your supervisor.", Toast.LENGTH_LONG).show();
							FormRecordListActivity.this.dismissDialog(DIALOG_PROCESS);
							break;
						case DataPullTask.UNREACHABLE_HOST:
							unlock();
							Toast.makeText(FormRecordListActivity.this, "Couldn't contact server, please check your network connection and try again.", Toast.LENGTH_LONG).show();
							FormRecordListActivity.this.dismissDialog(DIALOG_PROCESS);
							break;
							
						}
					}

					public void progressUpdate(Integer ... progress) {
						switch(progress[0]){
						case DataPullTask.PROGRESS_AUTHED:
							mProgressDialog.setMessage("Authed with server, downloading forms" + (progress[1] == 0 ? "" : " (" +progress[1] + ")"));
							break;
						}
					}
            		
            	});
            	
            	wakelock();
            	
            	pull.execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Don't ever lose this reference
    private static WakeLock wakelock;
    
    private void wakelock() {
    	if(wakelock != null) {
    		if(wakelock.isHeld()) {
    			wakelock.release();
    		}
    	}
    	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CommCareFormSync");
    	//Twenty minutes max.
    	wakelock.acquire(1000*60*20);
    }
    
    private void unlock() {
    	if(wakelock != null) {
    		wakelock.release();
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	//Make sure we're not holding onto the wake lock still, no matter what
    	unlock();
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PROCESS:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle("Fetching Old Forms");
                mProgressDialog.setMessage("Connecting to server...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                return mProgressDialog;
        }
        return null;
    }
}
