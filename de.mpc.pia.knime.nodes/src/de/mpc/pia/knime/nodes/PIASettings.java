package de.mpc.pia.knime.nodes;

import de.mpc.pia.knime.nodes.dialog.ExportFormats;
import de.mpc.pia.knime.nodes.dialog.ExportLevels;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory.ProteinInferenceMethod;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory.ScoringType;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * This class holds some keys and default values for stored settings.
 *
 * @author julian
 *
 */
public enum PIASettings {
    ERROR_ON_NO_DECOYS(Boolean.TRUE),
    CREATE_PSMSETS(Boolean.TRUE),
    CONSIDER_MODIFICATIONS(Boolean.FALSE),

    EXPORT_FILTER(Boolean.FALSE),
    EXPORT_LEVEL(ExportLevels.none.toString()),
    EXPORT_FORMAT(ExportFormats.mzIdentML.toString()),
    EXPORT_FILEBASENAME("piaExport"),

    FDR_THRESHOLD(Double.valueOf(0.01)),

    ALL_DECOY_STRATEGY(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString()),
    ALL_DECOY_PATTERN("rev_.*"),
    ALL_USED_IDENTIFICATIONS(0),   // 1 = top identifications, 0 = all identifications

    FDR_PREFERRED_SCORES(new String[]{}),

    CALCULATE_ALL_FDR(Boolean.TRUE),
    CALCULATE_COMBINED_FDR_SCORE(Boolean.TRUE),

    PSM_ANALYSIS_FILE_ID(0),

    PSM_FILTERS(new String[]{}),

    PEPTIDE_INFER_PEPTIDES(Boolean.TRUE),
    PEPTIDE_ANALYSIS_FILE_ID(0),
    PEPTIDE_FILTERS(new String[]{}),

    PROTEIN_INFER_PROTEINS(Boolean.TRUE),
    PROTEIN_INFERENCE_METHOD(ProteinInferenceMethod.REPORT_SPECTRUM_EXTRACTOR.getShortName()),
    PROTEIN_INFERENCE_FILTERS(new String[]{}),
    PROTEIN_SCORING_METHOD(ScoringType.MULTIPLICATIVE_SCORING.getShortName()),
    PROTEIN_SCORING_SCORE(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()),
    PROTEIN_SCORING_USED_PSMS(PSMForScoring.ONLY_BEST.getShortName()),

    PROTEIN_FILTERS(new String[]{}),

    CONFIG_INPUT_COLUMN((String)null),
    ;


    private Object defaultValue;


    private PIASettings(Object defaultValue) {
        this.defaultValue = defaultValue;
    }


    public String getKey() {
        return this.name();
    }


    public Object getDefaultValue() {
        return defaultValue;
    }


    public Boolean getDefaultBoolean() {
        if (defaultValue instanceof Boolean) {
            return (Boolean)defaultValue;
        }
        return null;
    }


    public Double getDefaultDouble() {
        if (defaultValue instanceof Double) {
            return (Double)defaultValue;
        }
        return null;
    }


    public Integer getDefaultInteger() {
        if (defaultValue instanceof Integer) {
            return (Integer)defaultValue;
        }
        return null;
    }


    public String getDefaultString() {
        if (defaultValue instanceof String) {
            return (String)defaultValue;
        }
        return null;
    }


    public String[] getDefaultStringArray() {
        if (defaultValue instanceof String[]) {
            return (String[])defaultValue;
        }
        return null;
    }
}