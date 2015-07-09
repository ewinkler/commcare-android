package org.commcare.dalvik.activities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.commcare.android.adapters.PdfPrintDocumentAdapter;
import org.commcare.android.tasks.TemplatePrinterTask;
import org.commcare.android.tasks.TemplatePrinterTask.PopulateListener;
import org.commcare.android.util.TemplatePrinterUtils;
import org.commcare.dalvik.R;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.dalvik.preferences.CommCarePreferences;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintManager;
import android.util.Log;

/**
 * Intermediate activity which populates a .DOCX/.ODT template
 * with data before sending it off to a document viewer app
 * capable of printing.
 * 
 * @author Richard Lu, amstone
 */
public class TemplatePrinterActivity extends Activity implements PopulateListener {

    //Unique name to use for the print job name
    private static String mJobName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_printer);

        //Check to make sure we are targeting API 19 or above, which is where print is supported
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            showErrorDialog(getString(R.string.print_not_supported));
        }

        Bundle data = getIntent().getExtras();
        //Check to make sure key-value data has been passed with the intent
        if (data == null) {
            showErrorDialog(R.string.no_data);
        }

        // Get the case number, which may be sent with the intent bundle -- For purposes of
        // creating the job name. If not included, will just not be used.
        String caseNum = data.getString("cc:case_num");

        //Check if a doc location is coming in from the Intent
        //Will return a reference of format jr://... if it has been set
        String path = data.getString("cc:print_template_reference");
        if (path != null) {
            try {
                path = ReferenceManager._().DeriveReference(path).getLocalURI();
                preparePrintDoc(path, caseNum);
            } catch (InvalidReferenceException e) {
                showErrorDialog(getString(R.string.template_invalid, path));
            }
        } else {
            //Try to use the document location that was set in Settings menu
            SharedPreferences prefs = CommCareApplication._().getCurrentApp().getAppPreferences();
            path = prefs.getString(CommCarePreferences.PRINT_DOC_LOCATION, "");
            if ("".equals(path)) {
                showErrorDialog(getString(R.string.template_not_set));
            } else {
                preparePrintDoc(path, caseNum);
            }
        }
    }

    private void preparePrintDoc(String path, String caseNum) {
        String extension = TemplatePrinterUtils.getExtension(path);
        File templateFile = new File(path);

        if (TemplatePrinterTask.DocTypeEnum.isSupportedExtension(extension) && templateFile.exists()) {

            generateJobName(templateFile, caseNum);

            new TemplatePrinterTask(
                    templateFile,
                    getIntent().getExtras(),
                    this
            ).execute();
        } else {
            showErrorDialog(getString(R.string.template_invalid, path));
        }
    }

    private void generateJobName(File templateFile, String caseNum) {
        String inputName = templateFile.getName().substring(0, templateFile.getName().lastIndexOf('.'));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = sdf.format(new Date());
        if (caseNum != null) {
            mJobName = inputName + "_" + caseNum + "_" + dateString;
        } else {
            mJobName = inputName + "_" + dateString;
        }
    }

    @Override
    public void onError(String message) {
        showErrorDialog(message);
    }

    @Override
    public void onFinished(File result) {
        executePrint(result);
    }

    private void showErrorDialog(int messageResId) {
        showErrorDialog(getString(messageResId));
    }

    /**
     * Displays an error dialog with the specified message.
     * Activity will quit upon exiting the dialog.
     *
     * @param message Error message
     */
    private void showErrorDialog(String message) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setTitle(R.string.error_occured)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        }
                );
        dialogBuilder.show();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void executePrint(File document) {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PdfPrintDocumentAdapter adapter = new PdfPrintDocumentAdapter(this, document.getPath(),
                mJobName);
        printManager.print(mJobName, adapter, null);
    }

}
