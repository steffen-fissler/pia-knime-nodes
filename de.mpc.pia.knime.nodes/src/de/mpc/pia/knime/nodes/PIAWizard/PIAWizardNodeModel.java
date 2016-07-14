package de.mpc.pia.knime.nodes.PIAWizard;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import de.mpc.pia.knime.nodes.PIAAnalysisModel;
import de.mpc.pia.knime.nodes.PIASettings;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.score.FDRData;


/**
 * This is the model implementation of "PIA Wizard".
 * 
 *
 * @author Julian Uszkoreit
 */
public class PIAWizardNodeModel extends NodeModel {
	
	// the logger instance
	protected static final NodeLogger logger = NodeLogger.getLogger(PIAWizardNodeModel.class);
	
	
	// storing models for settings
	private final SettingsModelBoolean m_create_psmsets =
			new SettingsModelBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean());
	private final SettingsModelDoubleBounded m_fdr_threshold = 
			new SettingsModelDoubleBounded(PIASettings.FDR_THRESHOLD.getKey(), PIASettings.FDR_THRESHOLD.getDefaultDouble(), 0, 1);
	
	private final SettingsModelString m_fdr_decoy_strategy =
			new SettingsModelString(PIASettings.ALL_DECOY_STRATEGY.getKey(), PIASettings.ALL_DECOY_STRATEGY.getDefaultString());
	private final SettingsModelString m_fdr_decoy_pattern =
			new SettingsModelString(PIASettings.ALL_DECOY_PATTERN.getKey(), PIASettings.ALL_DECOY_PATTERN.getDefaultString());
	private final SettingsModelInteger m_fdr_used_identifications =
			new SettingsModelInteger(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(), PIASettings.ALL_USED_IDENTIFICATIONS.getDefaultInteger());
	
	private final SettingsModelStringArray m_fdr_preferred_scores =
			new SettingsModelStringArray(PIASettings.FDR_PREFERRED_SCORES.getKey(), PIASettings.FDR_PREFERRED_SCORES.getDefaultStringArray());
	
	
	/** the model, which holds all the data */
	private PIAAnalysisModel m_piaViewModel;
	
	
	/**
	 * Constructor for the node model.
	 */
	protected PIAWizardNodeModel() {
		super(new PortType[]{ IURIPortObject.TYPE },
				new PortType[] { IURIPortObject.TYPE });
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		
		System.out.println("starting the execute");
		
		// get the input piaXML file
		IURIPortObject filePort = (IURIPortObject) inObjects[0];
		List<URIContent> uris = filePort.getURIContents();
		String piaFileName = null; 
		for (URIContent uric : uris) {
			URI uri = uric.getURI();
			piaFileName = new File(uri).getAbsolutePath();
			break;
		}
		
		// create the PIA modeller
		System.out.println("opening file: " + piaFileName);
		
		PIAModeller piaModeller = new PIAModeller(piaFileName);
		
		System.out.println("modeller opened");
		
		
		// create the view model
		m_piaViewModel = new PIAAnalysisModel(piaModeller);
		
		// set the settings
		m_piaViewModel.addSetting(PIASettings.CREATE_PSMSETS.getKey(), m_create_psmsets.getBooleanValue());
		m_piaViewModel.addSetting(PIASettings.FDR_THRESHOLD.getKey(), m_fdr_threshold.getDoubleValue());
		
		m_piaViewModel.addSetting(PIASettings.ALL_DECOY_STRATEGY.getKey(), m_fdr_decoy_strategy.getStringValue());
		m_piaViewModel.addSetting(PIASettings.ALL_DECOY_PATTERN.getKey(), m_fdr_decoy_pattern.getStringValue());
		m_piaViewModel.addSetting(PIASettings.ALL_USED_IDENTIFICATIONS.getKey(), m_fdr_used_identifications.getIntValue());
		
		m_piaViewModel.addSetting(PIASettings.FDR_PREFERRED_SCORES.getKey(), m_fdr_preferred_scores.getStringArrayValue());
		
		// execute the PSM level
		m_piaViewModel.executePSMOperations();
		
		
		
		// here the export should happen...
	    String exportFile = "/dev/null";
		List<URIContent> outExportFile = new ArrayList<URIContent>();
		
		if ((exportFile != null) && Files.exists(new File(exportFile).toPath(), new LinkOption[]{})) {
			outExportFile.add(new URIContent(new File(exportFile).toURI(), "mzTab"));
			
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
		
        return new PortObject[]{outExportFilePort};
	}
	
	
	/**
	 * Getter for the {@link PIAAnalysisModel}.
	 * @return
	 */
	protected PIAAnalysisModel getPIAViewModel() {
		return m_piaViewModel;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
	    // TODO Code executed on reset.
	    // Models build during execute are cleared here.
	    // Also data handled in load/saveInternals will be erased here.
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		
		// TODO: check, whether a correct input was provided
		
		PortObjectSpec[] out_spec = new PortObjectSpec[1];
		out_spec[0] = new URIPortObjectSpec(new String[]{"mzid", "mzIdentML", "CSV"});
		
		return out_spec;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
	    m_create_psmsets.saveSettingsTo(settings);
	    m_fdr_threshold.saveSettingsTo(settings);
	    
	    m_fdr_decoy_strategy.saveSettingsTo(settings);
	    m_fdr_decoy_pattern.saveSettingsTo(settings);
	    m_fdr_used_identifications.saveSettingsTo(settings);
	    
	    m_fdr_preferred_scores.saveSettingsTo(settings);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
	        throws InvalidSettingsException {
		m_create_psmsets.loadSettingsFrom(settings);
	    m_fdr_threshold.loadSettingsFrom(settings);
	    
	    m_fdr_decoy_strategy.loadSettingsFrom(settings);
	    m_fdr_decoy_pattern.loadSettingsFrom(settings);
	    m_fdr_used_identifications.loadSettingsFrom(settings);
	    
	    m_fdr_preferred_scores.loadSettingsFrom(settings);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
	        throws InvalidSettingsException {
		m_create_psmsets.validateSettings(settings);
	    m_fdr_threshold.validateSettings(settings);
	    
	    m_fdr_decoy_strategy.validateSettings(settings);
	    m_fdr_decoy_pattern.validateSettings(settings);
	    m_fdr_used_identifications.validateSettings(settings);
	    
	    m_fdr_preferred_scores.validateSettings(settings);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
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
	        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
	    // TODO save internal models. 
	    // Everything written to output ports is saved automatically (data
	    // returned by the execute method, models saved in the saveModelContent,
	    // and user settings saved through saveSettingsTo - is all taken care 
	    // of). Save here only the other internals that need to be preserved
	    // (e.g. data used by the views).
	}
}