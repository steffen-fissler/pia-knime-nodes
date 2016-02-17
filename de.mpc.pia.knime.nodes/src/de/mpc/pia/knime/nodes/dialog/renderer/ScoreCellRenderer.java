package de.mpc.pia.knime.nodes.dialog.renderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * Renderer which interprets the given String as a score. The String should be
 * a description of the score, e.g. the schortName, name or accession.
 *
 * @author julian
 *
 */
public class ScoreCellRenderer extends JLabel implements ListCellRenderer<String> {

    private static final long serialVersionUID = 1L;


    public ScoreCellRenderer() {
        super();
        setOpaque(true);
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        // show the name of the score
        ScoreModelEnum score = ScoreModelEnum.getModelByDescription(value);
        if ((score != null) && !ScoreModelEnum.UNKNOWN_SCORE.equals(score)) {
            this.setText(score.getName());
        } else {
            this.setText(value);
        }

        return this;
    }
}