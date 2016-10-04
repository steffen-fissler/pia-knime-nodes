package de.mpc.pia.knime.nodes.utils;

import java.io.FileNotFoundException;

import javax.swing.SwingWorker;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

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
    public PIAModeller doInBackground() {
        if (fileName == null) {
            logger.error("No valid file given.");
            return null;
        }

        PIAModeller modeller;
        Object notifier = new Object();

        modeller = new PIAModeller();
        progress[0] = 1L;
        setProgress(1);

        loadingStatus = "started parsing...";

        NotifierThread nThread = new NotifierThread(progress, notifier);
        nThread.start();

        if (modeller.loadFileName(fileName, progress, notifier)) {
            progress[0] = 100L;
            setProgress(100);
        } else {
            logger.error("File '" + fileName + "' could not be loaded.");
            progress[0] = -1L;
            modeller = null;
        }

        synchronized (notifier) {
            notifier.notifyAll();
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



    /**
     * A mini thread, which will follow the notifier object and update the progress.
     *
     * @author julian
     *
     */
    private class NotifierThread extends Thread {

        private Long[] progressArr;
        private Object notifier;

        public NotifierThread(Long[] progress, Object notifier) {
            this.progressArr = progress;
            this.notifier = notifier;
        }

        @Override
        public void run() {

            try {
                while ((notifier != null) && (progressArr[0] < 100L)) {
                    synchronized (notifier) {
                        notifier.wait(100);
                        setProgress(progress[0].intValue());
                    }
                }
            } catch (InterruptedException e) {
                // ok, just get no more notifications
            }
        }
    }
}