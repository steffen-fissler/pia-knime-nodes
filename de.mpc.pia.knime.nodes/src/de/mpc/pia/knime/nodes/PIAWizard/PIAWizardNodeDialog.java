package de.mpc.pia.knime.nodes.PIAWizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.ProgressMonitor;

import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

import de.mpc.pia.knime.nodes.dialog.WizardPanelPSM;
import de.mpc.pia.knime.nodes.utils.LoadPIAFileWorker;
import de.mpc.pia.modeller.PIAModeller;


/**
 * <code>NodeDialog</code> for the "PIA Wizard" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Julian Uszkoreit
 */
public class PIAWizardNodeDialog extends DataAwareNodeDialogPane implements PropertyChangeListener {
	
	/** used to load the pia file in a thread */
	private LoadPIAFileWorker loadPIAFileworker = null;
	
	/** monitoring the loading status */
	private ProgressMonitor loadPIAFileThreadMonitor = null;
	
	
	private PIAViewModel piaViewModel = null;
	
	
	private WizardPanelPSM wizardPanelPSM;
	
	/**
	 * New pane for configuring PIAknime node dialog.
	 * This is just a suggestion to demonstrate possible default dialog
	 * components.
	 */
	protected PIAWizardNodeDialog() {
		super();
		
		wizardPanelPSM = new WizardPanelPSM(piaViewModel);
		addTab("PIA Wizard - PSM", wizardPanelPSM);
	}
	
	
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings)
			throws InvalidSettingsException {
		Map<String, Object> settingsMap = wizardPanelPSM.getSettings();
		
		settings.addBoolean(PIASettings.CREATE_PSMSETS.getKey(),
				(Boolean)settingsMap.get(PIASettings.CREATE_PSMSETS.getKey()));
		settings.addDouble(PIASettings.FDR_THRESHOLD.getKey(),
				(Double)settingsMap.get(PIASettings.FDR_THRESHOLD.getKey()));
		
		settings.addString(PIASettings.DECOY_STRATEGY.getKey(),
				(String)settingsMap.get(PIASettings.DECOY_STRATEGY.getKey()));
		settings.addString(PIASettings.DECOY_PATTERN.getKey(),
				(String)settingsMap.get(PIASettings.DECOY_PATTERN.getKey()));
		settings.addInt(PIASettings.USED_IDENTIFICATIONS.getKey(),
				(Integer)settingsMap.get(PIASettings.USED_IDENTIFICATIONS.getKey()));
		
		settings.addStringArray(PIASettings.PREFERRED_SCORES.getKey(),
				(String[])settingsMap.get(PIASettings.PREFERRED_SCORES.getKey()));
	}
	
	
	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
                 throws NotConfigurableException {
		PIAWizardNodeModel.logger.warn("calling loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)");
		
		loadPIAFileworker = null;
		piaViewModel = null;
	}
	
	
	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, PortObject[] input)
			throws NotConfigurableException {
		
		PIAWizardNodeModel.logger.warn("calling loadSettingsFrom(NodeSettingsRO settings, PortObject[] input)");
		
		wizardPanelPSM.applyLoadedSettings(settings);
		
		IURIPortObject inputPortObject = (IURIPortObject)input[0];
		List<URIContent> uris = inputPortObject.getURIContents();
		try {
			String inputFile = new File(uris.iterator().next().getURI()).getAbsolutePath();
			loadPIAFileworker = new LoadPIAFileWorker(inputFile, PIAWizardNodeModel.logger);
		} catch (Exception e) {
			PIAWizardNodeModel.logger.error("Could not load file.", e);
			loadPIAFileworker = null;
			piaViewModel = null;
		}
		
		if (loadPIAFileworker != null) {
			PIAWizardNodeModel.logger.warn("creating the loading dialog");
			
			loadPIAFileThreadMonitor = new ProgressMonitor(this.getPanel(), "Loading intermediate file", "", 0, 100);
			loadPIAFileThreadMonitor.setProgress(0);
			loadPIAFileThreadMonitor.setMillisToDecideToPopup(0);
			loadPIAFileThreadMonitor.setMillisToPopup(0);
			
			loadPIAFileworker.addPropertyChangeListener(this);
			wizardPanelPSM.disableAllSettings();
			loadPIAFileworker.execute();
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource().equals(loadPIAFileworker)) {
			int progress = loadPIAFileworker.getLoadingProgress().intValue();
			loadPIAFileThreadMonitor.setProgress(progress);
			String message = String.format("Loading %d%%.\n", progress);
			loadPIAFileThreadMonitor.setNote(message);
			
			if (loadPIAFileThreadMonitor.isCanceled() || loadPIAFileworker.isDone()) {
				if (loadPIAFileThreadMonitor.isCanceled()) {
					loadPIAFileworker.cancel(true);
					piaViewModel = null;
					loadPIAFileworker = null;
                } else {
                	try {
                		piaViewModel = new PIAViewModel(loadPIAFileworker.get());
					} catch (Exception e) {
						PIAWizardNodeModel.logger.error(e);
						piaViewModel = null;
						loadPIAFileworker = null;
					}
                }
				
				wizardPanelPSM.setPIAViewModel(piaViewModel);
				
				wizardPanelPSM.updateAvailableSettings();
				wizardPanelPSM.enableSettings();
				
				wizardPanelPSM.updateFDRPanel();
			}
		}
	}
}