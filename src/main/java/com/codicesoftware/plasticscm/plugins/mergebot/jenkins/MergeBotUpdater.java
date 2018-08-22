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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MergeBotUpdater extends SCM {

    public static final String UPDATE_TO_SPEC_PARAMETER_NAME = "plasticsm.mergebot.update.spec";

    @DataBoundConstructor
    public MergeBotUpdater() {
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

        String updateToSpecString = run.getEnvironment(listener).get(UPDATE_TO_SPEC_PARAMETER_NAME);

        UpdateToSpec updateToSpec = UpdateToSpec.parse(updateToSpecString);

        String normalizedWorkspaceName = WorkspaceNameNormalizer.normalize(
            DEFAULT_WORKSPACE_NAME_PATTERN, run.getParent(), run);

        FilePath plasticWorkspace = new FilePath(workspace, normalizedWorkspaceName);

        CmExeWrapper cmExeWrapper = new CmExeWrapper(
            getDescriptor().getCmMergebotExecutable(), launcher, listener, plasticWorkspace);

        setupWorkspace(cmExeWrapper, plasticWorkspace, normalizedWorkspaceName);

        updateWorkspace(cmExeWrapper, plasticWorkspace.getRemote(), updateToSpec.getFullObjectSpec());

        ChangeSet buildObject = BuildObjectMetadata.retrieveFromServer(cmExeWrapper,  updateToSpec, listener);

        if (changelogFile == null)
            return;

        writeChangeLog(listener, changelogFile, buildObject);
    }

    private void setupWorkspace(
        CmExeWrapper cmExeWrapper, FilePath plasticWorkspace, String workspaceName)
        throws IOException, InterruptedException {

        if (!plasticWorkspace.exists())
            plasticWorkspace.mkdirs();

        if (PlasticWorkspace.exists(cmExeWrapper, plasticWorkspace.getRemote()))
        {
            PlasticWorkspace.undoAllChanges(cmExeWrapper, plasticWorkspace.getRemote());
            return;
        }

        PlasticWorkspace.create(cmExeWrapper, plasticWorkspace.getRemote(), workspaceName);
    }

    private void updateWorkspace(
        CmExeWrapper cmExeWrapper, String plasticWorkspace, String fullObjectSpec)
        throws IOException, InterruptedException {

        cmExeWrapper.execute(
            new String[]{
                "switch",
                fullObjectSpec,
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

    private static String DEFAULT_WORKSPACE_NAME_PATTERN = "Jenkins-${JOB_NAME}-${NODE_NAME}";

    private static final Logger logger = Logger.getLogger(MergeBotUpdater.class.getName());

    static class WorkspaceNameNormalizer {
        static String normalize(
            String workspaceName,
            Job<?,?> project,
            Run<?,?> build) {
            String result = workspaceName;

            Map<String,String> substitutionMap = new HashMap<String, String>();
            substitutionMap.put("JOB_NAME", project.getName());
            substitutionMap.put("NODE_NAME", getComputerName());

            if (build != null) {
                result = replaceBuildParameter(build, result);
                result = Util.replaceMacro(result, substitutionMap);
            }

            result = result.replaceAll("[\"/:<>\\|\\*\\?]+", "_");
            result = result.replaceAll("[\\.\\s]+$", "_");

            return result;
        }

        private static String getComputerName() {
            Computer comp = Computer.currentComputer();
            if (comp == null || Util.fixEmpty(comp.getName()) == null)
                return DEFAULT_COMPUTER_NODE_NAME;
            return comp.getName();
        }

        private static String replaceBuildParameter(Run<?,?> run, String text) {
            if (run instanceof AbstractBuild<?,?>) {
                AbstractBuild<?,?> build = (AbstractBuild<?,?>)run;
                if (build.getAction(ParametersAction.class) != null) {
                    return build.getAction(ParametersAction.class).substitute(build, text);
                }
            }
            return text;
        }

        private static String DEFAULT_COMPUTER_NODE_NAME = "MASTER";
    }

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
