package de.mpc.pia.knime.nodes.visualization;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableRowSorter;

import de.mpc.pia.knime.nodes.visualization.psm.PSMSetTable;
import de.mpc.pia.knime.nodes.visualization.psm.PSMSetTableListener;
import de.mpc.pia.knime.nodes.visualization.psm.PSMSetTableModel;
import de.mpc.pia.knime.nodes.visualization.psm.ReportPSMListCellRenderer;
import de.mpc.pia.modeller.PIAModeller;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.PSMReportItemComparator;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;


public class VisualizationPanel extends JPanel {
	
	private PIAModeller piaModeller;
	
	
	public VisualizationPanel(PIAModeller piaModeller) {
		this.piaModeller = piaModeller;
		
		drawPanel();
	}
	
	
	private void drawPanel() {
		if (piaModeller == null) {
			this.add(new JLabel("No model created."));
			this.setPreferredSize(new Dimension(400, 300));
			return;
		}
		
		this.removeAll();
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
		
		// >> the PSM Tab -----------------------------------------------------
		List<ReportPSMSet> reportPSMSet = piaModeller.getPSMModeller().getFilteredReportPSMSets(
				piaModeller.getPSMModeller().getFilters(0L));
		PSMSetTableModel psmSetTableModel = new PSMSetTableModel(reportPSMSet,
				piaModeller.getPSMModeller().isCombinedFDRScoreCalculated());
		
		
		JList<ReportPSM> psmInformationList = new JList<ReportPSM>();
		psmInformationList.setModel(new DefaultListModel<ReportPSM>());
		psmInformationList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		psmInformationList.setCellRenderer(new ReportPSMListCellRenderer());
		JScrollPane psmInformationScrollPane = new JScrollPane(psmInformationList);
		psmInformationScrollPane.setPreferredSize(new Dimension(800, 200));
		
		
		PSMSetTable psmSetTable = new PSMSetTable(psmSetTableModel);
		psmSetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		psmSetTable.getSelectionModel().addListSelectionListener(new PSMSetTableListener(psmSetTable, psmInformationList));
		JScrollPane listScroller = new JScrollPane(psmSetTable);
		
		// set some special sort methods
		TableRowSorter<PSMSetTableModel> psmSorter = new TableRowSorter<PSMSetTableModel>(psmSetTableModel);
		psmSetTable.setRowSorter(psmSorter);
		
		psmSorter.setComparator(PSMSetTableModel.ColumnNames.SEQUENCE.ordinal(), PSMReportItemComparator.SEQUENCE_SORT);
		psmSorter.setComparator(PSMSetTableModel.ColumnNames.DELTAMASS.ordinal(), PSMReportItemComparator.DELTA_PPM_SORT);
		
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				listScroller, psmInformationScrollPane);
		splitPane.setDividerLocation(0.7);
		splitPane.setResizeWeight(0.9);
		
		tabPane.addTab("PSM Sets", splitPane);
		// << the PSM Tab -----------------------------------------------------
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		this.add(tabPane, c);
	}
	
	
	public void updateView(PIAModeller piaModeller) {
		this.piaModeller = piaModeller;
		
		drawPanel();
		
		validate();
		repaint();
	}
}