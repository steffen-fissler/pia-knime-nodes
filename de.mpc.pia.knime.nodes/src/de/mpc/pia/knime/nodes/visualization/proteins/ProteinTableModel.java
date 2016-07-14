package de.mpc.pia.knime.nodes.visualization.proteins;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.modeller.protein.ReportProtein;


public class ProteinTableModel extends AbstractTableModel {

    /** decimal format converter for percentages */
    private final static DecimalFormat percentageTwoDigits = new DecimalFormat("0.##%");


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


    /**
     * Getter for the complete protein list
     * @param index
     * @return
     */
    public List<ReportProtein> getProteins() {
        return proteinList;
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
            List<String> coverages = new ArrayList<>(protein.getAccessions().size());
            for (Accession acc : protein.getAccessions()) {
                Double cov = protein.getCoverage(acc.getAccession());

                if ((cov == null) || cov.equals(Double.NaN)) {
                    coverages.add("NA");
                } else {
                    coverages.add(String.valueOf(percentageTwoDigits.format(cov)));
                }
            }
            return coverages;
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
            return List.class;
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
