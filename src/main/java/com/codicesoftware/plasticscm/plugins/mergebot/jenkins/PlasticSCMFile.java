package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.EnvVars;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class PlasticSCMFile extends SCMFile {
    public PlasticSCMFile(PlasticSCMFileSystem fs) {
        this.fs = fs;
        this.isDir = true;
    }

    public PlasticSCMFile(
            PlasticSCMFileSystem fs,
            @Nonnull PlasticSCMFile parent,
            String name,
            boolean isDir) {
        super(parent, name);
        this.fs = fs;
        this.isDir = isDir;
    }

    @Nonnull
    @Override
    protected SCMFile newChild(@Nonnull String name, boolean assumeIsDirectory) {
        return new PlasticSCMFile(fs, this, name, assumeIsDirectory);
    }

    @Nonnull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        return new ArrayList<SCMFile>();
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        return 0;
    }

    @Nonnull
    @Override
    protected Type type() throws IOException, InterruptedException {
        return isDir ? Type.DIRECTORY : Type.REGULAR_FILE;
    }

    @Nonnull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        String cmExe = fs.getScm().getDescriptor().getCmMergebotExecutable();

        CmExeWrapper cmExeWrapper = new CmExeWrapper(
            cmExe,
            fs.getLauncher(),
            fs.getTaskListener(),
            Jenkins.getInstance().getRootPath());

        EnvVars environment = fs.getRun().getEnvironment(fs.getTaskListener());
        String updateToSpecString = environment.get(
            MergeBotUpdater.UPDATE_TO_SPEC_PARAMETER_NAME);

        File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        cmExeWrapper.execute(new String[] {
            "cat",
            String.format("serverpath:/%s#%s", getPath(), updateToSpecString),
            "--file=" + tempFile.getPath()
        });

        return new DeleteOnCloseFileInputStream(tempFile);
    }

    private final PlasticSCMFileSystem fs;
    private final boolean isDir;
}
