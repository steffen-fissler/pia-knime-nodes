package de.mpc.pia.knime.nodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.node.NodeLogger;

import de.mpc.pia.knime.nodes.utils.ObjectSerializer;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.PSMModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory;
import de.mpc.pia.modeller.protein.scoring.AbstractScoring;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.FDRData.DecoyStrategy;


/**
 * An instance of this class handles a PIA analysis. It get's the modeller with
 * the actual PIA data during the initialization. All settings are key-worded by
 * the corresponding values in {@link PIASettings}.
 *
 * @author julian
 *
 */
public class PIAAnalysisModel {

    /** the logger instance */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(PIAAnalysisModel.class);


    /** the PIA modeller, which handles everything */
    private PIAModeller piaModeller;

    /** the settings, which are executed */
    private HashMap<String, Object> settings;


    private static final String PIA_MODEL_FILENAME = "knime.piaModel";

    private static final String SETTINGS_FILENAME = "knime.piaAnalysisSettings";


    public PIAAnalysisModel(PIAModeller piaModeller) {
        this.piaModeller = piaModeller;
        this.settings = new HashMap<>();
    }


    /**
     * Serializes the piaModeller and the settings map to files
     *
     * @param folderName
     * @throws IOException
     */
    public void saveModelTo(final File internDir) throws IOException {
        File piaModelFile = new File(internDir, PIA_MODEL_FILENAME);
        PIAModeller.serializeToFile(piaModeller, piaModelFile);

        File settingsFile = new File(internDir, SETTINGS_FILENAME);
        LOGGER.debug("Serializing settings data to " + settingsFile.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(settingsFile);
                GZIPOutputStream gzo = new GZIPOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(gzo); ) {
            oos.writeObject(settings);
        } catch (IOException e) {
            LOGGER.error("Could not write analysis settings to " + settingsFile.getAbsolutePath(), e);
            throw e;
        }
    }


