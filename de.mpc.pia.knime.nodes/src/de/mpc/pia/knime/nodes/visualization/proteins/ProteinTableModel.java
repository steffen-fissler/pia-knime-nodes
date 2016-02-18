package de.mpc.pia.knime.nodes.visualization.proteins;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.mpc.pia.modeller.protein.ReportProtein;


public class ProteinTableModel extends AbstractTableModel {

    /** the shown data */
    private List<ReportProtein> proteinList;

    /** fast access to the values of {@link ColumnNames} */
    private final static ColumnNames[] colNameValues = ColumnNames.values();


    public ProteinTableModel(List<ReportProtein> proteinList) {
        super();

        this.proteinList = proteinList;
    }

    /**
     * Getter for the protein at the given row
     * @param index
     * @return
     */
    public ReportProtein getProteinAt(int index) {
        if ((index < 0) || (index >= getRowCount())) {
            return null;
        }
        return proteinList.get(index);
    }

    @Override
    public int getColumnCount() {
        return colNameValues.length;
    }

    @Override
    public int getRowCount() {
        return proteinList.size();
    }

    @Override
    public String getColumnName(int col) {
        return colNameValues[col].getColName();
    }

    @Override
    public Object getValueAt(int row, int col) {
        ReportProtein protein = proteinList.get(row);

        ColumnNames colEnum = colNameValues[col];

        switch (colEnum) {
        case ACCESSIONS:
            return protein.getAccessions();
        case COVERAGES:
            return "TODO: coverage";
        case DECOY:
            return protein.getIsDecoy();
        case FDR_Q_VALUE:
            return protein.getQValue();
        case NR_PEPTIDES:
            return protein.getNrPeptides();
        case NR_PSMS:
            return protein.getNrPSMs();
        case NR_SPECTRA:
            return protein.getNrSpectra();
        case SCORES:
            return protein.getScore();
        default:
            break;

        }

        return null;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        ColumnNames colEnum = colNameValues[columnIndex];

        switch (colEnum) {
        case ACCESSIONS:
            return List.class;
        case COVERAGES:
            return String.class;
        case DECOY:
            return String.class;
        case FDR_Q_VALUE:
        case SCORES:
            return Double.class;
        case NR_PEPTIDES:
        case NR_PSMS:
        case NR_SPECTRA:
            return Integer.class;
        default:
            return Object.class;
        }
    }

    /**
     * The actual columns
     *
     * @author julian
     *
     */
    private enum ColumnNames {

        ACCESSIONS("Accessions"),
        SCORES("Score"),
        COVERAGES("Coverages"),
        NR_PEPTIDES("nrPeptides"),
        NR_PSMS("nrPSMs"),
        NR_SPECTRA("nrSpectra"),
        DECOY("Decoy"),
        FDR_Q_VALUE("FDR q-value"),
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
