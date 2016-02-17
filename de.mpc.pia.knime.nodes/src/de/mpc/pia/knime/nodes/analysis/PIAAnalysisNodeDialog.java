package de.mpc.pia.knime.nodes.analysis;

import java.util.Map;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import de.mpc.pia.knime.nodes.PIASettings;
import de.mpc.pia.knime.nodes.dialog.AnalysisDialog;

/**
 * <code>NodeDialog</code> for the "PIAAnalysis" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Julian Uszkoreit
 */
public class PIAAnalysisNodeDialog extends DataAwareNodeDialogPane {

    /** the dialog, which handles the settings */
    private AnalysisDialog dialogPanel;


    /**
     * New pane for configuring PIADefault node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PIAAnalysisNodeDialog() {
        super();

        dialogPanel = new AnalysisDialog();
        addTab("PIA Analysis", dialogPanel);
    }


    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        // called, when ok or apply is pressed
        Map<String, Object> settingsMap = dialogPanel.getSettings();

        // create PSM sets
        settings.addBoolean(PIASettings.CREATE_PSMSETS.getKey(),
                (Boolean)settingsMap.get(PIASettings.CREATE_PSMSETS.getKey()));
        // consider modifications
        settings.addBoolean(PIASettings.CONSIDER_MODIFICATIONS.getKey(),
                (Boolean)settingsMap.get(PIASettings.CONSIDER_MODIFICATIONS.getKey()));


        // used file ID for PSM export
        settings.addInt(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(),
                (Integer)settingsMap.get(PIASettings.PSM_ANALYSIS_FILE_ID.getKey()));

        // calculate FDR scores for all files
        settings.addBoolean(PIASettings.CALCULATE_ALL_FDR.getKey(),
                (Boolean)settingsMap.get(PIASettings.CALCULATE_ALL_FDR.getKey()));
        // calculate combined FDR score
        settings.addBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(),
                (Boolean)settingsMap.get(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey()));

        // decoy strategy for all files
        settings.addString(PIASettings.ALL_DECOY_STRATEGY.getKey(),
                (String)settingsMap.get(PIASettings.ALL_DECOY_STRATEGY.getKey()));
        // decoy pattern for all files
        settings.addString(PIASettings.ALL_DECOY_PATTERN.getKey(),
                (String)settingsMap.get(PIASettings.ALL_DECOY_PATTERN.getKey()));

        // used identifications (for FDR) for all files
        settings.addInt(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(),
                (Integer)settingsMap.get(PIASettings.ALL_USED_IDENTIFICATIONS.getKey()));

        // the preferred scores for FDR calculation
        settings.addStringArray(PIASettings.FDR_PREFERRED_SCORES.getKey(),
                (String[])settingsMap.get(PIASettings.FDR_PREFERRED_SCORES.getKey()));

        // the PSM level filters
        settings.addStringArray(PIASettings.PSM_FILTERS.getKey(),
                (String[])settingsMap.get(PIASettings.PSM_FILTERS.getKey()));


        // used file ID for peptide export
        settings.addInt(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(),
                (Integer)settingsMap.get(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey()));

        // the peptide level filters
        settings.addStringArray(PIASettings.PEPTIDE_FILTERS.getKey(),
                (String[])settingsMap.get(PIASettings.PEPTIDE_FILTERS.getKey()));


        // the protein inference method
        settings.addString(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(),
                (String)settingsMap.get(PIASettings.PROTEIN_INFERENCE_METHOD.getKey()));
        // the filters in the protein inference
        settings.addStringArray(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(),
                (String[])settingsMap.get(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey()));
        // the scoring method for the protein inference
        settings.addString(PIASettings.PROTEIN_SCORING_METHOD.getKey(),
                (String)settingsMap.get(PIASettings.PROTEIN_SCORING_METHOD.getKey()));
        // the base score used for the protein scoring
        settings.addString(PIASettings.PROTEIN_SCORING_SCORE.getKey(),
                (String)settingsMap.get(PIASettings.PROTEIN_SCORING_SCORE.getKey()));
        // the PSMs used for the protein scoring
        settings.addString(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(),
                (String)settingsMap.get(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey()));

        // the protein level filters
        settings.addStringArray(PIASettings.PROTEIN_FILTERS.getKey(),
                (String[])settingsMap.get(PIASettings.PROTEIN_FILTERS.getKey()));
    }


    @Override
    protected void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
             throws NotConfigurableException {
        // called, when no prior executed node is connected
        dialogPanel.applySettings(settings);
    }


    @Override
    protected void loadSettingsFrom(NodeSettingsRO settings, PortObject[] input)
                    throws NotConfigurableException {
        // called, when an executed  node is connected

        // TODO: set the available scores from the input file (if they are not in the ScoreModelEnum)
        dialogPanel.applySettings(settings);
    }
}
