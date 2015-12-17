package de.mpc.pia.knime.nodes.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.knime.core.node.NodeSettingsRO;

import de.mpc.pia.knime.nodes.PIAWizard.PIASettings;
import de.mpc.pia.knime.nodes.PIAWizard.PIAViewModel;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.score.FDRData;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import de.mpc.pia.visualization.VisualizePSM;

public class WizardPanelPSM extends JPanel implements ActionListener {
	
	/** the panel for the wizard PSM settings */
	private JPanel psmSettingsPanel;
	
	/** the panel showing the PSM FDR info */
	private JPanel psmFDRPanel;
	
	/** the viewed PIA modeller */
	private PIAViewModel piaViewModel;
	
	
	private JCheckBox checkCreatePSMSets;
	private JFormattedTextField fdrThreshold;
	
	private JRadioButton decoyStrategy_pattern; 
	private JRadioButton decoyStrategy_searchengine;
	private ButtonGroup decoyStrategyGroup;
	private JTextField decoyPattern_pattern;
	private JRadioButton usedIdentifications_top;
	private JRadioButton usedIdentifications_all;
	private ButtonGroup usedIdentificationsGroup;
	
	private DefaultListModel<ScoreModel> availableScoresModel;
	private JList<ScoreModel> availableScoresList;
	private DefaultListModel<ScoreModel> preferredScoresModel;
	private JList<ScoreModel> preferredScoresList;
	private JButton addToPreferred_button;
	private JButton removeFromPreferred_button;
	
