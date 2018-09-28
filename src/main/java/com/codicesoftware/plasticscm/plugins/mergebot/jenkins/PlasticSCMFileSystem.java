package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.util.LogTaskListener;
import jenkins.scm.api.*;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlasticSCMFileSystem extends SCMFileSystem {
    protected PlasticSCMFileSystem(
            @Nonnull Item owner,
            @Nonnull MergeBotUpdater scm,
            @CheckForNull SCMRevision rev) {
        super(rev);
        this.scm = scm;
        this.taskListener = new LogTaskListener(LOGGER, Level.ALL);
        this.launcher = new Launcher.LocalLauncher(this.taskListener);
        this.owner = owner;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    public SCMFile getRoot() {
        return new PlasticSCMFile(this);
    }

    public MergeBotUpdater getScm() {
        return scm;
    }

    public Run getRun() {
        for (Job job : owner.getAllJobs()) {
            return job.getLastBuild();
        }
        return null;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public TaskListener getTaskListener() {
        return taskListener;
    }

    @Extension
    public static class BuilderImpl extends SCMFileSystem.Builder {
        @Override
        public boolean supports(SCM source) {
            return isMergeBotUpdater(source);
        }

        @Override
        public boolean supports(SCMSource source) {
            return false;
        }

        @Override
        public SCMFileSystem build(
                @Nonnull Item owner,
                @Nonnull SCM scm,
                @CheckForNull SCMRevision rev){
            if (!isMergeBotUpdater(scm))
                return null;

            return new PlasticSCMFileSystem(owner, (MergeBotUpdater)scm, rev);
        }

        private static boolean isMergeBotUpdater(SCM scm){
            return scm instanceof MergeBotUpdater;
        }
    }

    private final Launcher launcher;
    private final TaskListener taskListener;
    private final MergeBotUpdater scm;
    private final Item owner;
    private static final Logger LOGGER = Logger.getLogger(PlasticSCMFileSystem.class.getName());
}
