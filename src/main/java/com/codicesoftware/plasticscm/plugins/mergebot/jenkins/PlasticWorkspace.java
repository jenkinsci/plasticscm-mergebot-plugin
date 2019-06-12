package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlasticWorkspace {

    public static boolean exists(
        CmExeWrapper cmExeWrapper, String pathToCheck)
        throws IOException, InterruptedException {

        Reader reader = null;
        BufferedReader bufferedReader = null;

        try {
            reader = cmExeWrapper.execute(new String[]{ "lwk", "--format={path}"});
            bufferedReader= new BufferedReader(reader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                if (!isSamePlasticWorkspacePath(pathToCheck, line.trim()))
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
                new String[]{ "mkwk", workspaceName, plasticWorkspace});

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
                new String[]{ "unco", plasticWorkspace, "--silent", "--all"});

        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static boolean isSamePlasticWorkspacePath(String pathToCheck, String existentPlasticWk)
        throws IOException {

        Path pathToCheckPath = Paths.get(pathToCheck);
        Path existentPlasticWkPath = Paths.get(existentPlasticWk);

        String existentCanonical = existentPlasticWkPath.toFile().getCanonicalPath();
        String requestedCanonical = pathToCheckPath.toFile().getCanonicalPath();

        if (isWindowsPath(pathToCheck))
        {
            existentCanonical = existentCanonical.toLowerCase();
            requestedCanonical = requestedCanonical.toLowerCase();
        }

        return existentCanonical.equals(requestedCanonical);
    }

    private static boolean isWindowsPath(String requestedPath)
    {
        if (requestedPath.length() < 2)
            return false;

        return Character.isLetter(requestedPath.charAt(0)) && requestedPath.charAt(1) ==':';
    }
}
