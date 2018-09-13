package de.mpc.pia.knime.nodes.analysis;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
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
import de.mpc.pia.knime.nodes.PIANodesPlugin;
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
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.tools.matomo.PIAMatomoTracker;
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
    private static final NodeLogger LOGGER = NodeLogger.getLogger(PIAAnalysisNodeModel.class);


    /** the model of the input files' URLs */
    private final SettingsModelString mInputColumn =
            new SettingsModelString(PIASettings.CONFIG_INPUT_COLUMN.getKey(), PIASettings.CONFIG_INPUT_COLUMN.getDefaultString());
    /** storing model for whether PSM sets should be created */
    private final SettingsModelBoolean mCreatePSMSets =
            new SettingsModelBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean());
    /** storing model for whether modifications should be used to distinguish peptides */
    private final SettingsModelBoolean mConsiderModifications =
            new SettingsModelBoolean(PIASettings.CONSIDER_MODIFICATIONS.getKey(), PIASettings.CONSIDER_MODIFICATIONS.getDefaultBoolean());

    /** filter the export */
    private final SettingsModelBoolean mExportFilter =
            new SettingsModelBoolean(PIASettings.EXPORT_FILTER.getKey(), PIASettings.EXPORT_FILTER.getDefaultBoolean());
    /** export level */
    private final SettingsModelString mExportLevel =
            new SettingsModelString(PIASettings.EXPORT_LEVEL.getKey(), PIASettings.EXPORT_LEVEL.getDefaultString());
    /** export format */
    private final SettingsModelString mExportFormat =
            new SettingsModelString(PIASettings.EXPORT_FORMAT.getKey(), PIASettings.EXPORT_FORMAT.getDefaultString());
    /** export file basename */
    private final SettingsModelString mExportFileBaseName =
            new SettingsModelString(PIASettings.EXPORT_FILEBASENAME.getKey(), PIASettings.EXPORT_FILEBASENAME.getDefaultString());


    /** the file ID for the PSM analysis */
    private final SettingsModelInteger mPSMAnalysisFileId =
            new SettingsModelInteger(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(), PIASettings.PSM_ANALYSIS_FILE_ID.getDefaultInteger());

    /** storing model for whether all FDRs should be calculated */
    private final SettingsModelBoolean mCalculateAllFDR =
            new SettingsModelBoolean(PIASettings.CALCULATE_ALL_FDR.getKey(), PIASettings.CALCULATE_ALL_FDR.getDefaultBoolean());
    /** storing model for whether the Combined FDR Score should be calculated */
    private final SettingsModelBoolean mCalculateCombinedFDR =
            new SettingsModelBoolean(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(), PIASettings.CALCULATE_COMBINED_FDR_SCORE.getDefaultBoolean());

    /** storing model for the used strategy of decoy identification for all files */
    private final SettingsModelString mAllDecoyStrategy =
            new SettingsModelString(PIASettings.ALL_DECOY_STRATEGY.getKey(), PIASettings.ALL_DECOY_STRATEGY.getDefaultString());
    /** storing model for the used decoy pattern for all files */
    private final SettingsModelString mAllDecoyPattern =
            new SettingsModelString(PIASettings.ALL_DECOY_PATTERN.getKey(), PIASettings.ALL_DECOY_PATTERN.getDefaultString());

    /** used identifications (for FDR calculation) for all files */
    private final SettingsModelInteger mAllUsedIdentifications =
            new SettingsModelInteger(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(), PIASettings.ALL_USED_IDENTIFICATIONS.getDefaultInteger());

    /** storing model for preferred scores for FDR calculation */
    private final SettingsModelStringArray mFDRPreferredScores =
            new SettingsModelStringArray(PIASettings.FDR_PREFERRED_SCORES.getKey(), PIASettings.FDR_PREFERRED_SCORES.getDefaultStringArray());

    /** storing model for PSM level filters */
    private final SettingsModelStringArray mPSMFilters =
            new SettingsModelStringArray(PIASettings.PSM_FILTERS.getKey(), PIASettings.PSM_FILTERS.getDefaultStringArray());


    /** whether to infer peptides */
    private final SettingsModelBoolean mPeptideInferPeptides =
            new SettingsModelBoolean(PIASettings.PEPTIDE_INFER_PEPTIDES.getKey(), PIASettings.PEPTIDE_INFER_PEPTIDES.getDefaultBoolean());

    /** the file ID for the peptide analysis */
    private final SettingsModelInteger mPeptideAnalysisFileId =
            new SettingsModelInteger(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(), PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getDefaultInteger());

    /** storing model for peptide level filters */
    private final SettingsModelStringArray mPeptideFilters =
            new SettingsModelStringArray(PIASettings.PEPTIDE_FILTERS.getKey(), PIASettings.PEPTIDE_FILTERS.getDefaultStringArray());


    /** whether to infer proteins */
    private final SettingsModelBoolean mProteinInferProteins =
            new SettingsModelBoolean(PIASettings.PROTEIN_INFER_PROTEINS.getKey(), PIASettings.PROTEIN_INFER_PROTEINS.getDefaultBoolean());

    /** storing model for the used protein inference method */
    private final SettingsModelString mProteinInferenceMethod =
            new SettingsModelString(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(), PIASettings.PROTEIN_INFERENCE_METHOD.getDefaultString());
    /** storing model for the protein inference filters */
    private final SettingsModelStringArray mProteinInferenceFilters =
            new SettingsModelStringArray(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(), PIASettings.PROTEIN_INFERENCE_FILTERS.getDefaultStringArray());
    /** storing model for the scoring method used by the protein inference */
    private final SettingsModelString mProteinScoringMethod =
            new SettingsModelString(PIASettings.PROTEIN_SCORING_METHOD.getKey(), PIASettings.PROTEIN_SCORING_METHOD.getDefaultString());
    /** storing model for the base score used by the scoring method of the protein inference */
    private final SettingsModelString mProteinScoringScore =
            new SettingsModelString(PIASettings.PROTEIN_SCORING_SCORE.getKey(), PIASettings.PROTEIN_SCORING_SCORE.getDefaultString());
    /** storing model for the PSMs used by the scoring during the protein inference  */
    private final SettingsModelString mProteinScoringUsedPSMs =
            new SettingsModelString(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(), PIASettings.PROTEIN_SCORING_USED_PSMS.getDefaultString());

    /** storing model for protein level filters */
    private final SettingsModelStringArray mProteinFilters =
            new SettingsModelStringArray(PIASettings.PROTEIN_FILTERS.getKey(), PIASettings.PROTEIN_FILTERS.getDefaultStringArray());


    /** temporary created file, delete on reset */
    private File piaXMLTmpFile;

    /** the reported PSMs */
    private List<PSMReportItem> filteredPSMs;

    /** the reported peptides */
    private List<ReportPeptide> filteredPeptides;

    /** the reported proteins */
    private List<ReportProtein> filteredProteins;

    /** the PSM to spectrum mapper */
    private PiaPsmToSpectrum<ReportPSM> psmToSpectrum;

    /** where to find the analysis model */
    private File piaAnalysisModelFile;

    /** where to find the analysis model's settings */
    private File piaAnalysisSettingsFile;


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

        piaXMLTmpFile = null;
        filteredProteins = null;
        filteredPeptides = null;
        filteredPSMs = null;
        psmToSpectrum = null;
        piaAnalysisModelFile = null;
        piaAnalysisSettingsFile = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        String piaXmlFileName = null;

        // get the input file from the first port, first object
        if ((mInputColumn.getStringValue() != null) && (inObjects[0] != null)) {
            DataTable table = (DataTable)inObjects[0];
            RowIterator rowIt = table.iterator();
            int inputIdx = table.getDataTableSpec().findColumnIndex(mInputColumn.getStringValue());
            if (inputIdx < 0) {
                throw new ExecutionException("Could not find column '" + mInputColumn.getStringValue()
                        + "' in input table. Settings must be reconfigured.");
            }
            while (rowIt.hasNext()) {
                DataRow row = rowIt.next();
                DataCell dataCell = row.getCell(inputIdx);

                piaXmlFileName = getFilenameFromTableCell(dataCell);

                if (piaXmlFileName != null) {
                    if (rowIt.hasNext()) {
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

            if ((piaXmlFileName != null) && !uris.isEmpty()) {
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
            throw new ExecutionException("No PIA XML file given! Provide either by "
                    + "datatable (e.g. from PIA Compiler or List Files) or port (Input File)");
        }

        PIAMatomoTracker.disableTracking(PIANodesPlugin.isUsageStatisticsDisabled());
        PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_KNIME_CATEGORY,
                PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                PIAMatomoTracker.PIA_TRACKING_MODELLER_KNIME_STARTED, null,
                PIANodesPlugin.getVisitorCid());

        // create modeller and load the file
        PIAModeller piaModeller = new PIAModeller(piaXmlFileName);
        PIAAnalysisModel analysisModel = new PIAAnalysisModel(piaModeller);

        // set whether PSM sets should be created
        analysisModel.addSetting(PIASettings.CREATE_PSMSETS.getKey(),
                mCreatePSMSets.getBooleanValue());
        // set whether modifications should be considered to distinguish peptides
        analysisModel.addSetting(PIASettings.CONSIDER_MODIFICATIONS.getKey(),
                mConsiderModifications.getBooleanValue());

        // set the used decoy strategy for all files
        analysisModel.addSetting(PIASettings.ALL_DECOY_STRATEGY.getKey(),
                mAllDecoyStrategy.getStringValue());
        // set the used decoy pattern for all files
        analysisModel.addSetting(PIASettings.ALL_DECOY_PATTERN.getKey(),
                mAllDecoyPattern.getStringValue());
        // set the used identifications for FDR calculations for all files
        analysisModel.addSetting(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(),
                mAllUsedIdentifications.getIntValue());

        // set the preferred scores for FDR calculation
        analysisModel.addSetting(PIASettings.FDR_PREFERRED_SCORES.getKey(),
                mFDRPreferredScores.getStringArrayValue());

        // set the analyzed fileId for PSM operations
        analysisModel.addSetting(PIASettings.PSM_ANALYSIS_FILE_ID.getKey(),
                mPSMAnalysisFileId.getIntValue());
        // set PSM level filters
        analysisModel.addSetting(PIASettings.PSM_FILTERS.getKey(),
                mPSMFilters.getStringArrayValue());

        // set whether all FDRs should be calculated
        analysisModel.addSetting(PIASettings.CALCULATE_ALL_FDR.getKey(),
                mCalculateAllFDR.getBooleanValue());
        // set whether the Combined FDR Score should be calculated
        analysisModel.addSetting(PIASettings.CALCULATE_COMBINED_FDR_SCORE.getKey(),
                mCalculateCombinedFDR.getBooleanValue());

        // set whether to infer peptides
        analysisModel.addSetting(PIASettings.PEPTIDE_INFER_PEPTIDES.getKey(),
                mPeptideInferPeptides.getBooleanValue());
        // set the analyzed fileId for peptide operations
        analysisModel.addSetting(PIASettings.PEPTIDE_ANALYSIS_FILE_ID.getKey(),
                mPeptideAnalysisFileId.getIntValue());
        // set peptide level filters
        analysisModel.addSetting(PIASettings.PEPTIDE_FILTERS.getKey(),
                mPeptideFilters.getStringArrayValue());

        // set whether to infer proteins
        analysisModel.addSetting(PIASettings.PROTEIN_INFER_PROTEINS.getKey(),
                mProteinInferProteins.getBooleanValue());
        // set the protein inference methods
        analysisModel.addSetting(PIASettings.PROTEIN_INFERENCE_METHOD.getKey(),
                mProteinInferenceMethod.getStringValue());
        // set filters for the protein inference
        analysisModel.addSetting(PIASettings.PROTEIN_INFERENCE_FILTERS.getKey(),
                mProteinInferenceFilters.getStringArrayValue());
        // set the scoring used during the protein inference
        analysisModel.addSetting(PIASettings.PROTEIN_SCORING_METHOD.getKey(),
                mProteinScoringMethod.getStringValue());
        // set the base score used by the protein inference
        analysisModel.addSetting(PIASettings.PROTEIN_SCORING_SCORE.getKey(),
                mProteinScoringScore.getStringValue());
        // set the used PSMs for the scoring
        analysisModel.addSetting(PIASettings.PROTEIN_SCORING_USED_PSMS.getKey(),
                mProteinScoringUsedPSMs.getStringValue());


        // filter the export
        analysisModel.addSetting(PIASettings.EXPORT_FILTER.getKey(),
                mExportFilter.getBooleanValue());
        // export format
        analysisModel.addSetting(PIASettings.EXPORT_FORMAT.getKey(),
                mExportFormat.getStringValue());
        // export level
        analysisModel.addSetting(PIASettings.EXPORT_LEVEL.getKey(),
                mExportLevel.getStringValue());


        // execute the PSM analysis
        List<String> errorMsgs = analysisModel.executePSMOperations();

        if (!errorMsgs.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (String msg : errorMsgs) {
                if (errors.length() > 0) {
                    errors.append("; ");
                }
                errors.append(msg);
            }
            throw new ExecutionException(errors.toString());
        }

        BufferedDataContainer psmContainer = createPSMContainer(analysisModel, exec);

        // execute the peptide analysis
        BufferedDataContainer pepContainer;
        if (mPeptideInferPeptides.getBooleanValue()) {
            analysisModel.executePeptideOperations();
            pepContainer = createPeptideContainer(analysisModel, exec);
        } else {
            pepContainer = exec.createDataContainer(getPeptideTableSpec());
            pepContainer.close();
        }

        // execute the protein analysis
        BufferedDataContainer proteinContainer;
        if (mProteinInferProteins.getBooleanValue()) {
            analysisModel.executeProteinOperations();
            proteinContainer = createProteinContainer(analysisModel, exec);
        } else {
            proteinContainer = exec.createDataContainer(getProteinTableSpec());
            proteinContainer.close();
        }

        // export the selected level to selected format
        FileStoreURIPortObject fsupo = exportToStoreObject(exec.createFileStore("PIA_export_file"), analysisModel);

        // create the PSM to spectra mapping
        psmToSpectrum = createPSMToSpectrumMapping((IURIPortObject) inObjects[2], analysisModel);

        // save the model and settings to disk
        piaAnalysisModelFile = File.createTempFile("piaAnalysisModel-", "");
        piaAnalysisModelFile.deleteOnExit();
        analysisModel.saveModelTo(piaAnalysisModelFile);

        piaAnalysisSettingsFile = File.createTempFile("piaAnalysisModelSettings-", "");
        piaAnalysisSettingsFile.deleteOnExit();
        analysisModel.saveSettingsTo(piaAnalysisSettingsFile);

        PIAMatomoTracker.trackPIAEvent(PIAMatomoTracker.PIA_TRACKING_KNIME_CATEGORY,
                PIAMatomoTracker.PIA_TRACKING_MODELLER_NAME,
                PIAMatomoTracker.PIA_TRACKING_MODELLER_FINISHED, null,
                PIANodesPlugin.getVisitorCid());

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
        if (piaAnalysisModelFile != null) {
            piaAnalysisModelFile.delete();
            piaAnalysisModelFile = null;
        }
        if (piaAnalysisSettingsFile != null) {
            piaAnalysisSettingsFile.delete();
            piaAnalysisSettingsFile = null;
        }

        if (piaXMLTmpFile != null) {
            piaXMLTmpFile.delete();
            piaXMLTmpFile = null;
        }

        filteredProteins = null;
        filteredPeptides = null;
        filteredPSMs = null;

        if (psmToSpectrum != null) {
            psmToSpectrum.close();
            psmToSpectrum = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        PortObjectSpec[] outSpec = new PortObjectSpec[4];

        outSpec[0] = getPSMTableSpec();
        outSpec[1] = getPeptideTableSpec();
        outSpec[2] = getProteinTableSpec();
        outSpec[3] = new URIPortObjectSpec("csv", "mzIdentML", "mzTab", "idXML");

        return outSpec;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        mInputColumn.saveSettingsTo(settings);
        mCreatePSMSets.saveSettingsTo(settings);
        mConsiderModifications.saveSettingsTo(settings);
        mExportFilter.saveSettingsTo(settings);
        mExportLevel.saveSettingsTo(settings);
        mExportFormat.saveSettingsTo(settings);
        mExportFileBaseName.saveSettingsTo(settings);

        mPSMAnalysisFileId.saveSettingsTo(settings);
        mCalculateAllFDR.saveSettingsTo(settings);
        mCalculateCombinedFDR.saveSettingsTo(settings);
        mAllDecoyStrategy.saveSettingsTo(settings);
        mAllDecoyPattern.saveSettingsTo(settings);
        mAllUsedIdentifications.saveSettingsTo(settings);
        mFDRPreferredScores.saveSettingsTo(settings);
        mPSMFilters.saveSettingsTo(settings);

        mPeptideInferPeptides.saveSettingsTo(settings);
        mPeptideAnalysisFileId.saveSettingsTo(settings);
        mPeptideFilters.saveSettingsTo(settings);

        mProteinInferProteins.saveSettingsTo(settings);
        mProteinInferenceMethod.saveSettingsTo(settings);
        mProteinInferenceFilters.saveSettingsTo(settings);
        mProteinScoringMethod.saveSettingsTo(settings);
        mProteinScoringScore.saveSettingsTo(settings);
        mProteinScoringUsedPSMs.saveSettingsTo(settings);
        mProteinFilters.saveSettingsTo(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        mInputColumn.loadSettingsFrom(settings);
        mCreatePSMSets.loadSettingsFrom(settings);
        mConsiderModifications.loadSettingsFrom(settings);
        mExportFilter.setBooleanValue(settings.getBoolean(PIASettings.EXPORT_FILTER.getKey(), PIASettings.EXPORT_FILTER.getDefaultBoolean()));
        mExportLevel.loadSettingsFrom(settings);
        mExportFormat.loadSettingsFrom(settings);
        mExportFileBaseName.setStringValue(settings.getString(PIASettings.EXPORT_FILEBASENAME.getKey(), PIASettings.EXPORT_FILEBASENAME.getDefaultString()));

        mPSMAnalysisFileId.loadSettingsFrom(settings);
        mCalculateAllFDR.loadSettingsFrom(settings);
        mCalculateCombinedFDR.loadSettingsFrom(settings);
        mAllDecoyStrategy.loadSettingsFrom(settings);
        mAllDecoyPattern.loadSettingsFrom(settings);
        mAllUsedIdentifications.loadSettingsFrom(settings);
        mFDRPreferredScores.loadSettingsFrom(settings);
        mPSMFilters.loadSettingsFrom(settings);

        mPeptideInferPeptides.loadSettingsFrom(settings);
        mPeptideAnalysisFileId.loadSettingsFrom(settings);
        mPeptideFilters.loadSettingsFrom(settings);

        mProteinInferProteins.loadSettingsFrom(settings);
        mProteinInferenceMethod.loadSettingsFrom(settings);
        mProteinInferenceFilters.loadSettingsFrom(settings);
        mProteinScoringMethod.loadSettingsFrom(settings);
        mProteinScoringScore.loadSettingsFrom(settings);
        mProteinScoringUsedPSMs.loadSettingsFrom(settings);
        mProteinFilters.loadSettingsFrom(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        mInputColumn.validateSettings(settings);
        mCreatePSMSets.validateSettings(settings);
        mConsiderModifications.validateSettings(settings);
        settings.getBoolean(PIASettings.EXPORT_FILTER.getKey(), PIASettings.EXPORT_FILTER.getDefaultBoolean());
        mExportLevel.validateSettings(settings);
        mExportFormat.validateSettings(settings);
        settings.getString(PIASettings.EXPORT_FILEBASENAME.getKey(), PIASettings.EXPORT_FILEBASENAME.getDefaultString());

        mPSMAnalysisFileId.validateSettings(settings);
        mCalculateAllFDR.validateSettings(settings);
        mCalculateCombinedFDR.validateSettings(settings);
        mAllDecoyStrategy.validateSettings(settings);
        mAllDecoyPattern.validateSettings(settings);
        mAllUsedIdentifications.validateSettings(settings);
        mFDRPreferredScores.validateSettings(settings);
        mPSMFilters.validateSettings(settings);

        mPeptideInferPeptides.validateSettings(settings);
        mPeptideAnalysisFileId.validateSettings(settings);
        mPeptideFilters.validateSettings(settings);

        mProteinInferProteins.validateSettings(settings);
        mProteinInferenceMethod.validateSettings(settings);
        mProteinInferenceFilters.validateSettings(settings);
        mProteinScoringMethod.validateSettings(settings);
        mProteinScoringScore.validateSettings(settings);
        mProteinScoringUsedPSMs.validateSettings(settings);
        mProteinFilters.validateSettings(settings);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        piaAnalysisModelFile = PIAAnalysisModel.getInternalModelFileFromDir(internDir);
        piaAnalysisSettingsFile = PIAAnalysisModel.getInternalSettingsFileFromDir(internDir);

        // TODO: also load the PSMToSpectrum
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (piaAnalysisModelFile != null) {
            piaAnalysisModelFile = PIAAnalysisModel.moveModelToInternal(internDir, piaAnalysisModelFile);
            piaAnalysisSettingsFile = PIAAnalysisModel.moveSettingsToInternal(internDir, piaAnalysisSettingsFile);
        }

        // TODO: also save the PSMToSpectrum
    }


    /**
     * Returns the PSM table specs
     *
     * @return
     */
    private DataTableSpec getPSMTableSpec() {
        // TODO: select one "main-score", which should be set for each fileID (selection by the psmModeller?)

        List<DataColumnSpec> psmCols = new ArrayList<>();
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

        if (mPSMAnalysisFileId.getIntValue() == 0) {
            psmCols.add(new DataColumnSpecCreator("nrIdentifications", IntCell.TYPE).createSpec());
        }

        psmCols.add(new DataColumnSpecCreator("scores", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());
        psmCols.add(new DataColumnSpecCreator("score names", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        psmCols.add(new DataColumnSpecCreator("score shorts", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        return new DataTableSpec(psmCols.toArray(new DataColumnSpec[]{}));
    }


    /**
     * Creates the PSM result data container.
     *
     * @param psmList
     * @param exec
     * @return
     */
    private BufferedDataContainer createPSMContainer(PIAAnalysisModel analysisModel, final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getPSMTableSpec());
        Integer psmId = 0;

        Map<String, String> scoreShortsToNames = analysisModel.getPSMModeller().getScoreShortsToScoreNames();
        List<String> psmScoreShorts = analysisModel.getPSMScoreShorts(mPSMAnalysisFileId.getIntValue());

        List<PSMReportItem> psmList = getFilteredPSMList(analysisModel);

        for (PSMReportItem psm : psmList) {
            psmId++;
            RowKey key = new RowKey(psmId.toString());

            List<DataCell> psmCells = new ArrayList<>();

            // sequence
            psmCells.add(new StringCell(psm.getSequence()));

            // accessions
            List<StringCell> accList = new ArrayList<>(psm.getAccessions().size());
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
            if (mPSMAnalysisFileId.getIntValue() == 0) {
                psmCells.add(new IntCell(((ReportPSMSet)psm).getPSMs().size()));
            }

            // scores
            List<DoubleCell> scoresList = new ArrayList<>();
            List<StringCell> scoreNamesList = new ArrayList<>();
            List<StringCell> scoreShortsList = new ArrayList<>();
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

        List<DataColumnSpec> pepCols = new ArrayList<>();
        pepCols.add(new DataColumnSpecCreator("Sequence", StringCell.TYPE).createSpec());
        pepCols.add(new DataColumnSpecCreator("Accessions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        if (mConsiderModifications.getBooleanValue()) {
            pepCols.add(new DataColumnSpecCreator("Modifications", StringCell.TYPE).createSpec());
        }

        pepCols.add(new DataColumnSpecCreator("number Spectra", IntCell.TYPE).createSpec());

        if (mPeptideAnalysisFileId.getIntValue() == 0) {
            pepCols.add(new DataColumnSpecCreator("number PSM sets", IntCell.TYPE).createSpec());
        } else {
            pepCols.add(new DataColumnSpecCreator("number PSMs", IntCell.TYPE).createSpec());
        }

        pepCols.add(new DataColumnSpecCreator("Missed Cleavages", IntCell.TYPE).createSpec());

        pepCols.add(new DataColumnSpecCreator("best scores", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());
        pepCols.add(new DataColumnSpecCreator("score names", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        pepCols.add(new DataColumnSpecCreator("score shorts", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        if (mCalculateAllFDR.getBooleanValue()) {
            pepCols.add(new DataColumnSpecCreator("FDR Score", DoubleCell.TYPE).createSpec());
        }

        return new DataTableSpec(pepCols.toArray(new DataColumnSpec[]{}));
    }


    /**
     * Creates the peptide result data container.
     *
     * @param psmList
     * @param exec
     * @return
     */
    private BufferedDataContainer createPeptideContainer(PIAAnalysisModel analysisModel, final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getPeptideTableSpec());
        Integer pepId = 0;

        Map<String, String> scoreShortsToNames = analysisModel.getPSMModeller().getScoreShortsToScoreNames();
        List<String> scoreShorts = analysisModel.getPSMScoreShorts(mPeptideAnalysisFileId.getIntValue());

        List<ReportPeptide> peptideList = getFilteredPeptides(analysisModel);

        for (ReportPeptide pep : peptideList) {
            pepId++;
            RowKey key = new RowKey(pepId.toString());

            List<DataCell> pepCells = new ArrayList<>();

            // sequence
            pepCells.add(new StringCell(pep.getSequence()));

            // accessions
            List<StringCell> accList = new ArrayList<>(pep.getAccessions().size());
            for (Accession acc : pep.getAccessions()) {
                accList.add(new StringCell(acc.getAccession()));
            }
            pepCells.add(CollectionCellFactory.createListCell(accList));

            // modifications
            if (mConsiderModifications.getBooleanValue()) {
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
            List<DoubleCell> scoresList = new ArrayList<>();
            List<StringCell> scoreNamesList = new ArrayList<>();
            List<StringCell> scoreShortsList = new ArrayList<>();

            for (String scoreShort : scoreShorts) {
                scoresList.add(new DoubleCell(pep.getBestScore(scoreShort)));
                scoreNamesList.add(new StringCell(scoreShortsToNames.get(scoreShort)));
                scoreShortsList.add(new StringCell(scoreShort));
            }
            pepCells.add(CollectionCellFactory.createListCell(scoresList));
            pepCells.add(CollectionCellFactory.createListCell(scoreNamesList));
            pepCells.add(CollectionCellFactory.createListCell(scoreShortsList));

            // FDR is calculated on peptide level
            if (mCalculateAllFDR.getBooleanValue()) {
                pepCells.add(new DoubleCell(pep.getFDRScore().getValue()));
            }

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
        List<DataColumnSpec> protCols = new ArrayList<>();
        protCols.add(new DataColumnSpecCreator("Accessions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        protCols.add(new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec());

        protCols.add(new DataColumnSpecCreator("Coverages", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());

        protCols.add(new DataColumnSpecCreator("nrPeptides", IntCell.TYPE).createSpec());
        protCols.add(new DataColumnSpecCreator("nrPSMs", IntCell.TYPE).createSpec());
        protCols.add(new DataColumnSpecCreator("nrSpectra", IntCell.TYPE).createSpec());

        protCols.add(new DataColumnSpecCreator("clusterID", IntCell.TYPE).createSpec());

        protCols.add(new DataColumnSpecCreator("Descriptions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());

        if (mCalculateAllFDR.getBooleanValue() ||
                mCalculateCombinedFDR.getBooleanValue()) {
            protCols.add(new DataColumnSpecCreator("Decoy", BooleanCell.TYPE).createSpec());
            protCols.add(new DataColumnSpecCreator("FDR q-value", DoubleCell.TYPE).createSpec());
        }

        return new DataTableSpec(protCols.toArray(new DataColumnSpec[]{}));
    }


    /**
     * Creates the protein result data container.
     *
     * @param psmList
     * @param exec
     * @return
     */
    private BufferedDataContainer createProteinContainer(PIAAnalysisModel analysisModel, final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getProteinTableSpec());
        Integer protId = 0;

        List<ReportProtein> proteinList = getFilteredProteinList(analysisModel);

        for (ReportProtein protein : proteinList) {
            protId++;
            RowKey key = new RowKey(protId.toString());

            List<DataCell> proteinCells = new ArrayList<>();

            // accessions
            List<StringCell> accList = new ArrayList<>(protein.getAccessions().size());
            List<StringCell> descriptionList = new ArrayList<>(protein.getAccessions().size());
            List<DataCell> coverageList = new ArrayList<>(protein.getAccessions().size());
            for (Accession acc : protein.getAccessions()) {
                accList.add(new StringCell(acc.getAccession()));

                descriptionList.add(new StringCell(acc.getDescription(0L)));

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
            proteinCells.add(new IntCell((int)protein.getAccessions().get(0).getGroup().getTreeID()));

            // the protein description
            proteinCells.add(CollectionCellFactory.createListCell(descriptionList));

            if (mCalculateAllFDR.getBooleanValue() ||
                    mCalculateCombinedFDR.getBooleanValue()) {
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
    public PIAAnalysisModel loadAnalysisModelFromFile() {
        PIAAnalysisModel analysisModel = null;

        try {
            if ((piaAnalysisModelFile != null) && (piaAnalysisSettingsFile != null)) {
                analysisModel = PIAAnalysisModel.loadModelFromInternal(piaAnalysisModelFile, piaAnalysisSettingsFile);
            }
        } catch (IOException e) {
            LOGGER.error("Could not get analysis model", e);
        }

        return analysisModel;
    }


    /**
     * Getter for the analysis model
     * @return
     */
    public int getAnalysisModelFileHash() {
        HashCodeBuilder hcb = new HashCodeBuilder(23, 31);
        int hash = 0;

        if (piaAnalysisModelFile != null) {
            hcb.append(piaAnalysisModelFile.getAbsolutePath());

            try {
                BasicFileAttributes attr = Files.readAttributes(piaAnalysisModelFile.toPath(), BasicFileAttributes.class);

                hcb.append(attr.creationTime());
                hcb.append(attr.lastModifiedTime());
                hcb.append(attr.size());

                hash = hcb.toHashCode();
            } catch (IOException e) {
                hash = 0;
                LOGGER.error("could not read file attributes", e);
            }
        }

        return hash;
    }


    /**
     * Returns the list of filtered PSMs
     *
     * @return
     */
    public List<PSMReportItem> getFilteredPSMList(PIAAnalysisModel analysisModel) {
        if ((analysisModel != null) && (filteredPSMs == null)) {
            filteredPSMs = analysisModel.getFilteredReportPSMs(
                    mPSMAnalysisFileId.getIntValue(), mPSMFilters.getStringArrayValue());
        }

        return filteredPSMs;
    }


    /**
     * Returns the list of filtered peptides
     *
     * @return
     */
    public List<ReportPeptide> getFilteredPeptides(PIAAnalysisModel analysisModel) {
        if ((analysisModel != null) && (filteredPeptides == null)) {
            filteredPeptides = analysisModel.getFilteredReportPeptides(
                    mPeptideAnalysisFileId.getIntValue(), mPeptideFilters.getStringArrayValue());
        }

        return filteredPeptides;
    }


    /**
     * Returns the list of filtered proteins
     *
     * @return
     */
    public List<ReportProtein> getFilteredProteinList(PIAAnalysisModel analysisModel) {
        if ((analysisModel != null) && ( filteredProteins == null)) {
            filteredProteins =  analysisModel.getFilteredReportProteins(
                    mProteinFilters.getStringArrayValue());
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
            InputStream is = null;
            FileOutputStream fos = null;

            try {
                is = new BufferedInputStream(((BinaryObjectDataValue) dataCell).openInputStream());
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

                piaXMLTmpFile = File.createTempFile("piaIntermediateFile", "pia.xml");
                piaXMLTmpFile.deleteOnExit();

                fos = new FileOutputStream(piaXMLTmpFile, false);
                LOGGER.debug("writing unzipped file to " + piaXMLTmpFile.getAbsolutePath());
                int len;
                while((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            } finally {
                //close resources
                if (fos != null) {
                    fos.close();
                    LOGGER.debug("outputStream closed");
                }

                if (is != null) {
                    is.close();
                    LOGGER.debug("inputStream closed");
                }
            }

            fileName = piaXMLTmpFile.getAbsolutePath();
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
    private FileStoreURIPortObject exportToStoreObject(FileStore fileStore, PIAAnalysisModel analysisModel)
            throws IOException {
        FileStoreURIPortObject fsupo = new FileStoreURIPortObject(fileStore);

        boolean exportOK = false;

        if (!mExportLevel.getStringValue().equals(ExportLevels.none.toString())) {
            File file = fsupo.registerFile(buildExportFilename());

            Long fileID;
            ExportLevels exportLvl = ExportLevels.valueOf(mExportLevel.getStringValue());
            switch (exportLvl) {
            case PSM:
                fileID = (long)(mPSMAnalysisFileId.getIntValue());
                break;

            case peptide:
                fileID = (long)(mPeptideAnalysisFileId.getIntValue());
                if (!mPeptideInferPeptides.getBooleanValue()) {
                    LOGGER.warn("Peptide inference is deactivated, but peptide level export on. The export is performed, but might not be as expected!");
                }
                break;

            case protein:
                fileID = 0L;
                if (!mProteinInferProteins.getBooleanValue()) {
                    LOGGER.warn("Protein inference is deactivated, but peptide level export on. The export is performed, but might not be as expected!");
                }
                break;

            case none:
            default:
                fileID = -1L;
            }

            if (file.createNewFile()) {
                exportReportTo(file, analysisModel.getPIAModeller(),
                        ExportFormats.valueOf(mExportFormat.getStringValue()),
                        exportLvl, mExportFilter.getBooleanValue(), fileID);
                exportOK = true;
            } else {
                exportOK = false;
            }
        }

        if (!exportOK) {
            File file = fsupo.registerFile("emptyfile.txt");
            file.createNewFile();
        }

        return fsupo;
    }


    /**
     * creates the export filename from teh given base name
     * @return
     */
    private String buildExportFilename() {
        StringBuilder fileNameSB = new StringBuilder();
        if ((mExportFileBaseName.getStringValue() == null) || mExportFileBaseName.getStringValue().isEmpty()) {
            fileNameSB.append(PIASettings.EXPORT_FILEBASENAME.getDefaultString());
        } else {
            fileNameSB.append(mExportFileBaseName.getStringValue());
        }

        if (!fileNameSB.toString().contains(".")) {
            // append the file suffix
            fileNameSB.append('.');
            fileNameSB.append(mExportFormat.getStringValue());
        }

        return fileNameSB.toString();
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
            ExportFormats exportFormat, ExportLevels exportLevel, boolean filterExport, Long fileID) {
        LOGGER.debug("Exporting to " + exportFormat + " (" + file.getAbsolutePath() + "), "
                + exportLevel + " (" + fileID + "), filter=" + filterExport);

        if (filterExport) {
            if (exportLevel.equals(ExportLevels.PSM)) {
                updatePSMFilters(fileID, piaModeller);
            } else if (exportLevel.equals(ExportLevels.peptide)) {
                updatePeptideFilters(fileID, piaModeller);
            } else {
                updateProteinFilters(piaModeller);
            }
        }

        switch (exportFormat) {
        case idXML:
            IdXMLExporter idXMLexporter = new IdXMLExporter(piaModeller);
            idXMLexporter.exportToIdXML(fileID, file, exportLevel.equals(ExportLevels.protein), filterExport);
            break;

        case mzIdentML:
            MzIdentMLExporter mzIDexporter = new MzIdentMLExporter(piaModeller);
            mzIDexporter.exportToMzIdentML(fileID, file, exportLevel.equals(ExportLevels.protein), filterExport);
            break;

        case mzTab:
            MzTabExporter mzTabExporter = new MzTabExporter(piaModeller);
            mzTabExporter.exportToMzTab(fileID, file, exportLevel.equals(ExportLevels.protein),
                    false /* TODO: implement peptide level statistics */, filterExport);
            break;

        case csv:
            // TODO: implement

        default:
            LOGGER.warn("Unimplemented export format: " + exportFormat);
            break;
        }
    }


    /**
     * Sets the PSM filters of the modeller to the currently set PSM level filters in the dialog.
     *
     * @param fileId
     * @param piaModeller
     */
    private void updatePSMFilters(Long fileId, PIAModeller piaModeller) {
        List<AbstractFilter> newFilters = PIAAnalysisModel.unserializeFilters(mPSMFilters.getStringArrayValue());
        List<AbstractFilter> filters = piaModeller.getPSMModeller().getFilters(fileId);
        filters.clear();
        filters.addAll(newFilters);
    }


    /**
     * Sets the report peptide filters of the dialog to the modeller
     *
     * @param fileId
     * @param piaModeller
     */
    private void updatePeptideFilters(Long fileId, PIAModeller piaModeller) {
        List<AbstractFilter> newFilters = PIAAnalysisModel.unserializeFilters(mPeptideFilters.getStringArrayValue());
        piaModeller.getPeptideModeller().removeAllFilters();
        for (AbstractFilter filter : newFilters) {
            piaModeller.getPeptideModeller().addFilter(fileId, filter);
        }
    }


    /**
     * Sets the report protein filters of the dialog to the modeller
     *
     * @param piaModeller
     */
    private void updateProteinFilters(PIAModeller piaModeller) {
        List<AbstractFilter> newFilters = PIAAnalysisModel.unserializeFilters(mProteinFilters.getStringArrayValue());
        List<AbstractFilter> filters = piaModeller.getProteinModeller().getReportFilters();
        filters.clear();
        filters.addAll(newFilters);
    }



    /**
     * Creates the PSM to spectrum mapper from the spectrum file on the given
     * port.
     *
     * @param filePort
     * @return null, if no spectrum file was found on the port
     */
    private PiaPsmToSpectrum<ReportPSM> createPSMToSpectrumMapping(IURIPortObject filePort,
            PIAAnalysisModel analysisModel) {
        PiaPsmToSpectrum<ReportPSM> psmToSpec = null;

        // get the input file from the file port input (if not set before)
        if (filePort != null) {
            List<URIContent> uris = filePort.getURIContents();
            ListIterator<URIContent> uriIter = uris.listIterator();

            while (uriIter.hasNext()) {
                URI uri = uriIter.next().getURI();
                File portFile = new File(uri);

                LOGGER.info("Matching PSMs to spectra in file " + portFile.getAbsolutePath());
                psmToSpec = new PiaPsmToSpectrum<>(portFile, getAllReportPSMs(analysisModel));

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
    private List<ReportPSM> getAllReportPSMs(PIAAnalysisModel analysisModel) {
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

