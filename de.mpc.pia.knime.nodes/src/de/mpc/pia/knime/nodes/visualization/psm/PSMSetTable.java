package de.mpc.pia.knime.nodes.visualization.psm;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Modification;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSMSet;


/**
 * This table handles a table showing {@link ReportPSMSet}s.
 * 
 * @author julian
 *
 */
public class PSMSetTable extends JTable {
	
	private SequenceRenderer sequenceRenderer;
	private AccessionsRenderer accessionsRenderer;
	private ScoreRenderer scoreRenderer;
	private DeltaMassRenderer deltaMassRenderer;
	private FourDigitsRenderer fourDigitsRenderer;
	private DefaultTableCellRenderer rightRenderer;
	
	
	/** the number of digits for the displayed scores */
	private final static int numberDigitsScores = 4;
	
	/** decimal format converter for very small scores */
	private final static DecimalFormat scoreDfVerySmall = new DecimalFormat("0.###E0");
	
	/** decimal format converter for normal scores */
	private final static DecimalFormat scoreDfNormal = new DecimalFormat("0.######");
	
	
	/** decimal format for exactly four digits */
	private final static DecimalFormat fourDigitsDf = new DecimalFormat("0.0000");
	
	/** decimal format for exactly two digits */
	private final static DecimalFormat twoDigitsDf = new DecimalFormat("0.00");
	
	
	
	public PSMSetTable(PSMSetTableModel tableModel) {
		super(tableModel);
		
		sequenceRenderer = new SequenceRenderer();
		
		accessionsRenderer = new AccessionsRenderer();
		
		scoreRenderer = new ScoreRenderer();
		
		deltaMassRenderer = new DeltaMassRenderer();
		
		fourDigitsRenderer = new FourDigitsRenderer();
		
		rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
	}
	
	
	public TableCellRenderer getCellRenderer(int rowIndex, int columnIndex) {
		PSMSetTableModel.ColumnNames columnName = PSMSetTableModel.columnNameValues[columnIndex];
		
		switch (columnName) {
		case ACCESSIONS:
			return accessionsRenderer;
			
		case IDENTIFICATIONS:
		case CHARGE:
		case MISSED:
			return rightRenderer;
			
		case MZ:
		case RETENTIONTIME:
			return fourDigitsRenderer;
			
		case DELTAMASS:
			return deltaMassRenderer;
			
		case COMBINEDFDRSCORE:
			return scoreRenderer;
			
		case SEQUENCE:
			return sequenceRenderer;
		}
		
		// return default renderer
		return super.getCellRenderer(rowIndex, columnIndex);
    }
	
	
	@Override
	public PSMSetTableModel getModel() {
		return (PSMSetTableModel) super.getModel();
	}
	
	
	/**
	 * The renderer of the sequence. Gets a complete PSM to get the modifications. 
	 * @author julian
	 *
	 */
	public class SequenceRenderer extends DefaultTableCellRenderer {
		public SequenceRenderer() {
			super();
		}
		
		public void setValue(Object data) {
			String sequence = null;
			Map<Integer, Modification> mods = null;
			if (data instanceof PSMReportItem) {
				sequence = ((PSMReportItem) data).getSequence();
				mods = ((PSMReportItem) data).getModifications();
			}
			
			if ((sequence == null) || (mods == null)) {
				return;
			}
			
			StringBuilder textSB = new StringBuilder("<html>");
			StringBuilder tooltipSB = new StringBuilder("<html>");
			
			int lastPos = 0;
			for (Map.Entry<Integer, Modification> modIt : mods.entrySet()) {
				int pos = modIt.getKey();
				
				// first add the unmodified residues from last to here
				if (pos - lastPos > 1) {
					textSB.append(sequence, lastPos, pos-1);
				}
				
				String residue;
				if ((pos == 0) || (pos == sequence.length()+1)) {
					// special case: terminal modification
					residue = ".";
				} else {
					residue = sequence.substring(pos-1, pos);
				}
				
				textSB.append("<span style=\"color: orange\";>");
				textSB.append(residue);
				textSB.append("</span>");
				
				
				if (tooltipSB.length() > 6) {
					tooltipSB.append("<br/>");
				}
				tooltipSB.append(pos);
				tooltipSB.append(": ");
				if (modIt.getValue().getDescription() != null) {
					tooltipSB.append(modIt.getValue().getMass()+": "+modIt.getValue().getDescription());
				} else {
					tooltipSB.append(Double.toString(modIt.getValue().getMass()));
				}
				
				lastPos = pos;
			}
			
			if (lastPos < sequence.length()) {
				textSB.append(sequence, lastPos, sequence.length());
			}
			
			textSB.append("</html>");
			tooltipSB.append("</html>");
			
			setText(textSB.toString());
			
			if (tooltipSB.length() > 13) {
				setToolTipText(tooltipSB.toString());
			}
		}
	}
	
	
	public class AccessionsRenderer extends JPanel implements TableCellRenderer {
		
		public AccessionsRenderer() {
			super();
		}
		
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object data,
				boolean isSelected, boolean hasFocus,
                int row, int column) {
			
			this.removeAll();
			
			this.setOpaque(false);
			
			List<Accession> accessions = null;
			if (data instanceof PSMReportItem) {
				accessions = ((PSMReportItem) data).getAccessions();
			}
			
			if (accessions == null) {
				this.add(new JLabel("ERROR: no accession!"));
				return this;
			}
			
			for (Accession acc : accessions) {
				this.add(new JLabel(acc.getAccession()));
			}
			
			return this;
		}
	}
	
	
	/**
	 * The renderer for a score value. Gets the value of the score as Number 
	 * @author julian
	 *
	 */
	public class ScoreRenderer extends DefaultTableCellRenderer {
		public ScoreRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
		
		public void setValue(Object data) {
			Double score = null;
			if (data instanceof Number) {
				score = ((Number) data).doubleValue();
			}
			
			if ((score != null) && !score.equals(Double.NaN)) {
				if (!score.equals(0.0) &&
						(Math.abs(score) < Math.pow(10.0, -numberDigitsScores))) {
					setText(scoreDfVerySmall.format(score));
				} else {
					setText(scoreDfNormal.format(score));
				}
			} else {
				if (score != null) {
					setText(score.toString());
				} else {
					setText("null");
				}
			}
		}
	}
	
	
	/**
	 * The renderer for the delta mass (m/z and ppm). Gets the complete PSM 
	 * @author julian
	 *
	 */
	public class DeltaMassRenderer extends DefaultTableCellRenderer {
		public DeltaMassRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
		
		public void setValue(Object data) {
			if (data instanceof PSMReportItem) {
				PSMReportItem psm = (PSMReportItem)data;
				setText(fourDigitsDf.format(psm.getDeltaMass()) + " (" + twoDigitsDf.format(psm.getDeltaPPM()) + ")");
			}
		}
	}
	
	
	/**
	 * This renderer renders a given Double with four digits on the right side. Gets a Number value. 
	 * @author julian
	 *
	 */
	public class FourDigitsRenderer extends DefaultTableCellRenderer {
		public FourDigitsRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
		
		public void setValue(Object data) {
			if (data instanceof Number) {
				setText(fourDigitsDf.format(data));
			}
		}
	}
}