	private JButton calculatePSMFDR;
	
	
	
	
	public WizardPanelPSM(PIAViewModel piaViewModel) {
		this.piaViewModel = piaViewModel;
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		
		this.psmSettingsPanel = initializeSettingsPanel();
		psmSettingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
		c.gridx = 0;
		c.gridy = 0;
		this.add(psmSettingsPanel, c);
		
		this.psmFDRPanel = new JPanel(new GridBagLayout());
		psmFDRPanel.setBorder(BorderFactory.createTitledBorder("FDR"));
		c.gridx = 0;
		c.gridy = 1;
		updateFDRPanel();
		this.add(psmFDRPanel, c);
	}
	
	
	/**
	 * creates a settings map with the current settings 
	 * 
	 * @param key
	 * @return
	 */
	public Map<String, Object> getSettings() {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		
		settings.put(PIASettings.CREATE_PSMSETS.getKey(), checkCreatePSMSets.isSelected());
		settings.put(PIASettings.FDR_THRESHOLD.getKey(), (Double)fdrThreshold.getValue());
		
		settings.put(PIASettings.DECOY_STRATEGY.getKey(), decoyStrategyGroup.getSelection().getActionCommand());
		settings.put(PIASettings.DECOY_PATTERN.getKey(), decoyPattern_pattern.getText());
		settings.put(PIASettings.USED_IDENTIFICATIONS.getKey(), Integer.parseInt(usedIdentificationsGroup.getSelection().getActionCommand()));
		
		String[] preferredScoreShorts = new String[preferredScoresModel.getSize()];
		for (int i=0; i < preferredScoresModel.getSize(); i++) {
			preferredScoreShorts[i] = preferredScoresModel.getElementAt(i).getShortName();
		}
		settings.put(PIASettings.PREFERRED_SCORES.getKey(), preferredScoreShorts);
		
		return settings;
	}
	
	
	/**
	 * Sets the PIA View model, after loading the file.
	 * @param modeller
	 */
	public void setPIAViewModel(PIAViewModel model) {
		this.piaViewModel = model;
	}
	
	
	/**
	 * Disables all settings
	 */
	public void disableAllSettings() {
		checkCreatePSMSets.setEnabled(false);
		fdrThreshold.setEnabled(false);
		
		decoyStrategy_pattern.setEnabled(false);
		decoyStrategy_searchengine.setEnabled(false);
		
		decoyPattern_pattern.setEnabled(false);
		
		usedIdentifications_top.setEnabled(false);
		usedIdentifications_all.setEnabled(false);
		
		availableScoresList.setEnabled(false);
		preferredScoresList.setEnabled(false);
		addToPreferred_button.setEnabled(false);
		removeFromPreferred_button.setEnabled(false);
		
		calculatePSMFDR.setEnabled(false);
	}
	
	
	/**
	 * Enables the settings
	 */
	public void enableSettings() {
		checkCreatePSMSets.setEnabled(true);
		fdrThreshold.setEnabled(true);
		
		decoyStrategy_pattern.setEnabled(true);
		decoyStrategy_searchengine.setEnabled(true);
		
		decoyPattern_pattern.setEnabled(true);
		
		usedIdentifications_top.setEnabled(true);
		usedIdentifications_all.setEnabled(true);
		
		availableScoresList.setEnabled(true);
		preferredScoresList.setEnabled(true);
		addToPreferred_button.setEnabled(true);
		removeFromPreferred_button.setEnabled(true);
		
		calculatePSMFDR.setEnabled(piaViewModel != null);
	}
	
	
	/**
	 * Creates a panel, which contains the settings for the wizard on PSM level
	 * 
	 * @return
	 */
	private JPanel initializeSettingsPanel() {
		JPanel psmPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		
		// >> create PSM sets and general FDR threshold ---
		c.gridx = 0;
		c.gridy = 0;
		checkCreatePSMSets = new JCheckBox("Create PSM sets");
		checkCreatePSMSets.setSelected(true);
		psmPanel.add(checkCreatePSMSets, c);
		
		c.gridx = 0;
		c.gridy = 1;
		psmPanel.add(new JLabel("FDR threshold:"), c);
		
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(32);
		
		fdrThreshold = new JFormattedTextField(nf);
		fdrThreshold.setValue(0.01);
		c.gridx = 1;
		c.gridy = 1;
		psmPanel.add(fdrThreshold, c);
		// << create PSM sets and general FDR threshold ---
		
		// >> how to define decoys ------------------------
		c.gridx = 0;
		c.gridy = 2;
		psmPanel.add(new JLabel("How to define decoys:"), c);
		
		decoyStrategy_pattern = new JRadioButton("accession pattern");
		decoyStrategy_pattern.setActionCommand(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString());
		decoyStrategy_pattern.addActionListener(this);
		 
		decoyStrategy_searchengine = new JRadioButton("by searchengine");
		decoyStrategy_searchengine.setActionCommand(FDRData.DecoyStrategy.SEARCHENGINE.toString());
		decoyStrategy_searchengine.addActionListener(this);
		
		decoyStrategyGroup = new ButtonGroup();
		decoyStrategyGroup.add(decoyStrategy_pattern);
		decoyStrategyGroup.add(decoyStrategy_searchengine);
		
		c.insets = new Insets(0, 5, 0, 5);
		c.gridx = 1;
		c.gridy = 2;
		psmPanel.add(decoyStrategy_pattern, c);
		
		c.gridx = 1;
		c.gridy = 3;
		psmPanel.add(decoyStrategy_searchengine, c);
		c.insets = new Insets(5, 5, 5, 5);
		// << how to define decoys ------------------------
		
		// >> decoy pattern -------------------------------
		c.gridx = 0;
		c.gridy = 4;
		psmPanel.add(new JLabel("Decoy pattern:"), c);
		
		decoyPattern_pattern = new JTextField("rev_.*", 10);
		c.gridx = 1;
		c.gridy = 4;
		psmPanel.add(decoyPattern_pattern, c);
		// << decoy pattern -------------------------------
		
		// >> used identifications ------------------------
		c.gridx = 0;
		c.gridy = 5;
		psmPanel.add(new JLabel("Used identifications:"), c);
		
		usedIdentifications_top = new JRadioButton("only top identification");
		usedIdentifications_top.setActionCommand("1");
		usedIdentifications_top.setSelected(true);
		usedIdentifications_all = new JRadioButton("all identifications");
		usedIdentifications_all.setActionCommand("0");
		
		usedIdentificationsGroup = new ButtonGroup();
		usedIdentificationsGroup.add(usedIdentifications_top);
		usedIdentificationsGroup.add(usedIdentifications_all);
		
		c.insets = new Insets(0, 5, 0, 5);
		c.gridx = 1;
		c.gridy = 5;
		psmPanel.add(usedIdentifications_top, c);
		
		c.gridx = 1;
		c.gridy = 6;
		psmPanel.add(usedIdentifications_all, c);
		c.insets = new Insets(5, 5, 5, 5);
		// << used identifications ------------------------
		
		// >> Preferred score(s) --------------------------
		c.gridx = 0;
		c.gridy = 7;
		psmPanel.add(new JLabel("Preferred score(s):"), c);
		
		
		JPanel fdrScorePanel = new JPanel(new GridBagLayout());
		GridBagConstraints layoutFdrScorePanel = new GridBagConstraints();
		layoutFdrScorePanel.fill = GridBagConstraints.HORIZONTAL;
		
		layoutFdrScorePanel.gridx = 0;
		layoutFdrScorePanel.gridy = 0;
		fdrScorePanel.add(new JLabel("Available PSM scores"), layoutFdrScorePanel);
		
		availableScoresModel = new DefaultListModel<ScoreModel>();
		
		availableScoresList = new JList<ScoreModel>(availableScoresModel);
		availableScoresList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		availableScoresList.setLayoutOrientation(JList.VERTICAL);
		availableScoresList.setVisibleRowCount(-1);
		availableScoresList.setCellRenderer(new ScoreNameListCellRenderer());
		
		JScrollPane listScroller = new JScrollPane(availableScoresList);
		listScroller.setPreferredSize(new Dimension(200, 100));
		
		layoutFdrScorePanel.gridx = 0;
		layoutFdrScorePanel.gridy = 1;
		fdrScorePanel.add(listScroller, layoutFdrScorePanel);
		
		
		layoutFdrScorePanel.gridx = 2;
		layoutFdrScorePanel.gridy = 0;
		fdrScorePanel.add(new JLabel("Preferred PSM scores"), layoutFdrScorePanel);
		
		preferredScoresModel = new DefaultListModel<ScoreModel>();
		
		preferredScoresList = new JList<ScoreModel>(preferredScoresModel);
		preferredScoresList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		preferredScoresList.setLayoutOrientation(JList.VERTICAL);
		preferredScoresList.setVisibleRowCount(-1);
		preferredScoresList.setCellRenderer(new ScoreNameListCellRenderer());
		
		listScroller = new JScrollPane(preferredScoresList);
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
		
		
		c.gridx = 1;
		c.gridy = 7;
		psmPanel.add(fdrScorePanel, c);
		// << Preferred score(s) --------------------------
		
		// >> calculate the FDR ---------------------------
		calculatePSMFDR = new JButton("Calculate FDR");
		calculatePSMFDR.addActionListener(this);
		c.gridx = 0;
		c.gridy = 8;
		c.fill = GridBagConstraints.CENTER;
		c.gridwidth = 2;
		psmPanel.add(calculatePSMFDR, c);
		// >> calculate the FDR ---------------------------
		
		updateAvailableSettings();
	
		return psmPanel;
	}
	
	
	/**
	 * Updates the available settings according to the opened file. 
	 * This should be called e.g. after loading a PIA intermediate file
	 */
	public void applyLoadedSettings(NodeSettingsRO settings) {
		
		checkCreatePSMSets.setSelected(
				settings.getBoolean(PIASettings.CREATE_PSMSETS.getKey(), PIASettings.CREATE_PSMSETS.getDefaultBoolean()));
		fdrThreshold.setValue(
				settings.getDouble(PIASettings.FDR_THRESHOLD.getKey(), PIASettings.FDR_THRESHOLD.getDefaultDouble()));
		
		
		if (settings.getString(PIASettings.DECOY_STRATEGY.getKey(), PIASettings.DECOY_STRATEGY.getDefaultString()).
				equals(FDRData.DecoyStrategy.ACCESSIONPATTERN.toString())) {
			decoyStrategy_pattern.setSelected(true);
		} else {
			decoyStrategy_searchengine.setSelected(true);
		}
		
		decoyPattern_pattern.setText(
				settings.getString(PIASettings.DECOY_PATTERN.getKey(), PIASettings.DECOY_PATTERN.getDefaultString()));
		
		if (settings.getInt(PIASettings.USED_IDENTIFICATIONS.getKey(), PIASettings.USED_IDENTIFICATIONS.getDefaultInteger()) == 0) {
			usedIdentifications_all.setSelected(true);
		} else {
			usedIdentifications_top.setSelected(true);
		}
		
		
		preferredScoresModel.removeAllElements();
		for (String scoreShort
				: settings.getStringArray(PIASettings.PREFERRED_SCORES.getKey(), PIASettings.PREFERRED_SCORES.getDefaultStringArray())) {
			ScoreModelEnum modelType = ScoreModelEnum.getModelByDescription(scoreShort);
			ScoreModel model;
			
			if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
				model = new ScoreModel(0.0, scoreShort, scoreShort);
			} else {
				model = new ScoreModel(0.0, modelType);
			}
			
			preferredScoresModel.addElement(model);
		}
	}
	
	
	/**
	 * Updates the available settings according to the opened file. 
	 * This should be called e.g. after loading a PIA intermediate file
	 */
	public void updateAvailableSettings() {
		if (piaViewModel != null) {
			boolean allAllowStrategySearchengine = true;
			for (Long fileID : piaViewModel.getPSMModeller().getFiles().keySet()) {
				if (fileID > 0) {
					if (!piaViewModel.getPSMModeller().getFileHasInternalDecoy(fileID)) {
						allAllowStrategySearchengine = false;
						break;
					}
				}
			}
			
			if (!allAllowStrategySearchengine) {
				decoyStrategy_searchengine.setText("by searchengine (files without internal decoys)");
			}
			
			for (int i=0; i < preferredScoresModel.size(); i++) {
				String scoreShort = preferredScoresModel.getElementAt(i).getShortName();
				
				ScoreModelEnum modelType = ScoreModelEnum.getModelByDescription(scoreShort);
				ScoreModel model;
				
				if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
					model = new ScoreModel(0.0, scoreShort, piaViewModel.getPSMModeller().getScoreName(scoreShort));
				} else {
					model = new ScoreModel(0.0, modelType);
				}
				
				preferredScoresModel.remove(i);
				preferredScoresModel.add(i, model);
			}
			
			availableScoresModel.removeAllElements();
			for (Long fileID : piaViewModel.getPSMModeller().getFiles().keySet()) {
				for (String scoreShort : piaViewModel.getPSMModeller().getFilesAvailableScoreShortsForFDR(fileID)) {
					ScoreModelEnum modelType = ScoreModelEnum.getModelByDescription(scoreShort);
					ScoreModel model;
					
					if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
						model = new ScoreModel(0.0, scoreShort, piaViewModel.getPSMModeller().getScoreName(scoreShort));
					} else {
						model = new ScoreModel(0.0, modelType);
					}
					
					if (!ScoreModelEnum.notForPSMFdrScore.contains(model)) {
						if (!availableScoresModel.contains(model) && !preferredScoresModel.contains(model)) {
							availableScoresModel.addElement(model);
						}
					}
				}
			}
			
			calculatePSMFDR.setEnabled(true);
		} else {
			// the default settings
			checkCreatePSMSets.setSelected(true);
			
			decoyStrategy_pattern.setSelected(true);
			
			for (ScoreModelEnum model : ScoreModelEnum.values()) {
				if (!ScoreModelEnum.notForPSMFdrScore.contains(model) && !model.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
					availableScoresModel.addElement(
							new ScoreModel(0.0, model.getShortName(), model.getName()));
				}
			}
			
			calculatePSMFDR.setEnabled(false);
		}
	}
	
	
	/**
	 * Sets recommended settings, based on the loaded PIA interemdiate data.
	 */
	public void applyRecommendedSettings() {
		if (piaViewModel != null) {
			checkCreatePSMSets.setSelected(piaViewModel.getPSMModeller().getFiles().size() > 2);
			
			boolean allAllowStrategySearchengine = true;
			for (Long fileID : piaViewModel.getPSMModeller().getFiles().keySet()) {
				if (!piaViewModel.getPSMModeller().getFileHasInternalDecoy(fileID)) {
					allAllowStrategySearchengine = false;
					break;
				}
			}
			
			if (allAllowStrategySearchengine) {
				decoyStrategy_searchengine.setSelected(true);
			} else {
				decoyStrategy_searchengine.setText("by searchengine (files without internal decoys)");
				decoyStrategy_searchengine.setEnabled(false);
				decoyStrategy_pattern.setSelected(true);
			}
			
			preferredScoresModel.removeAllElements();
			availableScoresModel.removeAllElements();
			for (Long fileID : piaViewModel.getPSMModeller().getFiles().keySet()) {
				for (String scoreShort : piaViewModel.getPSMModeller().getFilesAvailableScoreShortsForFDR(fileID)) {
					
					ScoreModelEnum modelType =
							ScoreModelEnum.getModelByDescription(scoreShort);
					ScoreModel model;
					
					if (modelType.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
						model = new ScoreModel(0.0, scoreShort, piaViewModel.getPSMModeller().getScoreName(scoreShort));
					} else {
						model = new ScoreModel(0.0, modelType);
					}
					
					if (!ScoreModelEnum.notForPSMFdrScore.contains(model)) {
						if (modelType.isSearchengineMainScore()) {
							if (!preferredScoresModel.contains(model)) {
								preferredScoresModel.addElement(model);
							}
						} else {
							if (!availableScoresModel.contains(model)) {
								availableScoresModel.addElement(model);
							}
						}
					}
				}
			}
			
			calculatePSMFDR.setEnabled(true);
		} else {
			// the default settings
			checkCreatePSMSets.setSelected(true);
			
			decoyStrategy_pattern.setSelected(true);
			
			for (ScoreModelEnum model : ScoreModelEnum.values()) {
				if (!ScoreModelEnum.notForPSMFdrScore.contains(model) && !model.equals(ScoreModelEnum.UNKNOWN_SCORE)) {
					availableScoresModel.addElement(
							new ScoreModel(0.0, model.getShortName(), model.getName()));
				}
			}
			
			calculatePSMFDR.setEnabled(false);
		}
	}
	
	
	/**
	 * Paints the FDR information on the panel, if it is calculated 
	 */
	public void updateFDRPanel() {
		if (piaViewModel == null) {
			psmFDRPanel.removeAll();
			psmFDRPanel.add(new JLabel("No file loaded yet."));
		} else {
			psmFDRPanel.removeAll();
			
			GridBagConstraints cFDRPanel = new GridBagConstraints();
			cFDRPanel.fill = GridBagConstraints.HORIZONTAL;
			cFDRPanel.insets = new Insets(5, 5, 5, 5);
			
			JPanel overviewPanel = new JPanel(new GridBagLayout());
			overviewPanel.setBorder(BorderFactory.createTitledBorder("FDR for PSM sets / all files"));
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(5, 5, 5, 5);
			
			// >> the information data --------------------
			JPanel fdrDataPanel = new JPanel(new GridBagLayout());
			
			if (piaViewModel.getPSMModeller().isCombinedFDRScoreCalculated()) {
				c.insets = new Insets(0, 5, 5, 5);
				c.gridx = 0;
				c.gridy = 0;
				fdrDataPanel.add(new JLabel("#PSM sets:"), c);
				c.gridx = 1;
				c.gridy = 0;
				fdrDataPanel.add(new JLabel("" + piaViewModel.getPSMModeller().getNrReportPSMs(0L)), c);
				
				
				FDRData fdrData = piaViewModel.getPSMModeller().getFilesFDRData(0L);
				
				c.gridx = 0;
				c.gridy = 1;
				fdrDataPanel.add(new JLabel("#PSM sets with FDR:"), c);
				c.gridx = 1;
				c.gridy = 1;
				fdrDataPanel.add(new JLabel("" + fdrData.getNrItems()), c);
				
				c.gridx = 0;
				c.gridy = 2;
				fdrDataPanel.add(new JLabel("#targets:"), c);
				c.gridx = 1;
				c.gridy = 2;
				fdrDataPanel.add(new JLabel("" + fdrData.getNrTargets()), c);
				
				c.gridx = 0;
				c.gridy = 3;
				fdrDataPanel.add(new JLabel("#decoys:"), c);
				c.gridx = 1;
				c.gridy = 3;
				fdrDataPanel.add(new JLabel("" + fdrData.getNrDecoys()), c);
				
				c.gridx = 0;
				c.gridy = 4;
				fdrDataPanel.add(new JLabel("#targets below threshold:"), c);
				c.gridx = 1;
				c.gridy = 4;
				fdrDataPanel.add(new JLabel("" + fdrData.getNrFDRGoodTargets()), c);
				
				c.gridx = 0;
				c.gridy = 5;
				fdrDataPanel.add(new JLabel("#decoys below threshold:"), c);
				c.gridx = 1;
				c.gridy = 5;
				fdrDataPanel.add(new JLabel("" + fdrData.getNrFDRGoodDecoys()), c);
				
				c.insets = new Insets(5, 5, 5, 5);
			} else {
				fdrDataPanel.add(new JLabel("<html>Combined FDR score<br/>was not calculated.</html>"), c);
			}
			
			c.gridx = 0;
			c.gridy = 0;
			overviewPanel.add(fdrDataPanel, c);
			// >> the information data --------------------
			
			// >> draw PPM deviation ----------------------
			VisualizePSM vPSM = new VisualizePSM(piaViewModel.getPSMModeller());
			
			HistogramDataset hd = vPSM.getPPMHistogramData(0l, piaViewModel.getPSMModeller().isCombinedFDRScoreCalculated());
			
			JFreeChart chart = ChartFactory.createHistogram("mass shift (PPM)",
					null,
					"nrPSMs",
					hd,
					PlotOrientation.VERTICAL,
					false,
					false,
					false);
			
			chart.getTitle().setFont(new Font("Dialog", Font.BOLD, 14));
			
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
			plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

	        plot.getDomainAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 10));
	        plot.getRangeAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 10));
	        
	        plot.getRangeAxis().setLabelFont(new Font("Dialog", Font.BOLD, 12));
	        
			XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
			renderer.setBarPainter(new StandardXYBarPainter());
			
			renderer.setDrawBarOutline(false);
			renderer.setShadowVisible(false);
			
			renderer.setSeriesPaint(0, new Color(47, 105, 191));
			
			JPanel ppmPanel = new ChartPanel(chart,
					240, 140,
					240, 140,
					800, 600,
					true, true, false, false, true, true);
			ppmPanel.setMinimumSize(new Dimension(240, 140));
			
			c.insets = new Insets(0, 0, 0, 0);
			c.gridx = 1;
			c.gridy = 0;
			overviewPanel.add(ppmPanel, c);
			// << draw PPM deviation ----------------------
			
			cFDRPanel.gridx = 0;
			cFDRPanel.gridy = 0;
			psmFDRPanel.add(overviewPanel, cFDRPanel);
		}
		
		psmFDRPanel.validate();
		psmFDRPanel.repaint();
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(decoyStrategy_pattern) || e.getSource().equals(decoyStrategy_searchengine)) {
			// decoy pattern can only be edited, if decoy strategy is pattern 
			decoyPattern_pattern.setEnabled(decoyStrategy_pattern.isSelected());
		} else if (e.getSource().equals(addToPreferred_button) && (availableScoresList.getSelectedIndex() > -1)) {
			preferredScoresModel.addElement(availableScoresList.getSelectedValue());
			availableScoresModel.remove(availableScoresList.getSelectedIndex());
		} else if (e.getSource().equals(removeFromPreferred_button) && (preferredScoresList.getSelectedIndex() > -1)) {
			availableScoresModel.addElement(preferredScoresList.getSelectedValue());
			preferredScoresModel.remove(preferredScoresList.getSelectedIndex());
		} else if (e.getSource().equals(calculatePSMFDR)) {
			// execute PSM level operations
			if (piaViewModel != null) {
				for (Map.Entry<String, Object> setIt : getSettings().entrySet()) {
					piaViewModel.addSetting(setIt.getKey(), setIt.getValue());
				}
				piaViewModel.executePSMOperations();
			}
			
			updateFDRPanel();
		}
	}
	
	
	/**
	 * Renders the cell with the ScoreModel name
	 * @author julian
	 *
	 */
	private class ScoreNameListCellRenderer extends JLabel implements ListCellRenderer<ScoreModel> {
		
		public ScoreNameListCellRenderer() {
	        setOpaque(true);
		}
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends ScoreModel> list, ScoreModel scoreModel,
				int index, boolean isSelected, boolean cellHasFocus) {
			
			if (isSelected) {
	            setBackground(list.getSelectionBackground());
	            setForeground(list.getSelectionForeground());
	        } else {
	            setBackground(list.getBackground());
	            setForeground(list.getForeground());
	        }
			
			setText(scoreModel.getName());
			
			return this;
		}
	}
}
