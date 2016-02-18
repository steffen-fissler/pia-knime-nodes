package de.mpc.pia.knime.nodes.visualization;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import de.mpc.pia.intermediate.Accession;


public class ReportTableCellRenderer extends DefaultTableCellRenderer {

    /** the number of digits for the displayed scores */
    private final static int numberDigitsScores = 4;

    /** decimal format converter for very small scores */
    private final static DecimalFormat scoreDfVerySmall = new DecimalFormat("0.###E0");

    /** decimal format converter for normal scores */
    private final static DecimalFormat scoreDfNormal = new DecimalFormat("0.######");


    public ReportTableCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object cellDate,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        Object changedCellDate = cellDate;

        if (cellDate instanceof Number) {
            changedCellDate = getStringOfNumber((Number)cellDate);
        } else if (cellDate instanceof List) {
            // get the items in the list as text
            StringBuffer textSB = new StringBuffer();

            ListIterator<?> iter = ((List<?>)cellDate).listIterator();
            while (iter.hasNext()) {
                Object objItem = iter.next();

                if (textSB.length() > 0) {
                    textSB.append(",");
                }

                if (objItem instanceof Accession) {
                    textSB.append(((Accession) objItem).getAccession());
                } if (objItem instanceof Number) {
                    textSB.append(getStringOfNumber((Number)objItem));
                }
            }

            changedCellDate = textSB.toString();
        }

        return super.getTableCellRendererComponent(table, changedCellDate,
                isSelected, hasFocus, row, column);
    }


    public static String getStringOfNumber(Number numberValue) {
        if (numberValue == null) {
            return "null";
        }

        Double value = numberValue.doubleValue();

        if (!value.equals(Double.NaN)) {
            if (!value.equals(0.0) &&
                    (Math.abs(value) < Math.pow(10.0, -numberDigitsScores))) {
                return scoreDfVerySmall.format(value);
            } else {
                return scoreDfNormal.format(value);
            }
        } else {
            return value.toString();
        }
    }

}