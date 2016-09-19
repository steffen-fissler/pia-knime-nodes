package de.mpc.pia.knime.nodes.analysis;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.eclipse.core.commands.ExecutionException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
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
import de.mpc.pia.knime.nodes.dialog.ExportFormats;
import de.mpc.pia.knime.nodes.dialog.ExportLevels;
import de.mpc.pia.knime.nodes.filestorageport.FileStoreURIPortObject;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.exporter.IdXMLExporter;
import de.mpc.pia.modeller.exporter.MzIdentMLExporter;
import de.mpc.pia.modeller.exporter.MzTabExporter;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.visualization.spectra.PiaPsmToSpectrum;

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
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PIAAnalysisNodeModel.class);


    /** the model of the input files' URLs */
    private final SettingsModelString m_input_column =
            new SettingsModelString(PIASettings.CONFIG_INPUT_COLUMN.getKey(), PIASettings.CONFIG_INPUT_COLUMN.getDefaultString());
    /** storing model for whether PSM sets should be created */
    private final SettingsModelBoolean m_create_psm_sets =
            new SettingsModelBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean());
    /** storing model for whether modifications should be used to distinguish peptides */
    private final SettingsModelBoolean m_consider_modifications =
            new SettingsModelBoolean(PIASettings.CONSIDER_MODIFICATIONS.getKey(), PIASettings.CONSIDER_MODIFICATIONS.getDefaultBoolean());

    /** export level */
    private final SettingsModelString m_export_level =
            new SettingsModelString(PIASettings.EXPORT_LEVEL.getKey(), PIASettings.EXPORT_LEVEL.getDefaultString());
    /** export format */
    private final SettingsModelString m_export_format =
            new SettingsModelString(PIASettings.EXPORT_FORMAT.getKey(), PIASettings.EXPORT_FORMAT.getDefaultString());


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


    /** whether to infer peptides */
    private final SettingsModelBoolean m_peptide_infer_peptides =
            new SettingsModelBoolean(PIASettings.PEPTIDE_INFER_PEPTIDES.getKey(), PIASettings.PEPTIDE_INFER_PEPTIDES.getDefaultBoolean());

    /** the file ID for the peptide analysis */
    private final SettingsModelInteger m_peptide_analysis_file_id =
            new SettingsModelInteger(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(), PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getDefaultInteger());

    /** storing model for peptide level filters */
    private final SettingsModelStringArray m_peptide_filters =
            new SettingsModelStringArray(PIASettings.PEPTIDE_FILTERS.getKey(), PIASettings.PEPTIDE_FILTERS.getDefaultStringArray());


    /** whether to infer proteins */
    private final SettingsModelBoolean m_protein_infer_proteins =
            new SettingsModelBoolean(PIASettings.PROTEIN_INFER_PROTEINS.getKey(), PIASettings.PROTEIN_INFER_PROTEINS.getDefaultBoolean());

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

    /** temporary created file, delete on reset */
    private File piaTmpFile;

    /** the reported PSMs */
    private List<PSMReportItem> filteredPSMs;

    /** the reported peptides */
    private List<ReportPeptide> filteredPeptides;

    /** the reported proteins */
    private List<ReportProtein> filteredProteins;

    /** the PSM to spectrum mapper */
    private PiaPsmToSpectrum<ReportPSM> psmToSpectrum;


    /**
     * Constructor for the node model.
     */
    protected PIAAnalysisNodeModel() {
        super(
                new PortType[]{ BufferedDataTable.TYPE_OPTIONAL,
                        IURIPortObject.TYPE_OPTIONAL,
                        IURIPortObject.TYPE_OPTIONAL},
                new PortType[]{ BufferedDataTable.TYPE,
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

        // get the input file from the first port, first object
        if ((m_input_column.getStringValue() != null) && (inObjects[0] != null)) {
            DataTable table = (DataTable)inObjects[0];
            RowIterator row_it = table.iterator();
            int inputIdx = table.getDataTableSpec().findColumnIndex(m_input_column.getStringValue());
            if (inputIdx < 0) {
                throw new ExecutionException("Could not find column '" + m_input_column.getStringValue()
                        + "' in input table. Settings must be reconfigured.");
            }
            while (row_it.hasNext()) {
                DataRow row = row_it.next();
                DataCell dataCell = row.getCell(inputIdx);

                piaXmlFileName = getFilenameFromTableCell(dataCell);

                if (piaXmlFileName != null) {
                    if (row_it.hasNext()) {
                        LOGGER.warn("Only the first suitable entry in the datatable is used.");
                    }
                    break;
                }
            }
        }

        // get the input file from the file port input (if not set before)
        IURIPortObject filePort = (IURIPortObject) inObjects[1];
        if (filePort != null) {
            List<URIContent> uris = filePort.getURIContents();
            ListIterator<URIContent> uriIter = uris.listIterator();

            if ((piaXmlFileName != null) && (uris.size() > 0)) {
                LOGGER.warn("The file from datatable is used preferentially, if table and port are available");
            } else {
                while (uriIter.hasNext()) {
                    URI uri = uriIter.next().getURI();
                    File file = new File(uri);
                    piaXmlFileName = file.getAbsolutePath();

                    if (piaXmlFileName != null) {
                        if (uriIter.hasNext()) {
                            LOGGER.warn("Only the first suitable entry in the port is used.");
                        }
                        break;
                    }
                }
            }
        }

        if (piaXmlFileName == null) {
            throw new Exception("No PIA XML file given! Provide either by "
                    + "datatable (e.g. from PIA Compiler or List Files) or port (Input File)");
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

        // set the analyzed fileId for PSM operations
        analysisModel.addSetting(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(),
                m_psm_analysis_file_id.getIntValue());
        // set PSM level filters
        analysisModel.addSetting(PIASettings.PSM_FILTERS.getKey(),
                m_psm_filters.getStringArrayValue());

        // set whether all FDRs should be calculated
        analysisModel.addSetting(PIASettings.CALCULATE_ALL_FDR.getKey(),
                m_calculate_all_fdr.getBooleanValue());
        // set whether the Combined FDR Score should be calculated
        analysisModel.addSetting(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(),
                m_calculate_combined_fdr.getBooleanValue());

        // set whether to infer peptides
        analysisModel.addSetting(PIASettings.PEPTIDE_INFER_PEPTIDES.getKey(),
                m_peptide_infer_peptides.getBooleanValue());
        // set the analyzed fileId for peptide operations
        analysisModel.addSetting(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(),
                m_peptide_analysis_file_id.getIntValue());
        // set peptide level filters
        analysisModel.addSetting(PIASettings.PEPTIDE_FILTERS.getKey(),
                m_peptide_filters.getStringArrayValue());

        // set whether to infer proteins
        analysisModel.addSetting(PIASettings.PROTEIN_INFER_PROTEINS.getKey(),
                m_protein_infer_proteins.getBooleanValue());
        // set the protein inference methods
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


        // export format
        analysisModel.addSetting(PIASettings.EXPORT_FORMAT.getKey(),
                m_export_format.getStringValue());
        // export level
        analysisModel.addSetting(PIASettings.EXPORT_LEVEL.getKey(),
                m_export_level.getStringValue());


        // execute the PSM analysis
        List<String> errorMsgs = analysisModel.executePSMOperations();

        if (errorMsgs.size() > 0) {
            StringBuilder errors = new StringBuilder();
            for (String msg : errorMsgs) {
                if (errors.length() > 0) {
                    errors.append("; ");
                }
                errors.append(msg);
            }
            throw new ExecutionException(errors.toString());
        }

        BufferedDataContainer psmContainer = createPSMContainer(getFilteredPSMList(), exec);

        // execute the peptide analysis
        BufferedDataContainer pepContainer;
        if (m_peptide_infer_peptides.getBooleanValue()) {
            analysisModel.executePeptideOperations();
            pepContainer = createPeptideContainer(getFilteredPeptides(), exec);
        } else {
            pepContainer = exec.createDataContainer(getPeptideTableSpec());
            pepContainer.close();
        }

        // execute the protein analysis
        BufferedDataContainer proteinContainer;
        if (m_protein_infer_proteins.getBooleanValue()) {
            analysisModel.executeProteinOperations();
            proteinContainer = createProteinContainer(getFilteredProteinList(), exec);
        } else {
            proteinContainer = exec.createDataContainer(getProteinTableSpec());
            proteinContainer.close();
        }

        // export the selected level to selected format
        FileStoreURIPortObject fsupo = exportToStoreObject(exec.createFileStore("PIA_export_file"));

        // create the PSM to spectra mapping
        psmToSpectrum = createPSMToSpectrumMapping((IURIPortObject) inObjects[2]);

        return new PortObject[]{psmContainer.getTable(),
                pepContainer.getTable(),
                proteinContainer.getTable(),
                fsupo};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // executed on reset.
        analysisModel = null;

        if (piaTmpFile != null) {
            piaTmpFile.delete();
            piaTmpFile = null;
        }

        filteredProteins = null;
        filteredPSMs = null;

        psmToSpectrum.close();
        psmToSpectrum = null;
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
        out_spec[3] = new URIPortObjectSpec(new String[]{"csv", "mzIdentML", "mzTab", "idXML"});

        return out_spec;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_input_column.saveSettingsTo(settings);
        m_create_psm_sets.saveSettingsTo(settings);
        m_consider_modifications.saveSettingsTo(settings);
        m_export_level.saveSettingsTo(settings);
        m_export_format.saveSettingsTo(settings);

        m_psm_analysis_file_id.saveSettingsTo(settings);
        m_calculate_all_fdr.saveSettingsTo(settings);
        m_calculate_combined_fdr.saveSettingsTo(settings);
        m_all_decoy_strategy.saveSettingsTo(settings);
        m_all_decoy_pattern.saveSettingsTo(settings);
        m_all_used_identifications.saveSettingsTo(settings);
        m_fdr_preferred_scores.saveSettingsTo(settings);
        m_psm_filters.saveSettingsTo(settings);

        m_peptide_infer_peptides.saveSettingsTo(settings);
        m_peptide_analysis_file_id.saveSettingsTo(settings);
        m_peptide_filters.saveSettingsTo(settings);

        m_protein_infer_proteins.saveSettingsTo(settings);
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
        m_input_column.loadSettingsFrom(settings);
        m_create_psm_sets.loadSettingsFrom(settings);
        m_consider_modifications.loadSettingsFrom(settings);
        m_export_level.loadSettingsFrom(settings);
        m_export_format.loadSettingsFrom(settings);

        m_psm_analysis_file_id.loadSettingsFrom(settings);
        m_calculate_all_fdr.loadSettingsFrom(settings);
        m_calculate_combined_fdr.loadSettingsFrom(settings);
        m_all_decoy_strategy.loadSettingsFrom(settings);
        m_all_decoy_pattern.loadSettingsFrom(settings);
        m_all_used_identifications.loadSettingsFrom(settings);
        m_fdr_preferred_scores.loadSettingsFrom(settings);
        m_psm_filters.loadSettingsFrom(settings);

        m_peptide_infer_peptides.loadSettingsFrom(settings);
        m_peptide_analysis_file_id.loadSettingsFrom(settings);
        m_peptide_filters.loadSettingsFrom(settings);

        m_protein_infer_proteins.loadSettingsFrom(settings);
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
        m_input_column.validateSettings(settings);
        m_create_psm_sets.validateSettings(settings);
        m_consider_modifications.validateSettings(settings);
        m_export_level.validateSettings(settings);
        m_export_format.validateSettings(settings);

        m_psm_analysis_file_id.validateSettings(settings);
        m_calculate_all_fdr.validateSettings(settings);
        m_calculate_combined_fdr.validateSettings(settings);
        m_all_decoy_strategy.validateSettings(settings);
        m_all_decoy_pattern.validateSettings(settings);
        m_all_used_identifications.validateSettings(settings);
        m_fdr_preferred_scores.validateSettings(settings);
        m_psm_filters.validateSettings(settings);

        m_peptide_infer_peptides.validateSettings(settings);
        m_peptide_analysis_file_id.validateSettings(settings);
        m_peptide_filters.validateSettings(settings);

        m_protein_infer_proteins.validateSettings(settings);
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

        // TODO: use own DataType for representation of PSM, peptides and proteins

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
            Double retentionTime = psm.getRetentionTime();
            if (retentionTime == null) {
                psmCells.add(DataType.getMissingCell());
            } else {
                psmCells.add(new DoubleCell(retentionTime));
            }

            // charge
            psmCells.add(new IntCell(psm.getMissedCleavages()));

            // source ID
            String sourceID = psm.getSourceID();
            if (sourceID == null) {
                psmCells.add(DataType.getMissingCell());
            } else {
                psmCells.add(new StringCell(psm.getSourceID()));
            }

            // spectrum title
            String title = psm.getSpectrumTitle();
            if (title == null) {
                psmCells.add(DataType.getMissingCell());
            } else {
                psmCells.add(new StringCell(psm.getSpectrumTitle()));
            }

            // number of identifications
            if (m_psm_analysis_file_id.getIntValue() == 0) {
                psmCells.add(new IntCell(((ReportPSMSet)psm).getPSMs().size()));
            }

            // scores
            List<DoubleCell> scoresList = new ArrayList<DoubleCell>();
            List<StringCell> scoreNamesList = new ArrayList<StringCell>();
            List<StringCell> scoreShortsList = new ArrayList<StringCell>();
            for (String scoreShort : psmScoreShorts) {
                Double scoreValue = psm.getScore(scoreShort);

                if (((scoreValue == null) || scoreValue.equals(Double.NaN))
                        && (psm instanceof ReportPSMSet)) {
                    // PSM level FDRScore is only valid as a bestScore
                    scoreValue = ((ReportPSMSet) psm).getBestScore(scoreShort);
                }

                scoresList.add(new DoubleCell(scoreValue));
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

        protCols.add(new DataColumnSpecCreator("clusterID", IntCell.TYPE).createSpec());

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

            // the cluster ID
            proteinCells.add(new IntCell(new Long(protein.getAccessions().get(0).getGroup().getTreeID()).intValue()));


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
     * Returns the list of filtered PSMs
     *
     * @return
     */
    public List<PSMReportItem> getFilteredPSMList() {
        if (analysisModel != null) {
            if (filteredPSMs == null) {
                filteredPSMs = analysisModel.getFilteredReportPSMs(
                        m_psm_analysis_file_id.getIntValue(), m_psm_filters.getStringArrayValue());
            }
        }

        return filteredPSMs;
    }


    /**
     * Returns the list of filtered peptides
     *
     * @return
     */
    public List<ReportPeptide> getFilteredPeptides() {
        if (analysisModel != null) {
            if (filteredPeptides == null) {
                filteredPeptides = analysisModel.getFilteredReportPeptides(
                        m_peptide_analysis_file_id.getIntValue(), m_peptide_filters.getStringArrayValue());
            }
        }

        return filteredPeptides;
    }


    /**
     * Returns the list of filtered proteins
     *
     * @return
     */
    public List<ReportProtein> getFilteredProteinList() {
        if (analysisModel != null) {
            if (filteredProteins == null) {
                filteredProteins =  analysisModel.getFilteredReportProteins(
                        m_protein_filters.getStringArrayValue());
            }
        }

        return filteredProteins;
    }


    /**
     * Gets the file name of the PIA XML file from the data table cell. If the
     * cell is a String or URL, return the absolute path. If the cell is a
     * binary object, gunzip the file temporarily and return the unzipped file
     * name.
     *
     * @param dataCell
     * @return
     * @throws IOException
     */
    private String getFilenameFromTableCell(DataCell dataCell) throws IOException {
        String fileName = null;

        if (dataCell.getType().isCompatible(StringValue.class)) {
            // an URL or file name
            String fileURL = ((StringValue) dataCell).getStringValue();
            File file = null;
            try {
                // try with URL encoding
                URL url = new URL(fileURL);
                file = new File(url.toURI());
            } catch (Exception e) {
                file = null;
            }

            if ((file == null) || !file.exists() || !file.canRead()) {
                // try with "normal" file name
                file = new File(fileURL);
            }

            if ((file != null) && file.exists()) {
                fileName = file.getAbsolutePath();
            }
        } else if (dataCell.getType().isCompatible(BinaryObjectDataValue.class)) {
            // it is a binary file , either GZipped or not
            InputStream is = new BufferedInputStream(((BinaryObjectDataValue) dataCell).openInputStream());
            byte[] buffer = new byte[1024];

            is.mark(2);
            int magic = 0;
            magic = is.read() & 0xff | ((is.read() << 8) & 0xff00);
            is.reset();

            if (magic == GZIPInputStream.GZIP_MAGIC) {
                // file is gzipped
                LOGGER.info("binary input file is gzipped");
                is = new GZIPInputStream(is);
            }

            piaTmpFile = File.createTempFile("piaIntermediateFile", "pia.xml");
            piaTmpFile.deleteOnExit();

            FileOutputStream fos = new FileOutputStream(piaTmpFile, false);

            LOGGER.debug("writing unzipped file to " + piaTmpFile.getAbsolutePath());
            int len;
            while((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            LOGGER.debug("outputStream closed");
            is.close();
            LOGGER.debug("inputStream closed");

            fileName = piaTmpFile.getAbsolutePath();
        }

        return fileName;
    }


    /**
     * Exports the report to the given {@link FileStore}.
     *
     * @param fileStore
     * @return
     * @throws IOException
     */
    private FileStoreURIPortObject exportToStoreObject(FileStore fileStore) throws IOException {
        FileStoreURIPortObject fsupo = new FileStoreURIPortObject(fileStore);

        if (!m_export_level.getStringValue().equals(ExportLevels.none.toString())) {
            String file_basename = "piaExport";
            File file = fsupo.registerFile(file_basename + "."
                    + m_export_format.getStringValue());
            file.createNewFile();

            Long fileID;
            ExportLevels exportLvl = ExportLevels.valueOf(m_export_level.getStringValue());
            switch (exportLvl) {
            case PSM:
                fileID = (long)(m_psm_analysis_file_id.getIntValue());
                break;

            case peptide:
                fileID = (long)(m_peptide_analysis_file_id.getIntValue());
                if (!m_peptide_infer_peptides.getBooleanValue()) {
                    LOGGER.warn("Peptide inference is deactivated, but peptide level export on. The export is performed, but might not be as expected!");
                }
                break;

            case protein:
                fileID = 0L;
                if (!m_protein_infer_proteins.getBooleanValue()) {
                    LOGGER.warn("Protein inference is deactivated, but peptide level export on. The export is performed, but might not be as expected!");
                }
                break;

            case none:
            default:
                fileID = -1L;
            }

            exportReportTo(file, analysisModel.getPIAModeller(),
                    ExportFormats.valueOf(m_export_format.getStringValue()),
                    exportLvl, fileID);

        } else {
            File file = fsupo.registerFile("emptyfile.txt");
            file.createNewFile();
        }

        return fsupo;
    }


    /**
     * Exports the PIA analysis to the given file format.
     *
     * @param file the file, must be created
     * @param piaModeller {@link PIAModeller} containing the analysis
     * @param exportFormat format to export to
     * @param exportLevel the level (PSM, peptide or protein)
     * @param fileID the file ID (0=overview, not used for protein level)
     */
    private void exportReportTo(File file, PIAModeller piaModeller,
            ExportFormats exportFormat, ExportLevels exportLevel, Long fileID) {
        LOGGER.debug("Exporting to " + exportFormat + " (" + file.getAbsolutePath() + "), "
                + exportLevel + " (" + fileID + ")");

        switch (exportFormat) {
        case idXML:
            IdXMLExporter idXMLexporter = new IdXMLExporter(piaModeller);
            idXMLexporter.exportToIdXML(fileID, file, exportLevel.equals(ExportLevels.protein));
            break;

        case mzIdentML:
            MzIdentMLExporter mzIDexporter = new MzIdentMLExporter(piaModeller);
            mzIDexporter.exportToMzIdentML(fileID, file, exportLevel.equals(ExportLevels.protein),
                    false /* no filtering */);
            break;

        case mzTab:
            MzTabExporter mzTabExporter = new MzTabExporter(piaModeller);
            mzTabExporter.exportToMzTab(fileID, file, exportLevel.equals(ExportLevels.protein),
                    false /* TODO: implement peptide level statistics */, false /* no filtering */);
            break;

        case csv:
            // TODO: implement

        default:
            LOGGER.warn("Unimplemented export format: " + exportFormat);
            break;
        }
    }


    /**
     * Creates the PSM to spectrum mapper from the spectrum file on the given
     * port.
     *
     * @param filePort
     * @return null, if no spectrum file was found on the port
     */
    private PiaPsmToSpectrum<ReportPSM> createPSMToSpectrumMapping(IURIPortObject filePort) {
        PiaPsmToSpectrum<ReportPSM> psmToSpec = null;

        // get the input file from the file port input (if not set before)
        if (filePort != null) {
            List<URIContent> uris = filePort.getURIContents();
            ListIterator<URIContent> uriIter = uris.listIterator();

            while (uriIter.hasNext()) {
                URI uri = uriIter.next().getURI();
                File portFile = new File(uri);

                LOGGER.info("Matching PSMs to spectra in file " + portFile.getAbsolutePath());
                psmToSpec = new PiaPsmToSpectrum<>(portFile, getAllReportPSMs());

                if (psmToSpec.getNrNullMatches() > 0) {
                    LOGGER.warn("There were " + psmToSpec.getNrNullMatches() + " PSMs, that could not be matched to spectra.");
                }

                if (uriIter.hasNext()) {
                    LOGGER.warn("Only the first suitable entry in the port for spectra file is used.");
                }
                break;
            }
        } else {
            LOGGER.debug("no spectrum file given");
        }

        return psmToSpec;
    }


    /**
     * Returns a List of all ReportPSMs in the analysis.
     *
     * @return
     */
    private List<ReportPSM> getAllReportPSMs() {
        List<ReportPSMSet> psmSets = analysisModel.getPSMModeller().getFilteredReportPSMSets(null);
        ListIterator<ReportPSMSet> setIterator = psmSets.listIterator();

        // guess the size of returned PSMs using the number of files
        int listSize = psmSets.size();
        if (analysisModel.getPIAModeller().getCreatePSMSets()) {
            listSize = listSize * analysisModel.getPIAModeller().getFiles().size();
        }
        List<ReportPSM> psmList = new ArrayList<>(listSize);

        while (setIterator.hasNext()) {
            ReportPSMSet psmSet = setIterator.next();
            psmList.addAll(psmSet.getPSMs());
        }

        LOGGER.debug("items in PSM list " + psmList.size());
        return psmList;
    }


    /**
     * getter for the PSM to spectrum matcher
     * @return
     */
    public PiaPsmToSpectrum<ReportPSM> getPSMToSpectrum() {
        return psmToSpectrum;
    }
}

