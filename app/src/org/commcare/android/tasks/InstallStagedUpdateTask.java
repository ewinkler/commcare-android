package org.commcare.android.tasks;

import org.commcare.android.resource.AndroidResourceManager;
import org.commcare.android.resource.ResourceInstallUtils;
import org.commcare.android.tasks.templates.CommCareTask;
import org.commcare.android.util.AndroidCommCarePlatform;
import org.commcare.dalvik.application.CommCareApp;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.resources.model.UnresolvedResourceException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class InstallStagedUpdateTask<R>
        extends CommCareTask<Void, int[], ResourceEngineOutcomes, R> {

    public InstallStagedUpdateTask(int taskId) {
        this.taskId = taskId;
        TAG = InstallStagedUpdateTask.class.getSimpleName();
    }

    @Override
    protected ResourceEngineOutcomes doTaskBackground(Void... params) {
        CommCareApp app = CommCareApplication._().getCurrentApp();
        app.setupSandbox();

        AndroidCommCarePlatform platform = app.getCommCarePlatform();
        AndroidResourceManager resourceManager =
            new AndroidResourceManager(platform);

        if (!resourceManager.isUpgradeTableStaged()) {
            return ResourceEngineOutcomes.StatusFailState;
        }

        try {
            resourceManager.upgrade();
        } catch (UnresolvedResourceException e) {
            return ResourceEngineOutcomes.StatusFailState;
        }

        ResourceInstallUtils.initAndCommitApp(app);

        return ResourceEngineOutcomes.StatusInstalled;
    }
}
