package de.mpc.pia.knime.nodes.visualization.psmspectrumviewer;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import de.mpc.pia.knime.nodes.visualization.ReportTableCellRenderer;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.visualization.spectra.PiaPsmToSpectrum;
import uk.ac.ebi.pride.utilities.data.core.Spectrum;


/**
 * This panel shows the PSMs (all, not in sets) andvisualizes the annotated
 * spectra.
 *
 * @author julian
 *
 */
public class PSMSpectrumViewerPanel extends JPanel implements ListSelectionListener {

    private static final long serialVersionUID = -2107614966374823815L;

    /** the PSM to spectra matcher */
    private PiaPsmToSpectrum<ReportPSM> psmToSpectrum;

    /** the list of PSMs to show */
    private List<ReportPSM> psmList;

    /** the panel holding the visualizer  */
    private JPanel spectrumVisualizerPanel;

    /** the shown PSM set table */
    private JTable psmTable;
    /** the shown PSM table model */
    private PSMTableModel psmTableModel;
    /** the index of the currently selected peptide */
    private int selectedPSMIdx;
    /** the PSM sorter and filter */
    private TableRowSorter<PSMTableModel> psmSorter;

    /** the sequence filter text */
    private JTextField sequenceFilter;


    public PSMSpectrumViewerPanel(List<PSMReportItem> psmList,
            PiaPsmToSpectrum<ReportPSM> psmToSpectrum) {
        if (psmList == null) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.add(new JLabel("No data available for visualisation."));
            this.add(new JLabel("Try re-running the node."));
        } else {
            this.psmToSpectrum = psmToSpectrum;

            // add the single PSMs (resolve the sets)
            this.psmList = new ArrayList<>(psmList.size());
            for (PSMReportItem item : psmList) {
                if (item instanceof ReportPSM) {
                    this.psmList.add((ReportPSM) item);
                } else if (item instanceof ReportPSMSet) {
                    this.psmList.addAll(((ReportPSMSet) item).getPSMs());
                }
            }

            initialize();
        }
    }


    /**
     * Initialize the panel
     */
    private void initialize() {
        this.setLayout(new GridLayout(1, 0));

        // PSMTable >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        JPanel psmPanel = new JPanel();
        psmPanel.setLayout(new BoxLayout(psmPanel, BoxLayout.Y_AXIS));
        psmPanel.setPreferredSize(new Dimension(600, 300));
        psmPanel.setMinimumSize(new Dimension(600, 150));

        psmPanel.add(new JLabel("PSMs"));

        psmTableModel = new PSMTableModel(psmList);

        psmTable = new JTable(psmTableModel);
        psmTable.setPreferredScrollableViewportSize(new Dimension(600, 300));
        psmTable.setFillsViewportHeight(true);
        psmTable.setDefaultRenderer(Double.class, new ReportTableCellRenderer());
        psmTable.setDefaultRenderer(List.class, new ReportTableCellRenderer());

        psmTable.getSelectionModel().addListSelectionListener(this);

        psmSorter = new TableRowSorter<>(psmTableModel);
        psmTable.setRowSorter(psmSorter);

        JScrollPane psmSetScrollPane = new JScrollPane(psmTable);
        psmPanel.add(psmSetScrollPane);
        // PSMTable <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // filters >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        JPanel filters = new JPanel(new SpringLayout());
        filters.setLayout(new GridLayout(0, 2));

        JLabel seqFilterLabel = new JLabel("Sequence Filter:", SwingConstants.TRAILING);
        filters.add(seqFilterLabel);
        sequenceFilter = new JTextField();
        //Whenever filterText changes, invoke newFilter.
        sequenceFilter.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        filterChanged();
                    }
                    public void insertUpdate(DocumentEvent e) {
                        filterChanged();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        filterChanged();
                    }
                });
        seqFilterLabel.setLabelFor(sequenceFilter);
        filters.add(sequenceFilter);

        psmPanel.add(filters);
        // filters <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // SpectrumVisualizer >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        spectrumVisualizerPanel = new JPanel();
        spectrumVisualizerPanel.setLayout(new GridBagLayout());
        updateSelectedPSM(-1);
        spectrumVisualizerPanel.setPreferredSize(new Dimension(800, 500));
        spectrumVisualizerPanel.setMinimumSize(new Dimension(600, 400));
        // SpectrumVisualizer <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        JSplitPane psmsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                psmPanel, spectrumVisualizerPanel);

        this.add(psmsSplitPane);
        psmsSplitPane.setDividerLocation(0.4);
    }


    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource() instanceof DefaultListSelectionModel) {
            // check new PSM index
            int psmIdx = psmTable.getSelectedRow();
            if (psmIdx >= 0) {
                psmIdx = psmTable.convertRowIndexToModel(psmIdx);
            }
            if (psmIdx != selectedPSMIdx) {
                // the selected PSM set changed
                updateSelectedPSM(psmIdx);
            }
        }
    }


    /**
     * Update the shown information due to the change of the selected PSM.
     *
     * @param psmIndex
     */
    private void updateSelectedPSM(int psmIndex) {
        selectedPSMIdx = psmIndex;
        if (psmToSpectrum == null) {
            spectrumVisualizerPanel.removeAll();
            spectrumVisualizerPanel.add(new JLabel("No spectrum file given. Re-run the node with an attached spectrum file."));
            spectrumVisualizerPanel.revalidate();
        } else if (selectedPSMIdx < 0) {
            spectrumVisualizerPanel.removeAll();
            spectrumVisualizerPanel.add(new JLabel("Please select a PSM in the list."));
            spectrumVisualizerPanel.revalidate();
        } else {
            ReportPSM psm = psmTableModel.getPSMAt(selectedPSMIdx);
            Spectrum spectrum = psmToSpectrum.getSpectrumForPSM(psm);

            PSMSpectrumAnnotator.annotateSpectrumInPanel(psm, spectrum, spectrumVisualizerPanel);
        }
    }


    /**
     * The filters were changed, update the filtering and the list.
     */
    private void filterChanged() {
        // TODO: add more filters!

        // string comparison: sequence, sourceID spectrumTitle
        // string comparison: accession
        // number bound: charge, m/z, delta, deltaPPM, RT, missed
        // modification

        List<RowFilter<PSMTableModel, Object>> filterList = new ArrayList<>(5);
        //If current expression doesn't parse, don't update.

        try {
            // add the sequence filter
            filterList.add(RowFilter.regexFilter(sequenceFilter.getText(), 0));
        } catch (PatternSyntaxException e) {

        }

        psmSorter.setRowFilter(RowFilter.andFilter(filterList));
    }
}
