package de.mpc.pia.knime.nodes.visualization.psms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSMSet;


public class PSMSetTableModel extends AbstractTableModel {

    /** the shown data */
    private List<ReportPSMSet> psmSetList;

    /** fast access to the values of {@link ColumnNames} */
    public final static ColumnNames[] colNameValues = ColumnNames.values();


    public PSMSetTableModel(List<PSMReportItem> psmSetList) {
        super();

        updatePSMSetList(psmSetList);
    }


    /**
     * Updates the shown information.
     *
     * @param peptideList
     * @param considerModifications
     * @param hasPSMsets
     * @param scoreNameMap
     */
    public void updatePSMSetList(List<PSMReportItem> psmSetList) {
        if (psmSetList == null) {
            this.psmSetList = Collections.emptyList();
        } else {
            this.psmSetList = new ArrayList<>(psmSetList.size());
            for (PSMReportItem psm : psmSetList) {
                if (psm instanceof ReportPSMSet) {
                    this.psmSetList.add((ReportPSMSet)psm);
                }
            }
        }

        fireTableDataChanged();
    }


    /**
     * Getter for the peptide at the given row
     * @param index
     * @return
     */
    public ReportPSMSet getPSMSetAt(int index) {
        if ((index < 0) || (index >= getRowCount())) {
            return null;
        }
        return psmSetList.get(index);
    }

    @Override
    public int getColumnCount() {
        return colNameValues.length;
    }

    @Override
    public int getRowCount() {
        return psmSetList.size();
    }

    @Override
    public String getColumnName(int col) {
        return colNameValues[col].getColName();
    }

    @Override
    public Object getValueAt(int row, int col) {
        ReportPSMSet psmSet = psmSetList.get(row);
        ColumnNames colEnum = colNameValues[col];

        switch (colEnum) {
        case CHARGE:
            return psmSet.getCharge();
        case DELTA_MASS:
            return psmSet.getDeltaMass();
        case DELTA_PPM:
            return psmSet.getDeltaPPM();
        case IDENTIFICATIONS:
            return psmSet.getPSMs().size();
        case MASS_TO_CHARGE:
            return psmSet.getMassToCharge();
        case MISSED_CLEAVAGES:
            return psmSet.getMissedCleavages();
        case MODIFICATIONS:
            return psmSet.getModificationsString();
        case RETENTIONTIME:
            return psmSet.getRetentionTime();
        case SOURCE_ID:
            return psmSet.getSourceID();
        case SPECTRUM_TITLE:
            return psmSet.getSpectrumTitle();
        }

        return null;
    }

    @Override
    public Class getColumnClass(int col) {
        ColumnNames colEnum = colNameValues[col];

        switch (colEnum) {
        case MODIFICATIONS:
        case SOURCE_ID:
        case SPECTRUM_TITLE:
            return String.class;
        case IDENTIFICATIONS:
        case CHARGE:
        case MISSED_CLEAVAGES:
            return Integer.class;
        case DELTA_MASS:
        case DELTA_PPM:
        case MASS_TO_CHARGE:
        case RETENTIONTIME:
            return Double.class;
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

        MODIFICATIONS("Modifications"),
        IDENTIFICATIONS("nrIdentifications"),
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
