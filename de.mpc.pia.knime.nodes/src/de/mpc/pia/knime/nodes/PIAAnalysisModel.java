package de.mpc.pia.knime.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** the PIA modeller, which handles everything */
    private PIAModeller piaModeller;

    /** the settings, which are executed */
    private Map<String, Object> settings;


    public PIAAnalysisModel(PIAModeller piaModeller) {
        this.piaModeller = piaModeller;
        this.settings = new HashMap<String, Object>();
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


    private String getSettingString(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof String) {
            return (String)value;
        }
        return null;
    }


    private String[] getSettingStringArray(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof String[]) {
            return (String[])value;
        }
        return null;
    }


    private Double getSettingDouble(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof Double) {
            return (Double)value;
        }
        return null;
    }


    private Integer getSettingInteger(PIASettings setting) {
        Object value = getSetting(setting);
        if (value instanceof Integer) {
            return (Integer)value;
        }
        return null;
    }


    private Boolean getSettingBoolean(PIASettings setting) {
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
    public void executePSMOperations() {
        // TODO: implement calculation of FDR for single files

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

        // calculate the Combined FDR Score
        if (createPSMSets &&
                getSettingBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE)) {
            piaModeller.getPSMModeller().calculateCombinedFDRScore();
        }
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
        List<AbstractFilter> filters = new ArrayList<AbstractFilter>();

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

        return new ArrayList<String>();
    }


    /**
     * Execute analysis on peptide level.
     * <p>
     * If a required setting is not given, the default value is used.
     */
    public void executePeptideOperations() {
        piaModeller.setCreatePSMSets(getSettingBoolean(PIASettings.CREATE_PSMSETS));
        piaModeller.setConsiderModifications(getSettingBoolean(PIASettings.CONSIDER_MODIFICATIONS));
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
       piaModeller.setCreatePSMSets(getSettingBoolean(PIASettings.CREATE_PSMSETS));
       piaModeller.setConsiderModifications(getSettingBoolean(PIASettings.CONSIDER_MODIFICATIONS));

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

       proteinScoring.setSetting(AbstractScoring.scoringSettingID,
               getSettingString(PIASettings.PROTEIN_SCORING_SCORE));
       proteinScoring.setSetting(AbstractScoring.scoringSpectraSettingID,
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
