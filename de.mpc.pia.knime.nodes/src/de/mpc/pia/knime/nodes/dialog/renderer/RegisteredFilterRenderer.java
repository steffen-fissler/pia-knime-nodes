package de.mpc.pia.knime.nodes.dialog.renderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import de.mpc.pia.modeller.report.filter.RegisteredFilters;

/**
 * Renderer for {@link RegisteredFilters}, showing the name of the filter
 *
 * @author julian
 *
 */
public class RegisteredFilterRenderer extends JLabel implements ListCellRenderer<RegisteredFilters> {

    private static final long serialVersionUID = 1L;

    public RegisteredFilterRenderer() {
        super();
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends RegisteredFilters> list,
            RegisteredFilters value,
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

        setText(value.getFilteringListName());

        return this;
    }
}
