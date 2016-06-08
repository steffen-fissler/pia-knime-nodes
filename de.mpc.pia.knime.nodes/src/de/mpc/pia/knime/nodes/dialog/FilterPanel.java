package de.mpc.pia.knime.nodes.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.knime.core.node.NodeLogger;

import de.mpc.pia.knime.nodes.dialog.renderer.AbstractFilterRenderer;
import de.mpc.pia.knime.nodes.dialog.renderer.RegisteredFilterRenderer;
import de.mpc.pia.knime.nodes.dialog.renderer.ScoreCellRenderer;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.FilterFactory;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.report.filter.impl.PSMScoreFilter;
import de.mpc.pia.modeller.report.filter.impl.PSMTopIdentificationFilter;
import de.mpc.pia.modeller.report.filter.impl.PeptideScoreFilter;
import de.mpc.pia.modeller.score.ScoreModelEnum;


/**
 * This class created an extension of the {@link JPanel} to allow the creation
 * of filters. Which filters are allowed is set by calling
 * {@link #addAvailableFilter(RegisteredFilters)}.
 *
 * @author julian
 *
 */
public class FilterPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;


    /** the logger instance */
    private static final NodeLogger logger =
            NodeLogger.getLogger(FilterPanel.class);


    /** the combobox for the available filters */
    private JComboBox<RegisteredFilters> comboAvailableFilters;

    /** whether to negate the filter */
    private JCheckBox checkNegateFilter;

    /** the combobox for the available filter comparators for the selected filter*/
    private JComboBox<FilterComparator> comboAvailableFilterComparators;

    /** text field for the argument of the filter*/
    private JTextField fieldFilterArgument;

    /** the combobox for the available scores for filtering */
    private JComboBox<String> comboAvailableFilterScores;

    /** button which adds the selected filter to the applied filters */
    private JButton btnAddFilter;

    /** button which removes the selected filter from the applied filters */
    private JButton btnRemoveFilter;

    /** the list of filters which should be applied */
    private JList<AbstractFilter> listAppliedFilters;
    /** the model for the applied filters */
    private DefaultListModel<AbstractFilter> modelAppliedFilters;


    /**
     * Constructs a panel for {@link AbstractFilter}s with the given title.
     *
     * @param title
     */
    public FilterPanel(String title) {
        super(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder(title));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(new JLabel("Available Filters"), gbc);

        comboAvailableFilters = new JComboBox<RegisteredFilters>();
        comboAvailableFilters.setRenderer(new RegisteredFilterRenderer());
        comboAvailableFilters.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = 1;
        this.add(comboAvailableFilters, gbc);

        // scores for score filters
        comboAvailableFilterScores = new JComboBox<String>();
        comboAvailableFilterScores.setRenderer(new ScoreCellRenderer());
        comboAvailableFilterScores.setEditable(true);
        comboAvailableFilterScores.setEnabled(false);

        // add all possible scores
        for (ScoreModelEnum score : ScoreModelEnum.values()) {
            comboAvailableFilterScores.addItem(score.getShortName());
        }

        gbc.gridx = 0;
        gbc.gridy = 2;
        this.add(comboAvailableFilterScores, gbc);

        // negate the filter
        checkNegateFilter = new JCheckBox("not");
        checkNegateFilter.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = 1;
        this.add(checkNegateFilter, gbc);

        // the comparator for the filter
        comboAvailableFilterComparators = new JComboBox<FilterComparator>();
        updateSelectedAvailableFilters((RegisteredFilters)comboAvailableFilters.getSelectedItem());

        gbc.gridx = 2;
        gbc.gridy = 1;
        this.add(comboAvailableFilterComparators, gbc);

        // the argument for the filter
        fieldFilterArgument = new JTextField(10);
        gbc.gridx = 3;
        gbc.gridy = 1;
        this.add(fieldFilterArgument, gbc);

        // button to add the filter
        btnAddFilter = new JButton("Add");
        btnAddFilter.addActionListener(this);
        gbc.gridx = 4;
        gbc.gridy = 1;
        this.add(btnAddFilter, gbc);

        // the currently active filters
        modelAppliedFilters = new DefaultListModel<AbstractFilter>();

        listAppliedFilters = new JList<AbstractFilter>(modelAppliedFilters);
        listAppliedFilters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listAppliedFilters.setLayoutOrientation(JList.VERTICAL);
        listAppliedFilters.setCellRenderer(new AbstractFilterRenderer());

        JScrollPane listScroller = new JScrollPane(listAppliedFilters);
        listScroller.setPreferredSize(new Dimension(300, 100));

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        this.add(listScroller, gbc);

        // button to remove a filter
        btnRemoveFilter = new JButton("Remove");
        btnRemoveFilter.addActionListener(this);
        gbc.gridx = 4;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        this.add(btnRemoveFilter, gbc);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(comboAvailableFilters)) {
            RegisteredFilters selectedFilter = (RegisteredFilters)comboAvailableFilters.getSelectedItem();
            updateSelectedAvailableFilters(selectedFilter);
        } else if (e.getSource().equals(btnAddFilter)) {
            addToAppliedFilters();
        } else if (e.getSource().equals(btnRemoveFilter) && (listAppliedFilters.getSelectedIndex() > -1)) {
            modelAppliedFilters.remove(listAppliedFilters.getSelectedIndex());
        }
    }


    /**
     * Creates a filter with the current settings and adds it to the applied
     * filters.
     */
    private void addToAppliedFilters() {
        RegisteredFilters selectedFilter = (RegisteredFilters)comboAvailableFilters.getSelectedItem();
        String filterShort = selectedFilter.getShortName();

        if (selectedFilter.equals(RegisteredFilters.PSM_SCORE_FILTER)) {
            filterShort = PSMScoreFilter.prefix + comboAvailableFilterScores.getSelectedItem();
        } else if (selectedFilter.equals(RegisteredFilters.PSM_TOP_IDENTIFICATION_FILTER)) {
            filterShort = PSMTopIdentificationFilter.prefix + comboAvailableFilterScores.getSelectedItem();
        } else if (selectedFilter.equals(RegisteredFilters.PEPTIDE_SCORE_FILTER)) {
            filterShort = PeptideScoreFilter.prefix + comboAvailableFilterScores.getSelectedItem();
        }

        FilterComparator comparator = (FilterComparator)comboAvailableFilterComparators.getSelectedItem();
        String argument = fieldFilterArgument.getText();
        Boolean negate = checkNegateFilter.isSelected();

        StringBuilder messageBuffer = new StringBuilder();

        /// we have a valid value, so go on
        AbstractFilter newFilter = FilterFactory.newInstanceOf(filterShort,
                comparator, argument, negate, messageBuffer);

        if (newFilter != null) {
            ((DefaultListModel<AbstractFilter>)listAppliedFilters.getModel()).addElement(newFilter);
            messageBuffer.append("new filter added");
        } else {
            messageBuffer.insert(0, "Error adding filter: ");
            logger.error(messageBuffer.toString());
        }
    }


    /**
     * Updates the shown options for the available filter comparators. These are
     * based on the currently selected filter and whether this is a score filter
     * or a basic filter.
     *
     * @param selectedFilter
     */
    private void updateSelectedAvailableFilters(RegisteredFilters selectedFilter) {
        // set the available filter comparators
        if (selectedFilter != null) {
            if (comboAvailableFilterComparators != null) {
                comboAvailableFilterComparators.removeAllItems();
                for (FilterComparator comparator : selectedFilter.getFilterType().getAvailableComparators()) {
                    comboAvailableFilterComparators.addItem(comparator);
                }
            }

            if (comboAvailableFilterScores != null) {
                comboAvailableFilterScores.setEnabled(
                        selectedFilter.equals(RegisteredFilters.PSM_SCORE_FILTER) ||
                        selectedFilter.equals(RegisteredFilters.PSM_TOP_IDENTIFICATION_FILTER) ||
                        selectedFilter.equals(RegisteredFilters.PEPTIDE_SCORE_FILTER) ||
                        selectedFilter.equals(RegisteredFilters.PROTEIN_SCORE_FILTER));
            }
        }
    }


    /**
     * Removes all currently applied filters.
     */
    public void removeAllAppliedFilters() {
        modelAppliedFilters.removeAllElements();
    }


    /**
     * Adds the given filter to the applied filters.
     *
     * @param filter
     */
    public void addAppliedFilter(AbstractFilter filter) {
        modelAppliedFilters.addElement(filter);
    }


    /**
     * Getter for the applied filters
     *
     * @return
     */
    public List<AbstractFilter> getAppliedFilters() {
        List<AbstractFilter> filtersList = new ArrayList<AbstractFilter>(modelAppliedFilters.size());
        for (int i=0; i < modelAppliedFilters.getSize(); i++) {
            filtersList.add(modelAppliedFilters.get(i));
        }

        return filtersList;
    }


    /**
     * Adds the given {@link RegisteredFilters} to the available filters.
     * @param filter
     */
    public void addAvailableFilter(RegisteredFilters filter) {
        comboAvailableFilters.addItem(filter);
    }


    /**
     * Removes all available filters
     */
    public void removeAllAvailableFilter() {
        comboAvailableFilters.removeAllItems();
    }
}
