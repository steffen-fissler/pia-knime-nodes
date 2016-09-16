package de.mpc.pia.knime.nodes.visualization.psmspectrumviewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.mpc.pia.modeller.psm.ReportPSM;

/**
 * Table model to show ReportPSMs
 *
 * @author julian
 *
 */
public class PSMTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 8577547471100937260L;

    /** the shown data */
    private List<ReportPSM> psmList;

    /** fast access to the values of {@link ColumnNames} */
    public final static ColumnNames[] colNameValues = ColumnNames.values();


    public PSMTableModel(List<ReportPSM> psmSetList) {
        super();

        updatePSMSetList(psmSetList);
    }


    /**
     * Updates the shown information.
     */
    public void updatePSMSetList(List<ReportPSM> psmSetList) {
        if (psmSetList == null) {
            this.psmList = Collections.emptyList();
        } else {
            this.psmList = new ArrayList<>(psmSetList.size());
            this.psmList.addAll(psmSetList);
        }

        fireTableDataChanged();
    }


    /**
     * Getter for the PSM at the given row
     * @param index
     * @return
     */
    public ReportPSM getPSMAt(int index) {
        if ((index < 0) || (index >= getRowCount())) {
            return null;
        }
        return psmList.get(index);
    }


    @Override
    public int getColumnCount() {
        return colNameValues.length;
    }


    @Override
    public int getRowCount() {
        return psmList.size();
    }


    @Override
    public String getColumnName(int col) {
        return colNameValues[col].getColName();
    }


    @Override
    public Object getValueAt(int row, int col) {
        ReportPSM psm = psmList.get(row);
        ColumnNames colEnum = colNameValues[col];

        switch (colEnum) {
        case ACCESSIONS:
            return psm.getAccessions();
        case CHARGE:
            return psm.getCharge();
        case DELTA_MASS:
            return psm.getDeltaMass();
        case DELTA_PPM:
            return psm.getDeltaPPM();
        case MASS_TO_CHARGE:
            return psm.getMassToCharge();
        case MISSED_CLEAVAGES:
            return psm.getMissedCleavages();
        case MODIFICATIONS:
            return psm.getModificationsString();
        case RETENTIONTIME:
            return psm.getRetentionTime();
        case SEQUENCE:
            return psm.getSequence();
        case SOURCE_ID:
            return psm.getSourceID();
        case SPECTRUM_TITLE:
            return psm.getSpectrumTitle();
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int col) {
        ColumnNames colEnum = colNameValues[col];

        switch (colEnum) {
        case ACCESSIONS:
            return List.class;
        case SEQUENCE:
        case MODIFICATIONS:
        case SOURCE_ID:
        case SPECTRUM_TITLE:
            return String.class;
        case CHARGE:
        case MISSED_CLEAVAGES:
            return Integer.class;
        case DELTA_MASS:
        case DELTA_PPM:
        case MASS_TO_CHARGE:
        case RETENTIONTIME:
            return Double.class;
        default:
            break;
        }

        return String.class;
    }



    /**
     * The actual columns
     *
     * @author julian
     *
     */
    private enum ColumnNames {

        SEQUENCE("Sequence"),
        MODIFICATIONS("Modifications"),
        ACCESSIONS("Accessions"),
        CHARGE("Charge"),
        MASS_TO_CHARGE("m/z"),
        DELTA_MASS("deltaMass"),
        DELTA_PPM("deltaPPM"),
        RETENTIONTIME("RT"),
        MISSED_CLEAVAGES("Missed Cleavages"),
        SOURCE_ID("Source ID"),
        SPECTRUM_TITLE("Spectrum Title"),
        ;

        private String colName;

        private ColumnNames(String colName) {
            this.colName = colName;
        }

        public String getColName() {
            return colName;
        }
    }
}
