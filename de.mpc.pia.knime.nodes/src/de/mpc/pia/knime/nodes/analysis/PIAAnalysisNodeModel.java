package de.mpc.pia.knime.nodes.analysis;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObject;
import org.knime.core.data.uri.URIPortObjectSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.knime.nodes.PIAAnalysisModel;
import de.mpc.pia.knime.nodes.PIASettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSMSet;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of PIADefault.
 *
 *
 * @author Julian Uszkoreit
 */
public class PIAAnalysisNodeModel extends NodeModel {

    /** the logger instance */
    private static final NodeLogger logger =
            NodeLogger.getLogger(PIAAnalysisNodeModel.class);


    /** storing model for whether PSM sets should be created */
    private final SettingsModelBoolean m_create_psm_sets =
            new SettingsModelBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean());
    /** storing model for whether modifications should be used to distinguish peptides */
    private final SettingsModelBoolean m_consider_modifications =
            new SettingsModelBoolean(PIASettings.CONSIDER_MODIFICATIONS.getKey(), PIASettings.CONSIDER_MODIFICATIONS.getDefaultBoolean());


    /** the file ID for the PSM analysis */
    private final SettingsModelInteger m_psm_analysis_file_id =
            new SettingsModelInteger(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(), PIASettings.PSM_ANALYSIS_FILE_ID.getDefaultInteger());

    /** storing model for whether all FDRs should be calculated */
    private final SettingsModelBoolean m_calculate_all_fdr =
            new SettingsModelBoolean(PIASettings.CALCULATE_ALL_FDR.getKey(), PIASettings.CALCULATE_ALL_FDR.getDefaultBoolean());
    /** storing model for whether the Combined FDR Score should be calculated */
    private final SettingsModelBoolean m_calculate_combined_fdr =
            new SettingsModelBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(), PIASettings.CALCULATE_COMBINED_FDR_SCORE.getDefaultBoolean());

    /** storing model for the used strategy of decoy identification for all files */
    private final SettingsModelString m_all_decoy_strategy =
            new SettingsModelString(PIASettings.ALL_DECOY_STRATEGY.getKey(), PIASettings.ALL_DECOY_STRATEGY.getDefaultString());
    /** storing model for the used decoy pattern for all files */
    private final SettingsModelString m_all_decoy_pattern =
            new SettingsModelString(PIASettings.ALL_DECOY_PATTERN.getKey(), PIASettings.ALL_DECOY_PATTERN.getDefaultString());

    /** used identifications (for FDR calculation) for all files */
    private final SettingsModelInteger m_all_used_identifications =
            new SettingsModelInteger(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(), PIASettings.ALL_USED_IDENTIFICATIONS.getDefaultInteger());

    /** storing model for preferred scores for FDR calculation */
    private final SettingsModelStringArray m_fdr_preferred_scores =
            new SettingsModelStringArray(PIASettings.FDR_PREFERRED_SCORES.getKey(), PIASettings.FDR_PREFERRED_SCORES.getDefaultStringArray());

    /** storing model for PSM level filters */
    private final SettingsModelStringArray m_psm_filters =
            new SettingsModelStringArray(PIASettings.PSM_FILTERS.getKey(), PIASettings.PSM_FILTERS.getDefaultStringArray());


    /** the file ID for the peptide analysis */
    private final SettingsModelInteger m_peptide_analysis_file_id =
            new SettingsModelInteger(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(), PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getDefaultInteger());

    /** storing model for peptide level filters */
    private final SettingsModelStringArray m_peptide_filters =
            new SettingsModelStringArray(PIASettings.PEPTIDE_FILTERS.getKey(), PIASettings.PEPTIDE_FILTERS.getDefaultStringArray());


    /** storing model for the used protein inference method */
    private final SettingsModelString m_protein_inference_method =
            new SettingsModelString(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(), PIASettings.PROTEIN_INFERENCE_METHOD.getDefaultString());
    /** storing model for the protein inference filters */
    private final SettingsModelStringArray m_protein_inference_filters =
            new SettingsModelStringArray(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(), PIASettings.PROTEIN_INFERENCE_FILTERS.getDefaultStringArray());
    /** storing model for the scoring method used by the protein inference */
    private final SettingsModelString m_protein_scoring_method =
            new SettingsModelString(PIASettings.PROTEIN_SCORING_METHOD.getKey(), PIASettings.PROTEIN_SCORING_METHOD.getDefaultString());
    /** storing model for the base score used by the scoring method of the protein inference */
    private final SettingsModelString m_protein_scoring_score =
            new SettingsModelString(PIASettings.PROTEIN_SCORING_SCORE.getKey(), PIASettings.PROTEIN_SCORING_SCORE.getDefaultString());
    /** storing model for the PSMs used by the scoring during the protein inference  */
    private final SettingsModelString m_protein_scoring_used_psms =
            new SettingsModelString(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(), PIASettings.PROTEIN_SCORING_USED_PSMS.getDefaultString());

    /** storing model for protein level filters */
    private final SettingsModelStringArray m_protein_filters =
            new SettingsModelStringArray(PIASettings.PROTEIN_FILTERS.getKey(), PIASettings.PROTEIN_FILTERS.getDefaultStringArray());


    /** the analysis model, if it is created yet */
    private PIAAnalysisModel analysisModel;


    /**
     * Constructor for the node model.
     */
    protected PIAAnalysisNodeModel() {
        super(new PortType[]{ IURIPortObject.TYPE },
                new PortType[] { BufferedDataTable.TYPE,
                        BufferedDataTable.TYPE,
                        BufferedDataTable.TYPE,
                        IURIPortObject.TYPE });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        String piaXmlFileName = null;

        // get the input files
        IURIPortObject filePort = (IURIPortObject) inObjects[0];
        List<URIContent> uris = filePort.getURIContents();
        for (URIContent uric : uris) {
            URI uri = uric.getURI();
            File file = new File(uri);
            piaXmlFileName = file.getAbsolutePath();

            if (piaXmlFileName != null) {
                break;
            }
        }

        // create modeller and load the file
        PIAModeller piaModeller = new PIAModeller(piaXmlFileName);
        analysisModel = new PIAAnalysisModel(piaModeller);

        // set whether PSM sets should be created
        analysisModel.addSetting(PIASettings.CREATE_PSMSETS.getKey(),
                m_create_psm_sets.getBooleanValue());
        // set whether modifications should be considered to distinguish peptides
        analysisModel.addSetting(PIASettings.CONSIDER_MODIFICATIONS.getKey(),
                m_consider_modifications.getBooleanValue());

        // set the used decoy strategy for all files
        analysisModel.addSetting(PIASettings.ALL_DECOY_STRATEGY.getKey(),
                m_all_decoy_strategy.getStringValue());
        // set the used decoy pattern for all files
        analysisModel.addSetting(PIASettings.ALL_DECOY_PATTERN.getKey(),
                m_all_decoy_pattern.getStringValue());
        // set the used identifications for FDR calculations for all files
        analysisModel.addSetting(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(),
                m_all_used_identifications.getIntValue());

        // set the preferred scores for FDR calculation
        analysisModel.addSetting(PIASettings.FDR_PREFERRED_SCORES.getKey(),
                m_fdr_preferred_scores.getStringArrayValue());

        // set whether all FDRs should be calculated
        analysisModel.addSetting(PIASettings.CALCULATE_ALL_FDR.getKey(),
                m_calculate_all_fdr.getBooleanValue());
        // set whether the Combined FDR Score should be calculated
        analysisModel.addSetting(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(),
                m_calculate_combined_fdr.getBooleanValue());


        // set the protein inference methos
        analysisModel.addSetting(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(),
                m_protein_inference_method.getStringValue());
        // set filters for the protein inference
        analysisModel.addSetting(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(),
                m_protein_inference_filters.getStringArrayValue());
        // set the scoring used during the protein inference
        analysisModel.addSetting(PIASettings.PROTEIN_SCORING_METHOD.getKey(),
                m_protein_scoring_method.getStringValue());
        // set the base score used by the protein inference
        analysisModel.addSetting(PIASettings.PROTEIN_SCORING_SCORE.getKey(),
                m_protein_scoring_score.getStringValue());
        // set the used PSMs for the scoring
        analysisModel.addSetting(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(),
                m_protein_scoring_used_psms.getStringValue());


        // execute the PSM analysis
        analysisModel.executePSMOperations();
        List<PSMReportItem> psmList = analysisModel.getFilteredReportPSMs(
                m_psm_analysis_file_id.getIntValue(), m_psm_filters.getStringArrayValue());
        BufferedDataContainer psmContainer = createPSMContainer(psmList, exec);

        // execute the peptide analysis
        analysisModel.executePeptideOperations();
        List<ReportPeptide> peptideList = analysisModel.getFilteredReportPeptides(
                m_peptide_analysis_file_id.getIntValue(), m_peptide_filters.getStringArrayValue());
        BufferedDataContainer pepContainer = createPeptideContainer(peptideList, exec);

        // execute the protein analysis
        analysisModel.executeProteinOperations();
        List<ReportProtein> proteinList = analysisModel.getFilteredReportProteins(
                m_protein_filters.getStringArrayValue());
        BufferedDataContainer proteinContainer = createProteinContainer(proteinList, exec);

        // TODO: export one level to one selected file format
        // here the export should happen...
        String exportFile = "/dev/null";
        List<URIContent> outExportFile = new ArrayList<URIContent>();

        if ((exportFile != null) && Files.exists(new File(exportFile).toPath(), new LinkOption[]{})) {
            outExportFile.add(new URIContent(new File(exportFile).toURI(), "idXML"));
            /*
                    TODO: output the errors here
                    setExternalOutput(externalOutput);
                    setExternalErrorOutput(externalErrorOutput);
             */
        } else {
            /*
                    TODO: output the errors here
                    setFailedExternalOutput(externalOutput);
                    setFailedExternalErrorOutput(externalErrorOutput);
             */
            throw new Exception("Error while executing PIA Wizard.");
        }
        URIPortObject outExportFilePort = new URIPortObject(outExportFile);

        // TODO: make calculation of each level selectable

        return new PortObject[]{psmContainer.getTable(),
                pepContainer.getTable(),
                proteinContainer.getTable(),
                outExportFilePort};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // executed on reset.

        analysisModel = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PortObjectSpec[] out_spec = new PortObjectSpec[4];

        out_spec[0] = getPSMTableSpec();
        out_spec[1] = getPeptideTableSpec();
        out_spec[2] = getProteinTableSpec();
        out_spec[3] = new URIPortObjectSpec(new String[]{"csv", "mzIdentML", "mzTab"});

        return out_spec;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_create_psm_sets.saveSettingsTo(settings);
        m_consider_modifications.saveSettingsTo(settings);

        m_psm_analysis_file_id.saveSettingsTo(settings);
        m_calculate_all_fdr.saveSettingsTo(settings);
        m_calculate_combined_fdr.saveSettingsTo(settings);
        m_all_decoy_strategy.saveSettingsTo(settings);
        m_all_decoy_pattern.saveSettingsTo(settings);
        m_all_used_identifications.saveSettingsTo(settings);
        m_fdr_preferred_scores.saveSettingsTo(settings);
        m_psm_filters.saveSettingsTo(settings);

        m_peptide_analysis_file_id.saveSettingsTo(settings);
        m_peptide_filters.saveSettingsTo(settings);

        m_protein_inference_method.saveSettingsTo(settings);
        m_protein_inference_filters.saveSettingsTo(settings);
        m_protein_scoring_method.saveSettingsTo(settings);
        m_protein_scoring_score.saveSettingsTo(settings);
        m_protein_scoring_used_psms.saveSettingsTo(settings);
        m_protein_filters.saveSettingsTo(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_create_psm_sets.loadSettingsFrom(settings);
        m_consider_modifications.loadSettingsFrom(settings);

        m_psm_analysis_file_id.loadSettingsFrom(settings);
        m_calculate_all_fdr.loadSettingsFrom(settings);
        m_calculate_combined_fdr.loadSettingsFrom(settings);
        m_all_decoy_strategy.loadSettingsFrom(settings);
        m_all_decoy_pattern.loadSettingsFrom(settings);
        m_all_used_identifications.loadSettingsFrom(settings);
        m_fdr_preferred_scores.loadSettingsFrom(settings);
        m_psm_filters.loadSettingsFrom(settings);

        m_peptide_analysis_file_id.loadSettingsFrom(settings);
        m_peptide_filters.loadSettingsFrom(settings);

        m_protein_inference_method.loadSettingsFrom(settings);
        m_protein_inference_filters.loadSettingsFrom(settings);
        m_protein_scoring_method.loadSettingsFrom(settings);
        m_protein_scoring_score.loadSettingsFrom(settings);
        m_protein_scoring_used_psms.loadSettingsFrom(settings);
        m_protein_filters.loadSettingsFrom(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_create_psm_sets.validateSettings(settings);
        m_consider_modifications.validateSettings(settings);

        m_psm_analysis_file_id.validateSettings(settings);
        m_calculate_all_fdr.validateSettings(settings);
        m_calculate_combined_fdr.validateSettings(settings);
        m_all_decoy_strategy.validateSettings(settings);
        m_all_decoy_pattern.validateSettings(settings);
        m_all_used_identifications.validateSettings(settings);
        m_fdr_preferred_scores.validateSettings(settings);
        m_psm_filters.validateSettings(settings);

        m_peptide_analysis_file_id.validateSettings(settings);
        m_peptide_filters.validateSettings(settings);

        m_protein_inference_method.validateSettings(settings);
        m_protein_inference_filters.validateSettings(settings);
        m_protein_scoring_method.validateSettings(settings);
        m_protein_scoring_score.validateSettings(settings);
        m_protein_scoring_used_psms.validateSettings(settings);
        m_protein_filters.validateSettings(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        // TODO load internal data.
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

        // TODO save internal models.
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }


    /**
     * Returns the PSM table specs
     *
     * @return
     */
    private DataTableSpec getPSMTableSpec() {
        // TODO: select one "main-score", which should be set for each fileID (seelction by the psmModeller?)

        List<DataColumnSpec> psmCols = new ArrayList<DataColumnSpec>();
        psmCols.add(new DataColumnSpecCreator("Sequence", StringCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Accessions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        psmCols.add(new DataColumnSpecCreator("Modifications", StringCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Decoy", BooleanCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Charge", IntCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("m/z", DoubleCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("deltaMass", DoubleCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("deltaPPM", DoubleCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Retention time", DoubleCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Missed Cleavages", IntCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Source ID", StringCell.TYPE).createSpec());
        psmCols.add(new DataColumnSpecCreator("Spectrum title", StringCell.TYPE).createSpec());

        if (m_psm_analysis_file_id.getIntValue() == 0) {
            psmCols.add(new DataColumnSpecCreator("nrIdentifications", IntCell.TYPE).createSpec());
        }

        psmCols.add(new DataColumnSpecCreator("scores", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());
        psmCols.add(new DataColumnSpecCreator("score names", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        psmCols.add(new DataColumnSpecCreator("score shorts", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        DataTableSpec psmSpecTable = new DataTableSpec(psmCols.toArray(new DataColumnSpec[]{}));

        return psmSpecTable;
    }


    /**
     * Creates the PSM result data container.
     *
     * @param psmList
     * @param exec
     * @return
     */
    private BufferedDataContainer createPSMContainer(List<PSMReportItem> psmList,
            final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getPSMTableSpec());
        Integer psmId = 0;

        Map<String, String> scoreShortsToNames = analysisModel.getPSMModeller().getScoreShortsToScoreNames();
        List<String> psmScoreShorts = analysisModel.getPSMScoreShorts(m_psm_analysis_file_id.getIntValue());

        for (PSMReportItem psm : psmList) {
            psmId++;
            RowKey key = new RowKey(psmId.toString());

            List<DataCell> psmCells = new ArrayList<DataCell>();

            // sequence
            psmCells.add(new StringCell(psm.getSequence()));

            // accessions
            List<StringCell> accList = new ArrayList<StringCell>(psm.getAccessions().size());
            for (Accession acc : psm.getAccessions()) {
                accList.add(new StringCell(acc.getAccession()));
            }
            psmCells.add(CollectionCellFactory.createListCell(accList));

            // modifications
            String modString = psm.getModificationsString();
            if (modString.trim().length() == 0) {
                psmCells.add(DataType.getMissingCell());
            } else {
                psmCells.add(new StringCell(modString));
            }

            // decoy state
            psmCells.add(BooleanCell.BooleanCellFactory.create(psm.getIsDecoy()));

            // charge
            psmCells.add(new IntCell(psm.getCharge()));

            // m/z
            psmCells.add(new DoubleCell(psm.getMassToCharge()));

            // deltaMass
            psmCells.add(new DoubleCell(psm.getDeltaMass()));

            // deltaPPM
            psmCells.add(new DoubleCell(psm.getDeltaPPM()));

            // retention time
            psmCells.add(new DoubleCell(psm.getRetentionTime()));

            // charge
            psmCells.add(new IntCell(psm.getMissedCleavages()));

            // source ID
            psmCells.add(new StringCell(psm.getSourceID()));

            // spectrum title
            psmCells.add(new StringCell(psm.getSpectrumTitle()));

            // number of identifications
            if (m_psm_analysis_file_id.getIntValue() == 0) {
                psmCells.add(new IntCell(((ReportPSMSet)psm).getPSMs().size()));
            }

            // scores
            List<DoubleCell> scoresList = new ArrayList<DoubleCell>();
            List<StringCell> scoreNamesList = new ArrayList<StringCell>();
            List<StringCell> scoreShortsList = new ArrayList<StringCell>();
            for (String scoreShort : psmScoreShorts) {
                scoresList.add(new DoubleCell(psm.getScore(scoreShort)));
                scoreNamesList.add(new StringCell(scoreShortsToNames.get(scoreShort)));
                scoreShortsList.add(new StringCell(scoreShort));
            }
            psmCells.add(CollectionCellFactory.createListCell(scoresList));
            psmCells.add(CollectionCellFactory.createListCell(scoreNamesList));
            psmCells.add(CollectionCellFactory.createListCell(scoreShortsList));

            container.addRowToTable(new DefaultRow(key, psmCells));
        }

        container.close();
        return container;
    }


    /**
     * Returns the peptide table specs
     *
     * @return
     */
    private DataTableSpec getPeptideTableSpec() {
        // TODO: select one "main-score", which should be set for each fileID (seelction by the peptideModeller?)

        List<DataColumnSpec> pepCols = new ArrayList<DataColumnSpec>();
        pepCols.add(new DataColumnSpecCreator("Sequence", StringCell.TYPE).createSpec());
        pepCols.add(new DataColumnSpecCreator("Accessions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        if (m_consider_modifications.getBooleanValue()) {
            pepCols.add(new DataColumnSpecCreator("Modifications", StringCell.TYPE).createSpec());
        }

        pepCols.add(new DataColumnSpecCreator("number Spectra", IntCell.TYPE).createSpec());

        if (m_peptide_analysis_file_id.getIntValue() == 0) {
            pepCols.add(new DataColumnSpecCreator("number PSM sets", IntCell.TYPE).createSpec());
        } else {
            pepCols.add(new DataColumnSpecCreator("number PSMs", IntCell.TYPE).createSpec());
        }

        pepCols.add(new DataColumnSpecCreator("Missed Cleavages", IntCell.TYPE).createSpec());

        pepCols.add(new DataColumnSpecCreator("best scores", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());
        pepCols.add(new DataColumnSpecCreator("score names", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        pepCols.add(new DataColumnSpecCreator("score shorts", ListCell.getCollectionType(StringCell.TYPE)).createSpec());


        DataTableSpec pepSpecTable = new DataTableSpec(pepCols.toArray(new DataColumnSpec[]{}));

        return pepSpecTable;
    }


    /**
     * Creates the peptide result data container.
     *
     * @param psmList
     * @param exec
     * @return
     */
    private BufferedDataContainer createPeptideContainer(List<ReportPeptide> peptideList,
            final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getPeptideTableSpec());
        Integer pepId = 0;

        Map<String, String> scoreShortsToNames = analysisModel.getPSMModeller().getScoreShortsToScoreNames();
        List<String> scoreShorts = analysisModel.getPSMScoreShorts(m_peptide_analysis_file_id.getIntValue());

        for (ReportPeptide pep : peptideList) {
            pepId++;
            RowKey key = new RowKey(pepId.toString());

            List<DataCell> pepCells = new ArrayList<DataCell>();

            // sequence
            pepCells.add(new StringCell(pep.getSequence()));

            // accessions
            List<StringCell> accList = new ArrayList<StringCell>(pep.getAccessions().size());
            for (Accession acc : pep.getAccessions()) {
                accList.add(new StringCell(acc.getAccession()));
            }
            pepCells.add(CollectionCellFactory.createListCell(accList));

            // modifications
            if (m_consider_modifications.getBooleanValue()) {
                String modString = pep.getPSMs().get(0).getModificationsString();
                if (modString.trim().length() == 0) {
                    pepCells.add(DataType.getMissingCell());
                } else {
                    pepCells.add(new StringCell(modString));
                }
            }

            // number of spectra
            pepCells.add(new IntCell(pep.getNrSpectra()));

            // number of PSMs / PSM sets
            pepCells.add(new IntCell(pep.getNrPSMs()));

            // missed cleavages
            pepCells.add(new IntCell(pep.getMissedCleavages()));

            // scores
            List<DoubleCell> scoresList = new ArrayList<DoubleCell>();
            List<StringCell> scoreNamesList = new ArrayList<StringCell>();
            List<StringCell> scoreShortsList = new ArrayList<StringCell>();
            for (String scoreShort : scoreShorts) {
                scoresList.add(new DoubleCell(pep.getBestScore(scoreShort)));
                scoreNamesList.add(new StringCell(scoreShortsToNames.get(scoreShort)));
                scoreShortsList.add(new StringCell(scoreShort));
            }
            pepCells.add(CollectionCellFactory.createListCell(scoresList));
            pepCells.add(CollectionCellFactory.createListCell(scoreNamesList));
            pepCells.add(CollectionCellFactory.createListCell(scoreShortsList));

            container.addRowToTable(new DefaultRow(key, pepCells));
        }

        container.close();
        return container;
    }


    /**
     * Returns the protein table specs
     *
     * @return
     */
    private DataTableSpec getProteinTableSpec() {
        List<DataColumnSpec> protCols = new ArrayList<DataColumnSpec>();
        protCols.add(new DataColumnSpecCreator("Accessions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        protCols.add(new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec());

        protCols.add(new DataColumnSpecCreator("Coverages", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());

        protCols.add(new DataColumnSpecCreator("nrPeptides", IntCell.TYPE).createSpec());
        protCols.add(new DataColumnSpecCreator("nrPSMs", IntCell.TYPE).createSpec());
        protCols.add(new DataColumnSpecCreator("nrSpectra", IntCell.TYPE).createSpec());

        if (m_calculate_all_fdr.getBooleanValue() ||
                m_calculate_combined_fdr.getBooleanValue()) {
            protCols.add(new DataColumnSpecCreator("Decoy", BooleanCell.TYPE).createSpec());
            protCols.add(new DataColumnSpecCreator("FDR q-value", DoubleCell.TYPE).createSpec());
        }

        DataTableSpec protSpecTable = new DataTableSpec(protCols.toArray(new DataColumnSpec[]{}));
        return protSpecTable;
    }


    /**
     * Creates the protein result data container.
     *
     * @param psmList
     * @param exec
     * @return
     */
    private BufferedDataContainer createProteinContainer(List<ReportProtein> proteinList,
            final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getProteinTableSpec());
        Integer protId = 0;

        for (ReportProtein protein : proteinList) {
            protId++;
            RowKey key = new RowKey(protId.toString());

            List<DataCell> proteinCells = new ArrayList<DataCell>();

            // accessions
            List<StringCell> accList = new ArrayList<StringCell>(protein.getAccessions().size());
            List<DataCell> coverageList = new ArrayList<DataCell>(protein.getAccessions().size());
            for (Accession acc : protein.getAccessions()) {
                accList.add(new StringCell(acc.getAccession()));

                Double coverage = protein.getCoverage(acc.getAccession());
                if (coverage.equals(Double.NaN)) {
                    coverageList.add(DataType.getMissingCell());
                } else {
                    coverageList.add(new DoubleCell(coverage));
                }
            }
            proteinCells.add(CollectionCellFactory.createListCell(accList));

            // score
            Double score = protein.getScore();
            if (score.equals(Double.NaN)) {
                proteinCells.add(DataType.getMissingCell());
            } else {
                proteinCells.add(DoubleCellFactory.create(score));
            }

            // coverage
            proteinCells.add(CollectionCellFactory.createListCell(coverageList));

            // number of peptides
            proteinCells.add(new IntCell(protein.getNrPeptides()));

            // number of PSMs
            proteinCells.add(new IntCell(protein.getNrPSMs()));

            // number of spectra
            proteinCells.add(new IntCell(protein.getNrSpectra()));


            if (m_calculate_all_fdr.getBooleanValue() ||
                    m_calculate_combined_fdr.getBooleanValue()) {
                // decoy state
                proteinCells.add(BooleanCellFactory.create(protein.getIsDecoy()));

                // FDR q-value
                proteinCells.add(new DoubleCell(protein.getQValue()));
            }


            container.addRowToTable(new DefaultRow(key, proteinCells));
        }

        container.close();
        return container;
    }


    /**
     * Getter for the analysis model
     * @return
     */
    public PIAAnalysisModel getAnalysisModel() {
        return analysisModel;
    }


    /**
     * Returns the list of filtered proteins
     *
     * @return
     */
    public List<ReportProtein> getFilteredProteinList() {
        if (analysisModel != null) {
            return analysisModel.getFilteredReportProteins(
                m_protein_filters.getStringArrayValue());
        }

        return null;
    }
}

