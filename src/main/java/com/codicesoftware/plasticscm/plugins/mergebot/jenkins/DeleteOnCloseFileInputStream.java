package com.codicesoftware.plasticscm.plugins.mergebot.jenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

public class DeleteOnCloseFileInputStream extends FileInputStream {
    public DeleteOnCloseFileInputStream(String fileName) throws FileNotFoundException {
        this(new File(fileName));
    }

    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException{
        super(file);
        this.file = file;
    }

    public void close() throws IOException {
        if (file == null)
            return;

        try {
            super.close();
        } finally {
            if (file != null) {
                if (!file.delete()) {
                    logger.warning(String.format(
                        "Unable to delete file '%s': delete() method returned false.",
                        file.getCanonicalPath()));
                }
                file = null;
            }
        }
    }

    private File file;
    private static final Logger logger = Logger.getLogger(MergeBotUpdater.class.getName());
}