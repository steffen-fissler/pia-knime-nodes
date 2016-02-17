package de.mpc.pia.knime.nodes.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;

import de.mpc.pia.knime.nodes.PIASettings;
import de.mpc.pia.knime.nodes.dialog.renderer.ScoreCellRenderer;
import de.mpc.pia.knime.nodes.utils.ObjectSerializer;
import de.mpc.pia.modeller.protein.inference.AbstractProteinInference;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory;
import de.mpc.pia.modeller.protein.inference.ProteinInferenceFactory.ProteinInferenceMethod;
import de.mpc.pia.modeller.protein.scoring.ProteinScoringFactory.ScoringType;
import de.mpc.pia.modeller.protein.scoring.settings.PSMForScoring;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * Handler for the settings in an analysis dialog.
 *
 * @author julian
 *
 */
public class AnalysisDialog extends JTabbedPane implements ActionListener {

    // TODO: usage of flow variables for the settings
    // TODO: add help/information for the settings from the help properties file
    // TODO: make settings for decoys, FDR calculation etc. possible for each single file

    /** the logger instance */
    private static final NodeLogger logger =
            NodeLogger.getLogger(AnalysisDialog.class);

    /** the panel for the PSM level analysis settings */
    private JPanel psmAnalysisPanel;

    /** the panel for the peptide level analysis settings */
    private JPanel peptideAnalysisPanel;

    /** the panel for the protein level analysis settings */
    private JPanel proteinAnalysisPanel;

    /** the panel for the general settings */
    private JPanel generalSettingsPanel;


    /** checkbox to select, whether PSM sets should be created */
    private JCheckBox checkCreatePSMSets;
    /** checkbox to select, whether modifications are considered to distinguish peptides */
    private JCheckBox checkConsiderModifications;


    /** text field for the selected file, for which the PSM export should be performed */
    private JFormattedTextField fieldPSMAnalysisFileID;

    /** checkbox to select, whether FDR for all files should be calculated */
    private JCheckBox checkCalculateAllFDR;
    /** checkbox to select, whether the Combined FDR Score should be calculated*/
    private JCheckBox checkCalculateCombinedFDRScore;

    /** radio button group for decoy strategy on all files */
    private ButtonGroup allDecoyStrategy;
    /** text field for the decoy pattern for all files */
    private JTextField fieldAllDecoyPattern;

    /** button group to select for all files whether to use all or only top identification */
    private ButtonGroup allUsedTopIdentifications;

    /** the list of scores available for FDR score calculation */
    private JList<String> availablePreferredFDRScoresList;

    /** the list of scores selected for FDR score calculation */
    private JList<String> selectedPreferredFDRScoresList;

    /** button which adds a score from the available to selected scores for FDR calculation */
    private JButton addToPreferred_button;
    /** button which removes a score from the selected scores for FDR calculation */
    private JButton removeFromPreferred_button;

    /** panel for the filters on the PSM level */
    private FilterPanel filtersPSMLevel;


    /** text field for the selected file, for which the peptide export should be performed */
    private JFormattedTextField fieldPeptideAnalysisFileID;

    /** panel for the filters on the peptide level */
    private FilterPanel filtersPeptideLevel;


    /** button group to select the inference method */
    private ButtonGroup radioGrpInferenceMethod;
    /** the filters for the protein inference */
    private FilterPanel filtersProteinInference;
    /** button group to select the protin scoring method */
    private ButtonGroup radioGrpProteinScoring;
    /** the combobox for the available base scores for protein scoring */
    private JComboBox<String> comboAvailableBaseScores;
    /** button group to select the used PSMs forscoring */
    private ButtonGroup radioGrpPSMsForScoring;

    /** panel for the filters on the protein level */
    private FilterPanel filtersProteinLevel;



