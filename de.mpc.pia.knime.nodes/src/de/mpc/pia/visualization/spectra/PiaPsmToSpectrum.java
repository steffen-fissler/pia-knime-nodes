package de.mpc.pia.visualization.spectra;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeLogger;

import de.mpc.pia.intermediate.PeptideSpectrumMatch;
import de.mpc.pia.modeller.psm.PSMItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import uk.ac.ebi.pride.utilities.data.controller.DataAccessController;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzDataControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzXmlControllerImpl;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.PeakControllerImpl;
import uk.ac.ebi.pride.utilities.data.core.Spectrum;

/**
 * This class maps the PSM IDs from PIA to spectra in any spectra file, which
 * can be parsed by the ms-data-core-api. The spectra file is parsed and the
 * spectra can be accessed by the PSM ID after the instantiation of this class.
 *
 * @author julian
 *
 */
public class PiaPsmToSpectrum<P extends PSMItem> {

    /** the logger instance */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(PiaPsmToSpectrum.class);

    /** the data access controller for the file containing the MZ data */
    private DataAccessController daController;

    /** mapping from the bin nr (= mz / BIN_WIDTH) to the actual spectra ID in the controller  */
    private Map<Integer, List<Comparable>> mzToSpectraBins;


    /** the allowed delta in the spectrum matching */
    public static final double MATCHING_TOLERANCE = 0.001;

    /** the m/z width for spectrum binning */
    private static final float BIN_WIDTH = 0.05f;


    /**
     * Creates a matcher for the given spectra file and the PSMs.
     *
     * @param spectraFile
     * @param psms
     */
    public PiaPsmToSpectrum(File spectraFile, Collection<P> psms) {
        daController = null;

        if (!initiateController(spectraFile)) {
            throw new AssertionError("Spectrum file could not be read.");
        }

        if (!psms.isEmpty()) {
            P psm = psms.iterator().next();

            if (!((psm instanceof PeptideSpectrumMatch) || (psm instanceof ReportPSM))) {
                throw new AssertionError("Not supported PSM type " + psm.getClass().getCanonicalName()
                        + ". The PSMs must be either " + PeptideSpectrumMatch.class.getCanonicalName()
                        + " or " + ReportPSM.class.getCanonicalName());
            }
        }

        mzToSpectraBins = preBinSpectra();
    }


    /**
     * Finish all up and close the controller.
     */
    public void close() {
        daController.close();
    }


    /**
     * initiates the {@link DataAccessController}
     *
     * @param spectraFile
     * @return true, if it was initiated correctly, otherwise false
     */
    private boolean initiateController(File spectraFile) {
        if (MzMLControllerImpl.isValidFormat(spectraFile)) {
            daController = new MzMLControllerImpl(spectraFile);
        } else if (MzXmlControllerImpl.isValidFormat(spectraFile)) {
            daController = new MzXmlControllerImpl(spectraFile);
        } else if (MzDataControllerImpl.isValidFormat(spectraFile)) {
            daController = new MzDataControllerImpl(spectraFile);
        } else if (PeakControllerImpl.isValidFormat(spectraFile) != null) {
            daController = new PeakControllerImpl(spectraFile);
        }

        return daController != null;
    }


    /**
     * Calculate the bin number (in the map) given the PSM (or its masstocharge)
     * @param psms
     * @return
     */
    private Integer calculateBinNumber(P psm) {
        return (int)(psm.getMassToCharge() / BIN_WIDTH);
    }


    /**
     * Bins the spectra according to their m/z values in bins
     *
     * @return
     */
    private Map<Integer, List<Comparable>> preBinSpectra() {
        LOGGER.debug("pre binning the spectra");
        Map<Integer, List<Comparable>> mzNrToSpectraBins = new HashMap<>();

        for (Comparable specID : daController.getSpectrumIds()) {
            int mzBin = (int) (daController.getSpectrumPrecursorMz(specID) / BIN_WIDTH);
            List<Comparable> specIDs = mzNrToSpectraBins.computeIfAbsent(mzBin, k -> new ArrayList<>());

            specIDs.add(specID);
        }

        LOGGER.debug("binned " + daController.getNumberOfSpectra() + " spectra into " + mzNrToSpectraBins.size() + " bins");
        return mzNrToSpectraBins;
    }


    /**
     * Finds a spectrumID for a spectrum in the controller, that fits into the
     * MZ range of the given PSM
     *
     * @return
     */
    private Comparable findMatchingSpectrumId(PSMItem psm, Collection<Comparable> spectraIDs) {
        if (spectraIDs == null) {
            return null;
        }

        // get the allowed m/z window
        double minMZ = psm.getMassToCharge() - MATCHING_TOLERANCE;
        double maxMZ = psm.getMassToCharge() + MATCHING_TOLERANCE;
        int psmCharge = psm.getCharge();
        double psmMZ = psm.getMassToCharge();

        Comparable matchingSpecID = null;
        Iterator<Comparable> specIter = spectraIDs.iterator();
        double deltaMZ = Double.POSITIVE_INFINITY;

        while (specIter.hasNext()) {
            Comparable specID = specIter.next();
            Double precMZ = getMatchingPrecursorMZ(specID, psmCharge, minMZ, maxMZ);

            if (precMZ != null) {
                double delta = Math.abs(precMZ - psmMZ);
                if (delta < deltaMZ) {
                    matchingSpecID = specID;
                    deltaMZ = delta;
                }
            }
        }

        return matchingSpecID;
    }


    /**
     * Return the precursor m/z of the spectrum in the controller given by the ID,
     * but only, if it has the correct charge and falls in the m/z region
     *
     * @param specID the spectrum id or null, if it does not match the charge or m/z region
     * @param psmCharge
     * @param minMZ
     * @param maxMZ
     * @return
     */
    private Double getMatchingPrecursorMZ(Comparable specID, int psmCharge, double minMZ, double maxMZ) {
        Integer precCharge = daController.getSpectrumPrecursorCharge(specID);
        if ((precCharge == null) || (precCharge == psmCharge)) {
            Double precMZ = daController.getSpectrumPrecursorMz(specID);

            if ((precMZ >= minMZ) && (precMZ <= maxMZ)) {
                return precMZ;
            }
        }

        return null;
    }


    /**
     * Get the spectrum from the controller without having to map it before
     *
     * @param psm
     * @return
     */
    private Spectrum getSpectrumFromController(P psm) {
        Integer mzBinNr = calculateBinNumber(psm);
        Comparable specID = findMatchingSpectrumId(psm, mzToSpectraBins.get(mzBinNr));

        if (specID != null) {
            return daController.getSpectrumById(specID);
        } else {
            return null;
        }
    }


    /**
     * returns the matched spectrum for the given PSM
     *
     * @param psm
     * @return the matching spectrum or null, if none matches
     */
    public Spectrum getSpectrumForPSM(P psm) {
        if (psm != null) {
            return getSpectrumFromController(psm);
        } else {
            return null;
        }
    }
}
