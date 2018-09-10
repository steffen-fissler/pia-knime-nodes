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

    ERROR_ON_NO_DECOYS(new Boolean(true)),
    CREATE_PSMSETS(new Boolean(true)),
    CONSIDER_MODIFICATIONS(new Boolean(false)),

    EXPORT_FILTER(new Boolean(false)),
    EXPORT_LEVEL(ExportLevels.none.toString()),
    EXPORT_FORMAT(ExportFormats.mzIdentML.toString()),

    FDR_THRESHOLD(new Double(0.01)),

    ALL_DECOY_STRATEGY(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString()),
    ALL_DECOY_PATTERN("rev_.*"),
    ALL_USED_IDENTIFICATIONS(0),   // 1 = top identifications, 0 = all identifications

    FDR_PREFERRED_SCORES(new String[]{}),

    CALCULATE_ALL_FDR(new Boolean(true)),
    CALCULATE_COMBINED_FDR_SCORE(new Boolean(true)),

    PSM_ANALYSIS_FILE_ID(0),

    PSM_FILTERS(new String[]{}),

    PEPTIDE_INFER_PEPTIDES(new Boolean(true)),
    PEPTIDE_ANALYSIS_FILE_ID(0),
    PEPTIDE_FILTERS(new String[]{}),

    PROTEIN_INFER_PROTEINS(new Boolean(true)),
    PROTEIN_INFERENCE_METHOD(ProteinInferenceMethod.REPORT_SPECTRUM_EXTRACTOR.getShortName()),
    PROTEIN_INFERENCE_FILTERS(new String[]{}),
    PROTEIN_SCORING_METHOD(ScoringType.MULTIPLICATIVE_SCORING.getShortName()),
    PROTEIN_SCORING_SCORE(ScoreModelEnum.PSM_LEVEL_FDR_SCORE.getShortName()),
    PROTEIN_SCORING_USED_PSMS(PSMForScoring.ONLY_BEST.getShortName()),

    PROTEIN_FILTERS(new String[]{}),

    CONFIG_INPUT_COLUMN((String)null),
    ;


    private Object default_value;


    private PIASettings(Object default_value) {
        this.default_value = default_value;
    }


    public String getKey() {
        return this.name();
    }


    public Object getDefaultValue() {
        return default_value;
    }


    public Boolean getDefaultBoolean() {
        if (default_value instanceof Boolean) {
            return (Boolean)default_value;
        }
        return null;
    }


    public Double getDefaultDouble() {
        if (default_value instanceof Double) {
            return (Double)default_value;
        }
        return null;
    }


    public Integer getDefaultInteger() {
        if (default_value instanceof Integer) {
            return (Integer)default_value;
        }
        return null;
    }


    public String getDefaultString() {
        if (default_value instanceof String) {
            return (String)default_value;
        }
        return null;
    }


    public String[] getDefaultStringArray() {
        if (default_value instanceof String[]) {
            return (String[])default_value;
        }
        return null;
    }
}