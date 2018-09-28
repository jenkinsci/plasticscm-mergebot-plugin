package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.*;
import hudson.model.*;
import hudson.scm.*;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class MergeBotUpdater extends SCM {

    public static final String UPDATE_TO_SPEC_PARAMETER_NAME = "PLASTICSCM_MERGEBOT_UPDATE_SPEC";

    @DataBoundConstructor
    public MergeBotUpdater() {
    }

    @Override
    public boolean supportsPolling() {
        return false;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new PlasticChangeLogParser();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void checkout(
        @Nonnull final Run<?, ?> run,
        @Nonnull final Launcher launcher,
        @Nonnull final FilePath workspace,
        @Nonnull final TaskListener listener,
        @CheckForNull final File changelogFile,
        @CheckForNull final SCMRevisionState baseline) throws IOException, InterruptedException {
        EnvVars environment = run.getEnvironment(listener);
        String updateToSpecString = environment.get(UPDATE_TO_SPEC_PARAMETER_NAME);

        UpdateToSpec updateToSpec = UpdateToSpec.parse(updateToSpecString);

        CmExeWrapper cmExeWrapper = new CmExeWrapper(
            getDescriptor().getCmMergebotExecutable(), launcher, listener, workspace);

        setupWorkspace(cmExeWrapper, workspace, run.getParent().getName());

        updateWorkspace(cmExeWrapper, workspace.getRemote(), updateToSpec.getFullObjectSpec());

        ChangeSet buildObject = BuildObjectMetadata.retrieveFromServer(cmExeWrapper,  updateToSpec, listener);

        if (changelogFile == null)
            return;

        writeChangeLog(listener, changelogFile, buildObject);
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(
            @Nonnull final Run<?, ?> run,
            @Nullable final FilePath wkPath,
            @Nullable final Launcher launcher,
            @Nonnull final TaskListener listener) throws IOException, InterruptedException {
        return SCMRevisionState.NONE;
    }

    private void setupWorkspace(
        CmExeWrapper cmExeWrapper, FilePath plasticWorkspace, String projectName)
        throws IOException, InterruptedException {

        if (!plasticWorkspace.exists())
            plasticWorkspace.mkdirs();

        if (PlasticWorkspace.exists(cmExeWrapper, plasticWorkspace.getRemote()))
        {
            PlasticWorkspace.undoAllChanges(cmExeWrapper, plasticWorkspace.getRemote());
            return;
        }

        String workspaceName = String.format(
            "%s-%s", projectName, UUID.randomUUID().toString());
        PlasticWorkspace.create(
            cmExeWrapper, plasticWorkspace.getRemote(), workspaceName);
    }

    private void updateWorkspace(
        CmExeWrapper cmExeWrapper, String plasticWorkspace, String fullObjectSpec)
        throws IOException, InterruptedException {

        cmExeWrapper.execute(
            new String[]{
                "switch",
                fullObjectSpec,
                "--silent",
                "--workspace=\"" + plasticWorkspace + "\""
            });
    }

    private void writeChangeLog(
        TaskListener listener, File changelogFile, ChangeSet buildObject) throws AbortException {
        try {
            ChangeLogWriter.writeLog(buildObject, changelogFile);
        } catch (Exception e) {
            listener.fatalError(e.getMessage());
            logger.severe(e.getMessage());
            throw new AbortException(e.getMessage());
        }
    }

    private static final Logger logger = Logger.getLogger(MergeBotUpdater.class.getName());

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<MergeBotUpdater> {
        private String cmMergebotExecutable;

        public DescriptorImpl() {
            super(MergeBotUpdater.class, null);
            load();
        }

        public String getCmMergebotExecutable() {
            if (cmMergebotExecutable == null) {
                return "cm";
            } else {
                return cmMergebotExecutable;
            }
        }

        @Override
        public boolean isApplicable(Job project) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            cmMergebotExecutable = Util.fixEmpty(req.getParameter("plastic.cmMergebotExecutable").trim());
            save();
            return true;
        }

        public FormValidation doCheckExecutable(@QueryParameter final String value) {
            return FormValidation.validateExecutable(value);
        }

        public String getDisplayName() {
            return "Mergebot Plastic SCM";
        }
    }
}
