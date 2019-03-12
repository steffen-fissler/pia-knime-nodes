package de.mpc.pia.knime.nodes.utils;

import javax.swing.SwingWorker;

import org.knime.core.node.NodeLogger;

import de.mpc.pia.modeller.PIAModeller;


/**
 * This class handles the loading and parsing of a PIA XML file as thread.
 *
 * @author julian
 */
public class LoadPIAFileWorker extends SwingWorker<PIAModeller, Void> {

    /** the name of the file, which should get parsed */
    private String fileName;

    /** the progress of loading in percent */
    private Long[] progress;

    /** a status string of the loading progress */
    private String loadingStatus;

    /** the logger */
    private NodeLogger logger;


    public LoadPIAFileWorker(String fileName, NodeLogger logger) {
        this.fileName = fileName;
        this.logger = logger;

        progress = new Long[1];
        progress[0] = 0L;
        setProgress(0);

        loadingStatus = "initialising...";
    }


    @Override
    public PIAModeller doInBackground() throws InterruptedException {
        if (fileName == null) {
            logger.error("No valid file given.");
            return null;
        }

        PIAModeller modeller;

        modeller = new PIAModeller();
        progress[0] = 1L;
        setProgress(1);

        loadingStatus = "started parsing...";

        if (modeller.loadFileName(fileName, progress)) {
            progress[0] = 100L;
            setProgress(100);
        } else {
            logger.error("File '" + fileName + "' could not be loaded.");
            progress[0] = -1L;
            modeller = null;
        }


        return modeller;
    }


    /**
     * Returns the filename, which is tried to be loaded.
     * @return
     */
    public String getFileName() {
        return fileName;
    }


    /**
     * The progress of the file loading.
     * @return
     */
    public Long getLoadingProgress() {
        if (progress != null) {
            return progress[0];
        } else {
            return -1L;
        }
    }


    /**
     * The status of the loading progress.
     * @return
     */
    public String getLoadingStatus() {
        return loadingStatus;
    }
}