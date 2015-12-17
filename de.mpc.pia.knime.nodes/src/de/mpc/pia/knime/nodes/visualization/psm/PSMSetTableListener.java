package de.mpc.pia.knime.nodes.visualization.psm;

import java.awt.TextArea;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;

public class PSMSetTableListener implements ListSelectionListener {
	
	private PSMSetTable table;
	
	private JList<ReportPSM> psmList;
	
	
	public PSMSetTableListener(PSMSetTable table, JList<ReportPSM> psmList) {
		this.table = table;
		this.psmList = psmList;
	}
	
	
	@Override
	public void valueChanged(ListSelectionEvent event) {
		
		((DefaultListModel<ReportPSM>)psmList.getModel()).removeAllElements();
		
		System.err.println("EVENT " + event);
		
		
		if (event.getValueIsAdjusting()) {
		    return;
		}
		
		StringBuffer output = new StringBuffer();
		
		output.append(String.format("Lead: %d, %d. ",
				table.getSelectionModel().getLeadSelectionIndex(),
				table.getColumnModel().getSelectionModel().
				getLeadSelectionIndex()));
		
		output.append("Rows:");
		for (int c : table.getSelectedRows()) {
			output.append(String.format(" %d", c));
		}
		
		output.append(". Columns:");
		for (int c : table.getSelectedColumns()) {
			output.append(String.format(" %d", c));
		}
		
		output.append(".\n");
		output.append("ROW SELECTION EVENT. ");
		
		
		
		int viewRow = table.getSelectedRow();
		if (viewRow < 0) {
			//Selection got filtered away.
			output.append("nix selected");
		} else {
			int modelRow = 
					table.convertRowIndexToModel(viewRow);
			
			output.append("\n");
			output.append(table.getModel().getElementInRow(modelRow).getSequence());
			
			for (ReportPSM psm : table.getModel().getElementInRow(modelRow).getPSMs()) {
				((DefaultListModel<ReportPSM>)psmList.getModel()).addElement(psm);
			}
		}
		
		psmList.revalidate();
	}

}