    public AnalysisDialog() {
        // general settings are used in other panels, create this first
        initializeGeneralSettingsPanel();

        initializePSMPanel();
        this.addTab("PSMs", psmAnalysisPanel);

        initializePeptidePanel();
        this.addTab("peptides", peptideAnalysisPanel);

        initializeProteinPanel();
        this.addTab("proteins", proteinAnalysisPanel);

        this.addTab("general", generalSettingsPanel);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(checkCreatePSMSets)) {
            checkCalculateCombinedFDRScore.setEnabled(checkCreatePSMSets.isSelected());
        } else if (e.getSource() instanceof JRadioButton) {
            JRadioButton radioButton = (JRadioButton)e.getSource();

            // decoy pattern can only be edited, if selected decoy strategy is pattern
            fieldAllDecoyPattern.setEnabled(
                    allDecoyStrategy.getSelection().getActionCommand().equals(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString()));

            if (radioButton.getActionCommand().equals(radioGrpInferenceMethod.getSelection().getActionCommand())) {
                // inference method clicked
                updateAvailableInferenceFilters();
            }
        } else if (e.getSource().equals(addToPreferred_button) && (availablePreferredFDRScoresList.getSelectedIndex() > -1)) {
            ((DefaultListModel<String>)selectedPreferredFDRScoresList.getModel()).addElement(
                    availablePreferredFDRScoresList.getSelectedValue());
            ((DefaultListModel<String>)availablePreferredFDRScoresList.getModel()).remove(
                    availablePreferredFDRScoresList.getSelectedIndex());
        } else if (e.getSource().equals(removeFromPreferred_button) && (selectedPreferredFDRScoresList.getSelectedIndex() > -1)) {
            ((DefaultListModel<String>)availablePreferredFDRScoresList.getModel()).addElement(
                    selectedPreferredFDRScoresList.getSelectedValue());
            ((DefaultListModel<String>)selectedPreferredFDRScoresList.getModel()).remove(
                    selectedPreferredFDRScoresList.getSelectedIndex());
        }
    }


    /**
     * Creates a map with the current settings
     *
     * @param key
     * @return
     */
    public Map<String, Object> getSettings() {
        HashMap<String, Object> settings = new HashMap<String, Object>();

        // create PSM sets
        settings.put(PIASettings.CREATE_PSMSETS.getKey(), checkCreatePSMSets.isSelected());
        // consider modifications
        settings.put(PIASettings.CONSIDER_MODIFICATIONS.getKey(), checkConsiderModifications.isSelected());


        // PSM file ID
        settings.put(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(), Integer.parseInt(fieldPSMAnalysisFileID.getText()));

        // calculate FDR scores for all files
        settings.put(PIASettings.CALCULATE_ALL_FDR.getKey(), checkCalculateAllFDR.isSelected());
        // calculate combined FDR score
        settings.put(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(), checkCalculateCombinedFDRScore.isSelected());

        // decoy strategy for all files
        settings.put(PIASettings.ALL_DECOY_STRATEGY.getKey(), allDecoyStrategy.getSelection().getActionCommand());
        // decoy pattern for all files
        settings.put(PIASettings.ALL_DECOY_PATTERN.getKey(), fieldAllDecoyPattern.getText());

        // used identifications (for FDR) for all files
        settings.put(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(), Integer.parseInt(allUsedTopIdentifications.getSelection().getActionCommand()));

        // the preferred scores for FDR calculation
        DefaultListModel<String> scoreListModel = (DefaultListModel<String>)selectedPreferredFDRScoresList.getModel();
        String[] preferredScoreShorts = new String[scoreListModel.getSize()];
        for (int i=0; i < scoreListModel.getSize(); i++) {
                preferredScoreShorts[i] = scoreListModel.get(i);
        }
        settings.put(PIASettings.FDR_PREFERRED_SCORES.getKey(), preferredScoreShorts);

        // the PSM level filters
        List<AbstractFilter> filtersList = filtersPSMLevel.getAppliedFilters();
        String[] filtersArr = new String[filtersList.size()];
        for (int i=0; i < filtersList.size(); i++) {
            filtersArr[i] = ObjectSerializer.serializeFilter(filtersList.get(i));
        }
        settings.put(PIASettings.PSM_FILTERS.getKey(), filtersArr);


        // peptide file ID
        settings.put(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(), Integer.parseInt(fieldPeptideAnalysisFileID.getText()));

        // the peptide level filters
        filtersList = filtersPeptideLevel.getAppliedFilters();
        filtersArr = new String[filtersList.size()];
        for (int i=0; i < filtersList.size(); i++) {
            filtersArr[i] = ObjectSerializer.serializeFilter(filtersList.get(i));
        }
        settings.put(PIASettings.PEPTIDE_FILTERS.getKey(), filtersArr);


        // protein inference method
        settings.put(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(), radioGrpInferenceMethod.getSelection().getActionCommand());

        // the protein inference filters
        filtersList = filtersProteinInference.getAppliedFilters();
        filtersArr = new String[filtersList.size()];
        for (int i=0; i < filtersList.size(); i++) {
            filtersArr[i] = ObjectSerializer.serializeFilter(filtersList.get(i));
        }
        settings.put(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(), filtersArr);

        // scoring method used by protein inference
        settings.put(PIASettings.PROTEIN_SCORING_METHOD.getKey(), radioGrpProteinScoring.getSelection().getActionCommand());

        // scoring method used by protein inference
        settings.put(PIASettings.PROTEIN_SCORING_SCORE.getKey(), comboAvailableBaseScores.getSelectedItem());

        // the PSMs used for protein scoring
        settings.put(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(), radioGrpPSMsForScoring.getSelection().getActionCommand());

        // the protein level filters
        filtersList = filtersProteinLevel.getAppliedFilters();
        filtersArr = new String[filtersList.size()];
        for (int i=0; i < filtersList.size(); i++) {
            filtersArr[i] = ObjectSerializer.serializeFilter(filtersList.get(i));
        }
        settings.put(PIASettings.PROTEIN_FILTERS.getKey(), filtersArr);

        return settings;
    }


    /**
     *
     * @param settings
     */
    public void applySettings(NodeSettingsRO settings) {
        // create PSM sets
        checkCreatePSMSets.setSelected(
                settings.getBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean()));
        checkCalculateCombinedFDRScore.setEnabled(checkCreatePSMSets.isSelected());
        // consider modifications
        checkConsiderModifications.setSelected(
                settings.getBoolean(PIASettings.CONSIDER_MODIFICATIONS.getKey(), PIASettings.CONSIDER_MODIFICATIONS.getDefaultBoolean()));


        // PSM file ID
        fieldPSMAnalysisFileID.setValue(
                settings.getInt(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(), PIASettings.PSM_ANALYSIS_FILE_ID.getDefaultInteger()));

        // calculate all FDR scores
        checkCalculateAllFDR.setSelected(
                settings.getBoolean(PIASettings.CALCULATE_ALL_FDR.getKey(), PIASettings.CALCULATE_ALL_FDR.getDefaultBoolean()));
        // calculate combined FDR score
        checkCalculateCombinedFDRScore.setSelected(
                settings.getBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(), PIASettings.CALCULATE_COMBINED_FDR_SCORE.getDefaultBoolean()));

        // decoy strategy for all files
        String decoyStrategy = settings.getString(PIASettings.ALL_DECOY_STRATEGY.getKey(), PIASettings.ALL_DECOY_STRATEGY.getDefaultString());
        updateSelectedRadioButtonInGroup(decoyStrategy, allDecoyStrategy);

        // decoy pattern for all files
        fieldAllDecoyPattern.setText(
                settings.getString(PIASettings.ALL_DECOY_PATTERN.getKey(), PIASettings.ALL_DECOY_PATTERN.getDefaultString()));
        fieldAllDecoyPattern.setEnabled(!decoyStrategy.equals(FDRData.DecoyStrategy.SEARCHENGINE.toString()));

        // used identifications (for FDR) for all files
        updateSelectedRadioButtonInGroup(
                Integer.toString(settings.getInt(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(), PIASettings.ALL_USED_IDENTIFICATIONS.getDefaultInteger())),
                allUsedTopIdentifications);

        // the preferred scores for FDR calculation
        DefaultListModel<String> scoreListModel = (DefaultListModel<String>)selectedPreferredFDRScoresList.getModel();
        scoreListModel.removeAllElements();
        for (String scoreShort
                        : settings.getStringArray(PIASettings.FDR_PREFERRED_SCORES.getKey(), PIASettings.FDR_PREFERRED_SCORES.getDefaultStringArray())) {
            scoreListModel.addElement(scoreShort);
        }

        // the PSM level filters
        replaceAppliedFilters(settings.getStringArray(PIASettings.PSM_FILTERS.getKey(), PIASettings.PSM_FILTERS.getDefaultStringArray()),
                filtersPSMLevel);


        // peptide file ID
        fieldPeptideAnalysisFileID.setValue(
                settings.getInt(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(), PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getDefaultInteger()));

        // the peptide level filters
        replaceAppliedFilters(settings.getStringArray(PIASettings.PEPTIDE_FILTERS.getKey(), PIASettings.PEPTIDE_FILTERS.getDefaultStringArray()),
                filtersPeptideLevel);


        // protein inference method
        updateSelectedRadioButtonInGroup(
                settings.getString(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(), PIASettings.PROTEIN_INFERENCE_METHOD.getDefaultString()),
                radioGrpInferenceMethod);

        // the protein inference filters
        updateAvailableInferenceFilters();
        replaceAppliedFilters(settings.getStringArray(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(), PIASettings.PROTEIN_INFERENCE_FILTERS.getDefaultStringArray()),
                filtersProteinInference);

        // scoring method used by protein inference
        updateSelectedRadioButtonInGroup(
                settings.getString(PIASettings.PROTEIN_SCORING_METHOD.getKey(), PIASettings.PROTEIN_SCORING_METHOD.getDefaultString()),
                radioGrpProteinScoring);

        // base score used by protein inference
        comboAvailableBaseScores.setSelectedItem(
                settings.getString(PIASettings.PROTEIN_SCORING_SCORE.getKey(), PIASettings.PROTEIN_SCORING_SCORE.getDefaultString()));

        // the PSMs used for protein scoring
        updateSelectedRadioButtonInGroup(
                settings.getString(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(), PIASettings.PROTEIN_SCORING_USED_PSMS.getDefaultString()),
                radioGrpPSMsForScoring);

        // the protein level filters
        replaceAppliedFilters(settings.getStringArray(PIASettings.PROTEIN_FILTERS.getKey(), PIASettings.PROTEIN_FILTERS.getDefaultStringArray()),
                filtersProteinLevel);
    }


    /**
     * Replaces the applied filters in the given filter panel by the filters in
     * the given array of serialized filters.
     *
     * @param filters
     * @param filterPanel
     */
    private static void replaceAppliedFilters(String[] serializedFilters, FilterPanel filterPanel) {
        filterPanel.removeAllAppliedFilters();
        for (String serialFilter : serializedFilters) {
            AbstractFilter filter = ObjectSerializer.unserializeFilter(serialFilter);
            if (filter != null) {
                filterPanel.addAppliedFilter(filter);
            }
        }
    }


    /**
     * Updates the selected RadioButtons in the given ButtonGroup to the one
     * whichs actionCommand is equal to the given String value.
     *
     * @param actionCommand
     * @param btnGroup
     */
    private static void updateSelectedRadioButtonInGroup(String actionCommand, ButtonGroup btnGroup) {
        for (Enumeration<AbstractButton> buttonIt = btnGroup.getElements(); buttonIt.hasMoreElements();) {
            AbstractButton button = buttonIt.nextElement();
            button.setSelected(button.getActionCommand().equals(actionCommand));
        }
    }


    /**
     * Initializes the panel for the PSM settings
     */
    private void initializePSMPanel() {
        psmAnalysisPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // fieldPSMAnalysisFileID >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        psmAnalysisPanel.add(new JLabel("FileID for PSM output:"), c);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(0);

        fieldPSMAnalysisFileID = new JFormattedTextField(nf);
        fieldPSMAnalysisFileID.setValue(PIASettings.PSM_ANALYSIS_FILE_ID.getDefaultInteger());
        c.gridx = 1;
        c.gridy = row++;
        psmAnalysisPanel.add(fieldPSMAnalysisFileID, c);
        // fieldPSMAnalysisFileID <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // CalculateAllFDR >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        checkCalculateAllFDR = new JCheckBox("Calculate FDR for all files");
        checkCalculateAllFDR.setSelected(PIASettings.CALCULATE_ALL_FDR.getDefaultBoolean());
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        psmAnalysisPanel.add(checkCalculateAllFDR, c);
        // CalculateAllFDR <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // CalculateCombinedFDRScore >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        checkCalculateCombinedFDRScore = new JCheckBox("Calculate Combined FDR Score");
        checkCalculateCombinedFDRScore.setSelected(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getDefaultBoolean());
        checkCalculateCombinedFDRScore.setEnabled(checkCreatePSMSets.isSelected());
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        psmAnalysisPanel.add(checkCalculateCombinedFDRScore, c);
        // CalculateCombinedFDRScore <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // DecoyStrategyAndPattern >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        psmAnalysisPanel.add(new JLabel("How to define decoys:"), c);

        JRadioButton allDecoyStrategy_pattern = new JRadioButton("accession pattern");
        allDecoyStrategy_pattern.setActionCommand(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString());
        allDecoyStrategy_pattern.addActionListener(this);
        allDecoyStrategy_pattern.setSelected(PIASettings.ALL_DECOY_STRATEGY.getDefaultString().equals(allDecoyStrategy_pattern.getActionCommand()));
        allDecoyStrategy_pattern.addActionListener(this);

        JRadioButton allDecoyStrategy_searchengine = new JRadioButton("by searchengine");
        allDecoyStrategy_searchengine.setActionCommand(FDRData.DecoyStrategy.SEARCHENGINE.toString());
        allDecoyStrategy_searchengine.addActionListener(this);
        allDecoyStrategy_searchengine.setSelected(PIASettings.ALL_DECOY_STRATEGY.getDefaultString().equals(allDecoyStrategy_searchengine.getActionCommand()));
        allDecoyStrategy_searchengine.addActionListener(this);

        allDecoyStrategy = new ButtonGroup();
        allDecoyStrategy.add(allDecoyStrategy_pattern);
        allDecoyStrategy.add(allDecoyStrategy_searchengine);

        c.insets = new Insets(0, 5, 0, 5);
        c.gridx = 1;
        c.gridy = row++;
        psmAnalysisPanel.add(allDecoyStrategy_pattern, c);

        c.gridx = 1;
        c.gridy = row++;
        psmAnalysisPanel.add(allDecoyStrategy_searchengine, c);
        c.insets = new Insets(5, 5, 5, 5);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        psmAnalysisPanel.add(new JLabel("Decoy pattern:"), c);

        fieldAllDecoyPattern = new JTextField(PIASettings.ALL_DECOY_PATTERN.getDefaultString(), 10);
        fieldAllDecoyPattern.setEnabled(allDecoyStrategy_pattern.isSelected());
        c.gridx = 1;
        c.gridy = row++;
        psmAnalysisPanel.add(fieldAllDecoyPattern, c);
        // DecoyStrategyAndPattern <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // AllUsedIdentifications >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        psmAnalysisPanel.add(new JLabel("Used identifications:"), c);

        JRadioButton usedIdentifications_top = new JRadioButton("only top identification");
        usedIdentifications_top.setActionCommand("1");
        usedIdentifications_top.setSelected(PIASettings.ALL_USED_IDENTIFICATIONS.getDefaultInteger() == 1 );
        JRadioButton usedIdentifications_all = new JRadioButton("all identifications");
        usedIdentifications_all.setActionCommand("0");
        usedIdentifications_all.setSelected(PIASettings.ALL_USED_IDENTIFICATIONS.getDefaultInteger() == 0);

        allUsedTopIdentifications = new ButtonGroup();
        allUsedTopIdentifications.add(usedIdentifications_top);
        allUsedTopIdentifications.add(usedIdentifications_all);

        c.insets = new Insets(0, 5, 0, 5);
        c.gridx = 1;
        c.gridy = row++;
        psmAnalysisPanel.add(usedIdentifications_top, c);

        c.gridx = 1;
        c.gridy = row++;
        psmAnalysisPanel.add(usedIdentifications_all, c);
        c.insets = new Insets(5, 5, 5, 5);
        // AllTopIdentifications <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // PreferredFDRScore >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        JPanel fdrScorePanel = new JPanel(new GridBagLayout());
        fdrScorePanel.setBorder(BorderFactory.createTitledBorder("Preferred PSM scores"));

        GridBagConstraints layoutFdrScorePanel = new GridBagConstraints();
        layoutFdrScorePanel.fill = GridBagConstraints.HORIZONTAL;

        layoutFdrScorePanel.gridx = 0;
        layoutFdrScorePanel.gridy = 0;
        fdrScorePanel.add(new JLabel("Available scores"), layoutFdrScorePanel);

        DefaultListModel<String> availablePreferredFDRScores = new DefaultListModel<String>();

        availablePreferredFDRScoresList = new JList<String>(availablePreferredFDRScores);
        availablePreferredFDRScoresList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availablePreferredFDRScoresList.setLayoutOrientation(JList.VERTICAL);
        availablePreferredFDRScoresList.setCellRenderer(new ScoreCellRenderer());

        JScrollPane listScroller = new JScrollPane(availablePreferredFDRScoresList);
        listScroller.setPreferredSize(new Dimension(200, 100));

        // add all the scores, refine later
        for (ScoreModelEnum score : ScoreModelEnum.values()) {
            if (!ScoreModelEnum.notForPSMFdrScore.contains(score) && !ScoreModelEnum.UNKNOWN_SCORE.equals(score)) {
                availablePreferredFDRScores.addElement(score.getShortName());
            }
        }

        layoutFdrScorePanel.gridx = 0;
        layoutFdrScorePanel.gridy = 1;
        fdrScorePanel.add(listScroller, layoutFdrScorePanel);

        layoutFdrScorePanel.gridx = 2;
        layoutFdrScorePanel.gridy = 0;
        fdrScorePanel.add(new JLabel("Selected scores"), layoutFdrScorePanel);

        DefaultListModel<String> selectedPreferredFDRScores = new DefaultListModel<String>();

        selectedPreferredFDRScoresList = new JList<String>(selectedPreferredFDRScores);
        selectedPreferredFDRScoresList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedPreferredFDRScoresList.setLayoutOrientation(JList.VERTICAL);
        selectedPreferredFDRScoresList.setCellRenderer(new ScoreCellRenderer());

        listScroller = new JScrollPane(selectedPreferredFDRScoresList);
        listScroller.setPreferredSize(new Dimension(200, 100));

        layoutFdrScorePanel.gridx = 2;
        layoutFdrScorePanel.gridy = 1;
        fdrScorePanel.add(listScroller, layoutFdrScorePanel);


        JPanel psmScoreButtonsPanel = new JPanel();
        psmScoreButtonsPanel.setLayout(new BoxLayout(psmScoreButtonsPanel, BoxLayout.Y_AXIS));
        psmScoreButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        addToPreferred_button = new JButton("Add >>");
        addToPreferred_button.setAlignmentX(Component.CENTER_ALIGNMENT);
        addToPreferred_button.addActionListener(this);
        psmScoreButtonsPanel.add(addToPreferred_button);

        psmScoreButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        removeFromPreferred_button = new JButton("Remove <<");
        removeFromPreferred_button.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeFromPreferred_button.addActionListener(this);
        psmScoreButtonsPanel.add(removeFromPreferred_button);

        layoutFdrScorePanel.gridx = 1;
        layoutFdrScorePanel.gridy = 1;
        fdrScorePanel.add(psmScoreButtonsPanel, layoutFdrScorePanel);

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        psmAnalysisPanel.add(fdrScorePanel, c);
        // PreferredFDRScore <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // PSMLevelFilters >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        filtersPSMLevel = new FilterPanel("Filters for PSM level");

        // add all available PSM filters
        for (RegisteredFilters filter : RegisteredFilters.getPSMFilters()) {
            filtersPSMLevel.addAvailableFilter(filter);
        }
        filtersPSMLevel.addAvailableFilter(RegisteredFilters.PSM_SCORE_FILTER);
        filtersPSMLevel.addAvailableFilter(RegisteredFilters.PSM_TOP_IDENTIFICATION_FILTER);

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        psmAnalysisPanel.add(filtersPSMLevel, c);
        // PSMLevelFilters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }


    /**
     * Initializes the panel for the peptide settings
     */
    private void initializePeptidePanel() {
        peptideAnalysisPanel = new JPanel(new GridBagLayout());

        peptideAnalysisPanel.setAlignmentY(0);


        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // fieldPeptideAnalysisFileID >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        peptideAnalysisPanel.add(new JLabel("FileID for peptide output:"), c);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(0);

        fieldPeptideAnalysisFileID = new JFormattedTextField(nf);
        fieldPeptideAnalysisFileID.setValue(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getDefaultInteger());
        c.gridx = 1;
        c.gridy = row++;
        c.weightx = 1.0;
        c.weighty = 0.0;
        peptideAnalysisPanel.add(fieldPeptideAnalysisFileID, c);
        // fieldPeptideAnalysisFileID <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // PeptideLevelFilters >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        filtersPeptideLevel = new FilterPanel("Filters for peptide level");

        // add all available peptide filters
        for (RegisteredFilters filter : RegisteredFilters.getPeptideFilters()) {
            filtersPeptideLevel.addAvailableFilter(filter);
        }
        filtersPeptideLevel.addAvailableFilter(RegisteredFilters.PEPTIDE_SCORE_FILTER);
        filtersPeptideLevel.addAvailableFilter(RegisteredFilters.PSM_SCORE_FILTER);

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        peptideAnalysisPanel.add(filtersPeptideLevel, c);
        // PeptideLevelFilters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }


    /**
     * Initializes the panel for the protein settings
     */
    private void initializeProteinPanel() {
        proteinAnalysisPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        JPanel proteinInferencePanel = new JPanel(new GridBagLayout());
        proteinInferencePanel.setBorder(BorderFactory.createTitledBorder("Inference method"));
        int panelRow = 0;
        GridBagConstraints infGBC = new GridBagConstraints();
        infGBC.fill = GridBagConstraints.HORIZONTAL;
        infGBC.insets = new Insets(0, 5, 0, 5);

        // InferenceMethod >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        radioGrpInferenceMethod = new ButtonGroup();
        infGBC.gridwidth = 1;

        for (ProteinInferenceMethod method : ProteinInferenceMethod.values()) {
            JRadioButton btnMethod = new JRadioButton(method.getName());
            btnMethod.setActionCommand(method.getShortName());
            btnMethod.setSelected(method.getShortName().equals(PIASettings.PROTEIN_INFERENCE_METHOD.getDefaultString()));
            btnMethod.addActionListener(this);

            radioGrpInferenceMethod.add(btnMethod);

            infGBC.gridx = 1;
            infGBC.gridy = panelRow++;
            proteinInferencePanel.add(btnMethod, infGBC);
        }
        // InferenceMethod <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // InferenceFilters >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        filtersProteinInference = new FilterPanel("Inference filters");

        updateAvailableInferenceFilters();

        infGBC.gridx = 1;
        infGBC.gridy = panelRow++;
        proteinInferencePanel.add(filtersProteinInference, infGBC);
        // InferenceFilters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        proteinAnalysisPanel.add(proteinInferencePanel, c);

        // ScoringMethod >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        JPanel proteinScoringPanel = new JPanel(new GridBagLayout());
        proteinScoringPanel.setBorder(BorderFactory.createTitledBorder("Scoring method"));
        panelRow = 0;
        infGBC = new GridBagConstraints();
        infGBC.fill = GridBagConstraints.HORIZONTAL;
        infGBC.insets = new Insets(0, 5, 0, 5);

        radioGrpProteinScoring = new ButtonGroup();
        infGBC.gridwidth = 1;
        for (ScoringType scoring : ScoringType.values()) {
            JRadioButton btnScoring = new JRadioButton(scoring.getName());
            btnScoring.setActionCommand(scoring.getShortName());
            btnScoring.setSelected(scoring.getShortName().equals(PIASettings.PROTEIN_SCORING_METHOD.getDefaultString()));

            radioGrpProteinScoring.add(btnScoring);

            infGBC.gridx = 1;
            infGBC.gridy = panelRow++;
            infGBC.gridwidth = 2;
            proteinScoringPanel.add(btnScoring, infGBC);
        }

        // >>> score for scoring
        JLabel scoreLabel = new JLabel("Basescore for protein scoring:");
        infGBC.gridx = 1;
        infGBC.gridy = panelRow;
        infGBC.gridwidth = 1;
        infGBC.weightx = 0.0;
        proteinScoringPanel.add(scoreLabel, infGBC);

        comboAvailableBaseScores = new JComboBox<String>();
        comboAvailableBaseScores.setRenderer(new ScoreCellRenderer());
        comboAvailableBaseScores.setEditable(true);
        // add all possible scores
        for (ScoreModelEnum score : ScoreModelEnum.values()) {
            comboAvailableBaseScores.addItem(score.getShortName());
        }
        comboAvailableBaseScores.setSelectedItem(PIASettings.PROTEIN_SCORING_SCORE.getDefaultString());
        infGBC.gridx = 2;
        infGBC.gridy = panelRow++;
        infGBC.weightx = 1.0;
        proteinScoringPanel.add(comboAvailableBaseScores, infGBC);

        // >>> PSMs used for scoring
        JLabel psmsLabel = new JLabel("PSMs used for scoring:");
        infGBC.gridx = 1;
        infGBC.gridy = panelRow;
        infGBC.gridwidth = 1;
        infGBC.weightx = 0.0;
        proteinScoringPanel.add(psmsLabel, infGBC);

        radioGrpPSMsForScoring = new ButtonGroup();
        infGBC.gridwidth = 1;
        for (PSMForScoring psmSetting : PSMForScoring.values()) {
            JRadioButton btnPSMs = new JRadioButton(psmSetting.getName());
            btnPSMs.setActionCommand(psmSetting.getShortName());
            btnPSMs.setSelected(psmSetting.getShortName().equals(PIASettings.PROTEIN_SCORING_USED_PSMS.getDefaultString()));

            radioGrpPSMsForScoring.add(btnPSMs);

            infGBC.gridx = 2;
            infGBC.gridy = panelRow++;
            infGBC.gridwidth = 1;
            infGBC.weightx = 1.0;
            proteinScoringPanel.add(btnPSMs, infGBC);
        }


        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        proteinAnalysisPanel.add(proteinScoringPanel, c);
        // ScoringMethod <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // ProteinLevelFilters >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        filtersProteinLevel = new FilterPanel("Filters for protein level");

        // add all available protein filters
        for (RegisteredFilters filter : RegisteredFilters.getProteinFilters()) {
            filtersProteinLevel.addAvailableFilter(filter);
        }

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        proteinAnalysisPanel.add(filtersProteinLevel, c);
        // ProteinLevelFilters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }


    /**
     * Sets the available inference filters according to the selected inference
     * method.
     */
    private void updateAvailableInferenceFilters() {
        if ((radioGrpInferenceMethod != null) && (filtersProteinInference!= null)) {
            filtersProteinInference.removeAllAvailableFilter();

            ProteinInferenceMethod method = ProteinInferenceFactory.getProteinInferenceByName(
                    radioGrpInferenceMethod.getSelection().getActionCommand());

            if (method != null) {
                AbstractProteinInference inference = method.createInstanceOf();

                for (RegisteredFilters filter : inference.getAllAvailableFilters()) {
                    filtersProteinInference.addAvailableFilter(filter);
                }
            } else {
                logger.error("Could not create inference method for "
                        + radioGrpInferenceMethod.getSelection().getActionCommand());
            }
        }
    }


    /**
     * Initializes the panel for the general settings
     */
    private void initializeGeneralSettingsPanel() {
        generalSettingsPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // CreatePSMSets >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        checkCreatePSMSets = new JCheckBox("Create PSM sets");
        checkCreatePSMSets.setSelected(PIASettings.CREATE_PSMSETS.getDefaultBoolean());
        checkCreatePSMSets.addActionListener(this);
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 1;
        c.weighty = 0.0;
        c.weightx = 1.0;
        generalSettingsPanel.add(checkCreatePSMSets, c);
        // CreatePSMSets <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // ConsiderModifications >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        checkConsiderModifications = new JCheckBox("consider modifications to distinguish peptides");
        checkConsiderModifications.setSelected(PIASettings.CONSIDER_MODIFICATIONS.getDefaultBoolean());
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 1;
        c.weighty = 1.0;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        generalSettingsPanel.add(checkConsiderModifications, c);
        // ConsiderModifications <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    }
}
