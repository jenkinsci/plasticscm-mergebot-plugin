package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.util.ForkOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;


public class CmExeWrapper {
    private final String mExecutable;
    private Launcher mLauncher;
    private TaskListener mListener;
    private FilePath mWorkspace;

    private static final int MAX_RETRIES = 3;
    private static final int TIME_BETWEEN_RETRIES = 500;

    private static final Logger logger = Logger.getLogger(CmExeWrapper.class.getName());

    public CmExeWrapper(String executable, Launcher launcher, TaskListener listener,
                       FilePath workspace) {
        mExecutable = executable;
        mLauncher = launcher;
        mListener = listener;
        mWorkspace = workspace;
    }

    public Reader execute(String[] arguments) throws IOException, InterruptedException {
        String[] cmdArgs = getToolArguments(arguments);
        String cliLine = getCliLine(cmdArgs);

        int retries = 0;
        while (retries < MAX_RETRIES) {
            Reader result = tryExecute(cmdArgs);
            if (result != null)
                return result;

            retries++;
            logger.warning(String.format(
                "The cm command '%s' failed. Retrying after %d ms... (%d)",
                    cliLine, TIME_BETWEEN_RETRIES, retries));
            Thread.sleep(TIME_BETWEEN_RETRIES);
        }

        mListener.fatalError(String.format(
            "The cm command '%s' failed after %d retries", cliLine, MAX_RETRIES));
        throw new AbortException();
    }

    private String[] getToolArguments(String[] cmArgs) {
        String[] result = new String[cmArgs.length + 1];
        result[0] = mExecutable;
        System.arraycopy(cmArgs, 0, result, 1, cmArgs.length);
        return result;
    }

    private String getCliLine(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (builder.length() == 0)
                builder.append(' ');
            builder.append(arg);
        }
        return builder.toString();
    }

    private Reader tryExecute(String[] cmdArgs) throws IOException, InterruptedException {
        ByteArrayOutputStream consoleStream = new ByteArrayOutputStream();
        Proc proc = mLauncher.launch().cmds(cmdArgs)
            .stdout(new ForkOutputStream(consoleStream, mListener.getLogger()))
            .pwd(mWorkspace).start();
        consoleStream.close();

        if (proc.join() != 0)
            return null;

        return new InputStreamReader(
            new ByteArrayInputStream(consoleStream.toByteArray()),
            StandardCharsets.UTF_8);
    }
}