    /**
     * Loads a {@link PIAAnalysisModel} from the given file folder
     * @param internDir
     * @return
     * @throws IOException
     */
    public static PIAAnalysisModel loadModelFrom(final File internDir) throws IOException {
        File piaModelFile = new File(internDir, PIA_MODEL_FILENAME);
        PIAModeller piaModeller = PIAModeller.deSerializeFromFile(piaModelFile);
        PIAAnalysisModel analysisModel = new PIAAnalysisModel(piaModeller);

        File settingsFile = new File(internDir, SETTINGS_FILENAME);
        HashMap<String, Object> settings = null;
        LOGGER.debug("De-serializing settings data from " + settingsFile.getAbsolutePath());
        try (FileInputStream fin = new FileInputStream(settingsFile);
                GZIPInputStream gzi = new GZIPInputStream(fin);
                ObjectInputStream ois = new ObjectInputStream(gzi);) {
            Object readObject = ois.readObject();
            if (readObject instanceof HashMap) {
                settings = (HashMap) readObject;
            } else {
                String msg = "Could not read the settings from the file " + settingsFile.getAbsolutePath();
                LOGGER.error(msg);
                throw new IOException(msg);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read settings from " + settingsFile.getAbsolutePath(), e);
            throw e;
        } catch (ClassNotFoundException e) {
            String msg = "Could not read settings from " + settingsFile.getAbsolutePath();
            LOGGER.error(msg, e);
            throw new IOException(msg, e);
        }

        analysisModel.settings = settings;

        return analysisModel;
    }


    /**
     * Getter for the {@link PIAModeller}
     * @return
     */
    public PIAModeller getPIAModeller() {
        return piaModeller;
    }


    /**
     * Getter for the {@link PSMModeller}
     * @return
     */
    public PSMModeller getPSMModeller() {
        return piaModeller.getPSMModeller();
    }


    /**
     * Adds the specified setting to the settings.
     *
     * @param key
     * @param value
     * @return the value of the setting with the given key, before adding the new value (might be null)
     */
    public Object addSetting(String key, Object value) {
        return settings.put(key, value);
    }


    /**
     * Gets the value of the setting with the given key.
     *
     * @param key
     * @return
     */
    public Object getSetting(String key) {
        return settings.get(key);
    }


    /**
     * Gets the value of the setting with the given key or the default value,
     * if there is no setting with this key.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public Object getSetting(PIASettings setting) {
        if (settings.containsKey(setting.getKey())) {
            return settings.get(setting.getKey());
        }
        return setting.getDefaultValue();
    }


    public String getSettingString(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof String) {
            return (String)value;
        }
        return null;
    }


    public String[] getSettingStringArray(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof String[]) {
            return (String[])value;
        }
        return null;
    }


    public Double getSettingDouble(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof Double) {
            return (Double)value;
        }
        return null;
    }


    public Integer getSettingInteger(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof Integer) {
            return (Integer)value;
        }
        return null;
    }


    public Boolean getSettingBoolean(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof Boolean) {
            return (Boolean)value;
        }
        return null;
    }


    /**
     * Execute analysis on PSM level, after all settings are set.
     * <p>
     * If a required setting is not given, the default value is used.
     */
    public List<String> executePSMOperations() {
        // TODO: implement calculation of FDR for single files

        List<String> errorList = new ArrayList<>();

        boolean createPSMSets = getSettingBoolean(PIASettings.CREATE_PSMSETS);
        piaModeller.setCreatePSMSets(createPSMSets);

        // FDR strategy and pattern
        String decoyStrategy = getSettingString(PIASettings.ALL_DECOY_STRATEGY);
        String decoyPattern = getSettingString(PIASettings.ALL_DECOY_PATTERN);
        if (decoyStrategy.equals(FDRData.DecoyStrategy.SEARCHENGINE.toString())) {
            // set the strategy to searchengine
            decoyPattern = FDRData.DecoyStrategy.SEARCHENGINE.toString();
        }
        piaModeller.getPSMModeller().setAllDecoyPattern(decoyPattern);

        for (FDRData fdrData : piaModeller.getPSMModeller().getFileFDRData().values()) {
            fdrData.setFDRThreshold(getSettingDouble(PIASettings.FDR_THRESHOLD));
        }

        // set the top identifications
        piaModeller.getPSMModeller().setAllTopIdentifications(
                getSettingInteger(PIASettings.ALL_USED_IDENTIFICATIONS));


        // set the preferred scores for FDR calculation
        String[] preferredScores = getSettingStringArray(PIASettings.FDR_PREFERRED_SCORES);
        piaModeller.getPSMModeller().resetPreferredFDRScores();
        for (int i=0; i < preferredScores.length; i++) {
            piaModeller.getPSMModeller().addPreferredFDRScore(preferredScores[i]);
        }


        // calculate the FDR
        if (getSettingBoolean(PIASettings.CALCULATE_ALL_FDR)) {
            // all FDR should be calculated
            piaModeller.getPSMModeller().calculateAllFDR();
        }

        if (getSettingBoolean(PIASettings.CALCULATE_ALL_FDR) &&
                getSettingBoolean(PIASettings.ERROR_ON_NO_DECOYS)) {
            long allDecoys = 0;
            boolean oneFdrNotNull = false;
            for (Map.Entry<Long, FDRData> fdrDataIt : piaModeller.getPSMModeller().getFileFDRData().entrySet()) {
                if (fdrDataIt.getKey() != 0L) {
                    FDRData fdrData = fdrDataIt.getValue();
                    if (fdrData != null) {
                        oneFdrNotNull = true;
                        allDecoys += fdrData.getNrDecoys();
                    }
                }
            }

            if (oneFdrNotNull == false) {
                errorList.add("FDR could not be calculated for any file.");
            }
            if (allDecoys < 1) {
                errorList.add("No decoy was found, check the pattern used for decoy detection.");
            }
        }

        if (createPSMSets
                && getSettingBoolean(PIASettings.CALCULATE_ALL_FDR)
                && getSettingBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE)) {
            // calculate the Combined FDR Score only if there are PSM sets and calculated FDRs
            piaModeller.getPSMModeller().calculateCombinedFDRScore();
        } else if (getSettingBoolean(PIASettings.CALCULATE_ALL_FDR)) {
            // PSM sets are not created, but all FDRs are calculated -> set decoy level on PSM overview
            piaModeller.getPSMModeller().updateDecoyStates(0L);
        }

        return errorList;
    }


    /**
     * Returns the filtered PSMs after the analysis for the given file.
     *
     * @param fileID
     * @param filters
     * @return
     */
    public List<PSMReportItem> getFilteredReportPSMs(Integer fileID, String[] serializedFilters) {
        List<AbstractFilter> filters = unserializeFilters(serializedFilters);

        List<PSMReportItem> report = new ArrayList<>();
        if (fileID == 0) {
            for (ReportPSMSet psm : piaModeller.getPSMModeller().getFilteredReportPSMSets(filters)) {
                report.add(psm);
            }
        } else {
            List<ReportPSM> psmList = piaModeller.getPSMModeller().getFilteredReportPSMs(fileID.longValue(), filters);

            if (psmList != null) {
                for (ReportPSM psm : psmList) {
                    report.add(psm);
                }
            }
        }

        return report;
    }


