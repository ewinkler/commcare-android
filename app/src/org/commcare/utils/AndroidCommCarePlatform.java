package org.commcare.utils;

import org.commcare.CommCareApp;
import org.commcare.CommCareApplication;
import org.commcare.android.database.app.models.FormDefRecord;
import org.commcare.engine.resource.AndroidResourceTable;
import org.commcare.models.database.SqlStorage;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * @author ctsims
 */
public class AndroidCommCarePlatform extends CommCarePlatform {

    private final Hashtable<String, Integer> xmlnstable;
    private ResourceTable global;
    private ResourceTable upgrade;
    private ResourceTable recovery;

    private Profile profile;
    private final Vector<Suite> installedSuites;
    private final CommCareApp app;
    private String mUpdateInfoFormXmlns;

    public AndroidCommCarePlatform(int majorVersion, int minorVersion, CommCareApp app) {
        super(majorVersion, minorVersion);
        xmlnstable = new Hashtable<>();
        installedSuites = new Vector<>();
        this.app = app;
    }

    public void registerXmlns(String xmlns, Integer formDefId) {
        xmlnstable.put(xmlns, formDefId);
    }

    public Set<String> getInstalledForms() {
        return xmlnstable.keySet();
    }

    public int getFormDefId(String xFormNamespace) {
        if (xmlnstable.containsKey(xFormNamespace)) {
            return xmlnstable.get(xFormNamespace);
        }

        // Search through manually?
        return -1;
    }

    public ResourceTable getGlobalResourceTable() {
        if (global == null) {
            global = new AndroidResourceTable(app.getStorage("GLOBAL_RESOURCE_TABLE", Resource.class), new AndroidResourceInstallerFactory());
        }
        return global;
    }

    public ResourceTable getUpgradeResourceTable() {
        if (upgrade == null) {
            upgrade = new AndroidResourceTable(app.getStorage("UPGRADE_RESOURCE_TABLE", Resource.class), new AndroidResourceInstallerFactory());
        }
        return upgrade;
    }

    public ResourceTable getRecoveryTable() {
        if (recovery == null) {
            recovery = new AndroidResourceTable(app.getStorage("RECOVERY_RESOURCE_TABLE", Resource.class), new AndroidResourceInstallerFactory());
        }
        return recovery;
    }

    @Override
    public Profile getCurrentProfile() {
        return profile;
    }

    @Override
    public Vector<Suite> getInstalledSuites() {
        return installedSuites;
    }

    @Override
    public void setProfile(Profile p) {
        this.profile = p;
    }

    @Override
    public void registerSuite(Suite s) {
        this.installedSuites.add(s);
    }

    @Override
    public void initialize(ResourceTable global, boolean isUpgrade) {
        this.profile = null;
        this.installedSuites.clear();
        // We also need to clear any _resource table_ linked localization files which may have
        // been registered from another app, or from a pre-install location.
        CommCareApplication.instance().initializeDefaultLocalizerData();

        super.initialize(global, isUpgrade);
    }

    public IStorageUtilityIndexed<FormInstance> getFixtureStorage() {
        return app.getFileBackedStorage("fixture", FormInstance.class);
    }

    public SqlStorage<FormDefRecord> getFormDefStorage() {
        return app.getStorage(FormDefRecord.class);
    }

    public void setUpdateInfoFormXmlns(String updateInfoFormXmlns) {
        mUpdateInfoFormXmlns = updateInfoFormXmlns;
    }

    public String getUpdateInfoFormXmlns() {
        return mUpdateInfoFormXmlns;
    }
}
