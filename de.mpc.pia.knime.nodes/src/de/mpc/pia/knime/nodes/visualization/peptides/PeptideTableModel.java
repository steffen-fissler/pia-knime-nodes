package de.mpc.pia.knime.nodes.visualization.peptides;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import de.mpc.pia.modeller.peptide.ReportPeptide;


public class PeptideTableModel extends AbstractTableModel {

    /** the shown data */
    private List<ReportPeptide> peptideList;

    /** whether to consider (and show) modifications */
    private boolean considerModifications;

    /** whether PSMs or PSM sets are shown */
    private boolean hasPSMsets;

    /** mapping from the score short names to the score */
    private SortedMap<String, String> scoreNameMap;

    /** numer of columns, depends on the scores*/
    private int columnCount = 0;


    public PeptideTableModel(List<ReportPeptide> peptideList, boolean considerModifications,
            boolean hasPSMsets, Map<String, String> scoreNameMap) {
        super();

        updatePeptideList(peptideList, considerModifications, hasPSMsets, scoreNameMap);
    }


    /**
     * Updates the shown information.
     *
     * @param peptideList
     * @param considerModifications
     * @param hasPSMsets
     * @param scoreNameMap
     */
    public void updatePeptideList(List<ReportPeptide> peptideList,
            boolean considerModifications, boolean hasPSMsets,
            Map<String, String> scoreNameMap) {
        if (peptideList == null) {
            this.peptideList = Collections.emptyList();
        } else {
            this.peptideList = peptideList;
        }

        this.considerModifications = considerModifications;
        this.hasPSMsets = hasPSMsets;
        this.scoreNameMap = new TreeMap<String, String>(scoreNameMap);

        columnCount = 5;
        if (considerModifications) {
            columnCount++;
        }
        columnCount += scoreNameMap.size();

        fireTableDataChanged();
    }


    /**
     * Getter for the peptide at the given row
     * @param index
     * @return
     */
    public ReportPeptide getPeptideAt(int index) {
        if ((index < 0) || (index >= getRowCount())) {
            return null;
        }
        return peptideList.get(index);
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public int getRowCount() {
        return peptideList.size();
    }

    @Override
    public String getColumnName(int col) {
        if (col == 0) {
            return "Sequence";
        }

        if (col == 1) {
            return "Accessions";
        }

        int rowCheck = 2;

        if (considerModifications) {
            if (col == rowCheck)  {
                return "Modifications";
            }
            rowCheck++;
        }

        if (col == rowCheck)  {
            return "nrSpectra";
        }
        rowCheck++;

        if (col == rowCheck)  {
            return hasPSMsets ? "nr PSM sets" : "nrPSMs";
        }
        rowCheck++;

        if (col == rowCheck)  {
            return "Missed Cleavages";
        }
        rowCheck++;

        for (Map.Entry<String, String> scoreIt : scoreNameMap.entrySet()) {
            if (col == rowCheck)  {
                return scoreIt.getValue();
            }
            rowCheck++;
        }

        return null;
    }

    @Override
    public Object getValueAt(int row, int col) {

        ReportPeptide peptide = peptideList.get(row);

        if (col == 0) {
            return peptide.getSequence();
        }

        if (col == 1) {
            return peptide.getAccessions();
        }

        int rowCheck = 2;

        if (considerModifications) {
            if (col == rowCheck)  {
                return peptide.getPSMs().get(0).getModificationsString();
            }
            rowCheck++;
        }

        if (col == rowCheck)  {
            return peptide.getNrSpectra();
        }
        rowCheck++;

        if (col == rowCheck)  {
            return peptide.getNrPSMs();
        }
        rowCheck++;

        if (col == rowCheck)  {
            return peptide.getMissedCleavages();
        }
        rowCheck++;

        for (Map.Entry<String, String> scoreIt : scoreNameMap.entrySet()) {
            if (col == rowCheck)  {
                return peptide.getBestScore(scoreIt.getKey());
            }
            rowCheck++;
        }

        return null;
    }

    @Override
    public Class getColumnClass(int col) {

        if (col == 0) {
            return String.class;
        }

        if (col == 1) {
            return List.class;
        }

        int rowCheck = 2;

        if (considerModifications) {
            if (col == rowCheck)  {
                return String.class;
            }
            rowCheck++;
        }

        if (col == rowCheck)  {
            return Integer.class;
        }
        rowCheck++;

        if (col == rowCheck)  {
            return Integer.class;
        }
        rowCheck++;

        if (col == rowCheck)  {
            return Integer.class;
        }
        rowCheck++;

        if ((col >= rowCheck) && (col <= rowCheck + scoreNameMap.size()))  {
            return Double.class;
        }
        rowCheck += scoreNameMap.size();

        return String.class;
    }
}
