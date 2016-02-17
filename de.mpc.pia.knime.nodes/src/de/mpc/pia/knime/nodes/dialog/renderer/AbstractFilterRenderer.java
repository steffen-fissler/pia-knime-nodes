package de.mpc.pia.knime.nodes.dialog.renderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;

/**
 * Renderer for {@link RegisteredFilters}, showing the name of the filter
 *
 * @author julian
 *
 */
public class AbstractFilterRenderer extends JLabel implements ListCellRenderer<AbstractFilter> {

    private static final long serialVersionUID = 1L;

    public AbstractFilterRenderer() {
        super();
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends AbstractFilter> list,
            AbstractFilter value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        StringBuffer sb = new StringBuffer();

        if (value instanceof PSMScoreFilter) {
            sb.append(ScoreModelEnum.getName(((PSMScoreFilter) value).getScoreShortName()));
            sb.append(" (PSM)");
        } else if (value instanceof PSMTopIdentificationFilter) {
            sb.append(ScoreModelEnum.getName(((PSMTopIdentificationFilter) value).getScoreShortName()));
            sb.append(" (PSM Topidentifications)");
        } else if (value instanceof PeptideScoreFilter) {
            sb.append(ScoreModelEnum.getName(((PeptideScoreFilter) value).getScoreShortName()));
            sb.append(" (Peptide)");
        } else {
            sb.append(value.getFilteringListName());
        }

        sb.append(' ');
        if (value.getFilterNegate()) {
            sb.append("NOT ");
        }
        sb.append(value.getFilterComparator().getLabel());
        sb.append(' ');
        sb.append(value.getFilterValue());

        setText(sb.toString());

        return this;
    }
}
