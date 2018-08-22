package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlasticWorkspace {

    public static boolean exists(
        CmExeWrapper cmExeWrapper, String plasticWorkspace)
        throws IOException, InterruptedException {

        Reader reader = null;
        BufferedReader bufferedReader = null;
        Path existentWkPath;

        Path requestedPath = Paths.get(plasticWorkspace);

        try {
            reader = cmExeWrapper.execute(new String[]{ "lwk", "--format={path}"});
            bufferedReader= new BufferedReader(reader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                existentWkPath = Paths.get(line.trim());

                if (!existentWkPath.equals(requestedPath))
                    continue;

                return true;
            }

            return false;

        } finally {
            if (bufferedReader != null)
                bufferedReader.close();

            IOUtils.closeQuietly(reader);
        }
    }

    public static void create(
        CmExeWrapper cmExeWrapper, String plasticWorkspace, String workspaceName)
        throws IOException, InterruptedException {

        Reader reader = null;

        try {
            reader = cmExeWrapper.execute(
                new String[]{ "mkwk", "\"" + workspaceName + "\"", "\"" + plasticWorkspace + "\""});

        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static void undoAllChanges(
        CmExeWrapper cmExeWrapper, String plasticWorkspace)
        throws IOException, InterruptedException {

        Reader reader = null;

        try {
            reader = cmExeWrapper.execute(
                new String[]{ "unco", "\"" + plasticWorkspace + "\"", "--all"});

        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