    /**
     * Unserializes the filters in the given String array and returns them as a
     * List&lt;AbstractFilter&gt;.
     *
     * @param serializedFilters
     * @return always a List, maybe null, but never null
     */
    private static List<AbstractFilter> unserializeFilters(String[] serializedFilters) {
        List<AbstractFilter> filters = new ArrayList<>();

        for (String serialFilter : serializedFilters) {
            AbstractFilter filter = ObjectSerializer.unserializeFilter(serialFilter);
            if (filter != null) {
                filters.add(filter);
            }
        }

        return filters;
    }


    /**
     * getter for the scoreShorts of the given file.
     * @return
     */
    public List<String> getPSMScoreShorts(Integer fileID) {
        if (piaModeller != null) {
            return piaModeller.getPSMModeller().getScoreShortNames(fileID.longValue());
        }

        return new ArrayList<>();
    }


    /**
     * Execute analysis on peptide level.
     * <p>
     * If a required setting is not given, the default value is used.
     */
    public void executePeptideOperations() {
        piaModeller.setConsiderModifications(getSettingBoolean(PIASettings.CONSIDER_MODIFICATIONS));

        piaModeller.getPeptideModeller().removeAllFilters();

        Long fileID = getSettingInteger(PIASettings.PEPTIDE_ANALYSIS_FILE_ID).longValue();

        String[] serializedInferenceFilters =
                getSettingStringArray(PIASettings.PEPTIDE_FILTERS);
        for (AbstractFilter filter : unserializeFilters(serializedInferenceFilters)) {
            piaModeller.getPeptideModeller().addFilter(fileID, filter);
        }
    }


    /**
     * Returns the filtered peptides after the analysis for the given file.
     *
     * @param fileID
     * @param serializedFilters
     * @return
     */
   public List<ReportPeptide> getFilteredReportPeptides(Integer fileID, String[] serializedFilters) {
       List<AbstractFilter> filters = unserializeFilters(serializedFilters);

       return piaModeller.getPeptideModeller().getFilteredReportPeptides(fileID.longValue(), filters);
   }


   /**
    * Execute analysis on protein level, i.e. execute the protein inference with
    * the given parameters.
    * <p>
    * If a required setting is not given, the default value is used.
    */
   public void executeProteinOperations() {
       AbstractProteinInference proteinInference =
               ProteinInferenceFactory.createInstanceOf(getSettingString(PIASettings.PROTEIN_INFERENCE_METHOD));

       String[] serializedInferenceFilters =
               getSettingStringArray(PIASettings.PROTEIN_INFERENCE_FILTERS);
       for (AbstractFilter filter : unserializeFilters(serializedInferenceFilters)) {
           proteinInference.addFilter(filter);
       }

       AbstractScoring proteinScoring = ProteinScoringFactory.getNewInstanceByName(
               getSettingString(PIASettings.PROTEIN_SCORING_METHOD),
               Collections.emptyMap());

       proteinScoring.setSetting(AbstractScoring.SCORING_SETTING_ID,
               getSettingString(PIASettings.PROTEIN_SCORING_SCORE));
       proteinScoring.setSetting(AbstractScoring.SCORING_SPECTRA_SETTING_ID,
               getSettingString(PIASettings.PROTEIN_SCORING_USED_PSMS));

       proteinInference.setScoring(proteinScoring);

       piaModeller.getProteinModeller().infereProteins(proteinInference);

       // calculate protein FDR, if PSM FDR was calculated
       if (getSettingBoolean(PIASettings.CALCULATE_ALL_FDR) ||
               getSettingBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE)) {
           DecoyStrategy decoyStrategy = DecoyStrategy.getStrategyByString(
                   getSettingString(PIASettings.ALL_DECOY_STRATEGY));
           String decoyPattern = getSettingString(PIASettings.ALL_DECOY_PATTERN);

           piaModeller.getProteinModeller().updateFDRData(decoyStrategy,
                   decoyPattern, getSettingDouble(PIASettings.FDR_THRESHOLD));

           piaModeller.getProteinModeller().updateDecoyStates();
           piaModeller.getProteinModeller().calculateFDR();
       }
   }


   /**
    * Returns the filtered report proteins.
    *
    * @param serializedFilters
    * @return
    */
   public List<ReportProtein> getFilteredReportProteins(String[] serializedFilters) {
       List<AbstractFilter> filters = unserializeFilters(serializedFilters);

       return piaModeller.getProteinModeller().getFilteredReportProteins(filters);
   }
}
