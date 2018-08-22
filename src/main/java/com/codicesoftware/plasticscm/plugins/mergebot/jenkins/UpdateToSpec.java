package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.AbortException;

public class UpdateToSpec {

    public String getFullObjectSpec() {
        return fullObjectSpec;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getRepName() {
        return repName;
    }

    public String getRepServer() {
        return repServer;
    }

    public SpecObjectType getObjectType() {
        return specObjectType;
    }

    public static UpdateToSpec parse(final String updateToSpecStr) throws AbortException {
        if (updateToSpecStr == null || updateToSpecStr.trim().equals(""))
            throw new AbortException(
                "Update spec cannot be empty. " +
                "Ensure your project is correctly parametrized with a String parameter named '" +
                MergeBotUpdater.UPDATE_TO_SPEC_PARAMETER_NAME +
                "'.");

        String[] parts = updateToSpecStr.split("@");

        if (parts.length != 3)
            throw new AbortException(
                "Update spec requires an object name, a rep name and a server name, separated by '@' character.");

        UpdateToSpec spec = new UpdateToSpec();
        spec.fullObjectSpec = updateToSpecStr;
        spec.repName = parts[1];
        spec.repServer = parts[2];

        if (!parseObjectSpec(parts[0], spec))
            throw new AbortException(
               "Unrecognized object prefix in update spec. The update spec object should be a label (lb:), " +
               "a changeset (cs:), a shelve (sh:) or a branch (br:). Example: 'cs:45@myProject@myServer:8087'");

        return spec;
    }

    private static boolean parseObjectSpec(final String objectName, UpdateToSpec spec) {
        if (objectName == null || objectName.trim().equals(""))
            return false;

        if (objectName.startsWith("/")) {
            spec.specObjectType = SpecObjectType.Branch;
            spec.objectName = objectName;
            return true;
        }

        if (objectName.startsWith("br:/")){
            spec.specObjectType = SpecObjectType.Branch;
            spec.objectName = objectName.substring("br:".length());
            return true;
        }

        if (objectName.startsWith("cs:")) {
            spec.specObjectType = SpecObjectType.Changeset;
            spec.objectName = objectName.substring("cs:".length());
            return true;
        }

        if (objectName.startsWith("lb:")) {
            spec.specObjectType = SpecObjectType.Label;
            spec.objectName = objectName.substring("lb:".length());
            return true;
        }

        if (objectName.startsWith("sh:")) {
            spec.specObjectType = SpecObjectType.Shelve;
            spec.objectName = objectName.substring("sh:".length());
            return true;
        }

        return false;
    }

    private SpecObjectType specObjectType;
    private String objectName;
    private String repName;
    private String repServer;
    private String fullObjectSpec;
}
