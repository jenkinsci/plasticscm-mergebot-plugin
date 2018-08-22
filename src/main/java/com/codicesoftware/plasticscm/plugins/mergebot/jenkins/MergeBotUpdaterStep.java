package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.scm.SCM;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;

import javax.annotation.Nonnull;

public class MergeBotUpdaterStep extends SCMStep {
    @Nonnull
    @Override
    protected SCM createSCM() {
        return new MergeBotUpdater();
    }
}
