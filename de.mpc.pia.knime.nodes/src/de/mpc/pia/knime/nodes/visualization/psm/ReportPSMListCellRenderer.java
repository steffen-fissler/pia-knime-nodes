package de.mpc.pia.knime.nodes.visualization.psm;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;

public class ReportPSMListCellRenderer extends JPanel implements ListCellRenderer<ReportPSM> {
	
	
	public ReportPSMListCellRenderer() {
		super();
		setOpaque(true);
		/*
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
		*/
	}
	
	
	@Override
	public Component getListCellRendererComponent(
			JList<? extends ReportPSM> list, ReportPSM psm, int index,
			boolean isSelected, boolean cellHasFocus) {
		this.removeAll();
		
		this.setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), psm.getFileName()));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		
		// TODO: besser eine textarea einfuegen
		
		JLabel label;
		int row = 0;
		for (ScoreModel scoreModel : psm.getScores()) {
			label = new JLabel(scoreModel.getName());
			c.gridx = 0;
			c.gridy = row;
			this.add(label, c);
			
			label = new JLabel(scoreModel.getValue().toString());
			c.gridx = 1;
			c.gridy = row;
			this.add(label, c);
			
			row++;
		}
		
		if (psm.getFDRScore() != null) {
			label = new JLabel(ScoreModelEnum.PSM_LEVEL_Q_VALUE.getName());
			c.gridx = 0;
			c.gridy = row;
			this.add(label, c);
			
			label = new JLabel(Double.toString(psm.getQValue()));
			c.gridx = 1;
			c.gridy = row;
			this.add(label, c);
			
			row++;
		}
		
		return this;
	}
}