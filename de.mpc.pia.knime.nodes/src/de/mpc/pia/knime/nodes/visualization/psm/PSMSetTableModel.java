package de.mpc.pia.knime.nodes.visualization.psm;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.score.ScoreModelEnum;


/**
 * The table model to render {@link ReportPSMSet}s
 * 
 * @author julian
 *
 */
public class PSMSetTableModel extends AbstractTableModel {
	
	/** the PSM sets to show*/
	private ArrayList<ReportPSMSet> psmList;
	
	/** whether to show the combined PSM scores or not */
	private boolean showCombinedFDRScore;
	
	
	public enum ColumnNames {
		SEQUENCE("Sequence"),
		ACCESSIONS("Accessions"),
		NRACCESSIONS("#Accessions"),
		DECOY("Decoy"),
		IDENTIFICATIONS("#Identifications"),
		CHARGE("Charge"),
		MZ("m/z"),
		DELTAMASS("dMass (ppm)"),
		RETENTIONTIME("RT"),
		MISSED("Missed cleavages"),
		SOURCEID("Source ID"),
		SPECTRUMTITLE("Spectrum Title"),
		COMBINEDFDRSCORE(ScoreModelEnum.PSM_LEVEL_COMBINED_FDR_SCORE.getName()),
		;
		
		private String fullName;
		
		private ColumnNames(String name) {
			this.fullName = name;
		}
		
		public String getName() {
			return fullName;
		}
	}
	
	
	protected final static ColumnNames[] columnNameValues = ColumnNames.values();
	
	
	public PSMSetTableModel(List<ReportPSMSet> psms, boolean showCombinedFDRScore) {
		if (psms == null) {
			this.psmList = new ArrayList<ReportPSMSet>(0);
		} else {
			this.psmList = new ArrayList<ReportPSMSet>(psms);
		}
		
		this.showCombinedFDRScore = showCombinedFDRScore;
	}
	
	
	/**
	 * Returns the PSM element in the given row
	 * @param row
	 * @return
	 */
	public ReportPSMSet getElementInRow(int row) {
		return psmList.get(row);
	}
	
	
	@Override
    public Class getColumnClass(int columnIndex) {
		ColumnNames columnName = columnNameValues[columnIndex];
		
        switch (columnName) {
		case SEQUENCE:
		case ACCESSIONS:
		case DELTAMASS:
			return PSMReportItem.class;
			
		case NRACCESSIONS:
		case CHARGE:
		case IDENTIFICATIONS:
		case MISSED:
			return Integer.class;
			
		case DECOY:
			return Boolean.class;
			
		case SOURCEID:
		case SPECTRUMTITLE:
			return String.class;
			
		case COMBINEDFDRSCORE:
		case MZ:
		case RETENTIONTIME:
			return Double.class;
			
		default:
			return Object.class;
        }
    }
	
	@Override
	public int getColumnCount() {
		return showCombinedFDRScore ? columnNameValues.length : columnNameValues.length - 1;
	}
	
	@Override
	public String getColumnName(int col) {
        return columnNameValues[col].getName();
    }
	
	
	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return psmList.size();
	}
	
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		ReportPSMSet psm = psmList.get(rowIndex);
		
		ColumnNames columnName = columnNameValues[columnIndex];
		
		switch (columnName) {
		case SEQUENCE:
			return psm;
			
		case NRACCESSIONS:
			return psm.getAccessions().size();
			
		case ACCESSIONS:
			return psm;
			
		case DECOY:
			return psm.getIsDecoy();
			
		case IDENTIFICATIONS:
			return psm.getPSMs().size();
			
		case CHARGE:
			return psm.getCharge();
			
		case MZ:
			return psm.getMassToCharge();
			
		case DELTAMASS:
			return psm;
			
		case RETENTIONTIME:
			return psm.getRetentionTime();
			
		case MISSED:
			return psm.getMissedCleavages();
			
		case SOURCEID:
			return psm.getSourceID();
			
		case SPECTRUMTITLE:
			return psm.getSpectrumTitle();
			
		case COMBINEDFDRSCORE:
			return psm.getFDRScore().getValue();
		}
		
		return null;
	}

}
