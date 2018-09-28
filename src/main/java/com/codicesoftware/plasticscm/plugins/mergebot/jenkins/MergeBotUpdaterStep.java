package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.Extension;
import hudson.scm.SCM;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class MergeBotUpdaterStep extends SCMStep {
    @DataBoundConstructor
    public MergeBotUpdaterStep() {
    }

    @Nonnull
    @Override
    protected SCM createSCM() {
        return new MergeBotUpdater();
    }

    @Extension
    public static final class MergeBotUpdaterStepDescriptor extends SCMStepDescriptor {
        @Override
        public String getFunctionName() {
            return "mergebotCheckout";
        }

        @Override
        public String getDisplayName() {
            return "Plastic SCM Mergebot Checkout";
        }
    }
}
