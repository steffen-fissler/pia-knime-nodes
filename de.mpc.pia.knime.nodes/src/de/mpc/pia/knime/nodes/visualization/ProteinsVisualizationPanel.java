package de.mpc.pia.knime.nodes.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.ui.RefineryUtilities;

import de.mpc.pia.intermediate.Accession;
import de.mpc.pia.intermediate.Group;
import de.mpc.pia.knime.nodes.PIAAnalysisModel;
import de.mpc.pia.knime.nodes.PIASettings;
import de.mpc.pia.knime.nodes.visualization.peptides.PeptideTableModel;
import de.mpc.pia.knime.nodes.visualization.proteins.ProteinTableModel;
import de.mpc.pia.knime.nodes.visualization.psms.PSMSetTableModel;
import de.mpc.pia.modeller.peptide.ReportPeptide;
import de.mpc.pia.modeller.protein.ReportProtein;
import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.visualization.graph.AmbiguityGroupVisualizationHandler;
import de.mpc.pia.visualization.graph.ProteinLayout;
import de.mpc.pia.visualization.graph.ProteinVertexBorderColorTransformer;
import de.mpc.pia.visualization.graph.ProteinVertexFillColorTransformer;
import de.mpc.pia.visualization.graph.ProteinVertexLabeller;
import de.mpc.pia.visualization.graph.ProteinVertexShapeTransformer;
import de.mpc.pia.visualization.graph.ProteinVisualizationGraphHandler;
import de.mpc.pia.visualization.graph.VertexObject;
import de.mpc.pia.visualization.graph.VertexRelation;
import de.mpc.protXML.ProteinSummary.ProteinGroup;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.picking.MultiPickedState;
import edu.uci.ics.jung.visualization.picking.PickedState;


public class ProteinsVisualizationPanel extends JPanel implements ListSelectionListener {

    // TODO: visualization of sub-protein-groups


    /** the shown proteins table */
    private JTable proteinTable;
    /** the shown proteins table model */
    private ProteinTableModel proteinTableModel;
    /** the index of the currently selected protein */
    private int selectedProteinIdx;

    /** additional information about the selected protein */
    private JTextArea proteinInformation;

    /** the shown peptides table */
    private JTable peptideTable;
    /** the shown peptides table model */
    private PeptideTableModel peptideTableModel;
    /** the index of the currently selected peptide */
    private int selectedPeptideIdx;

    /** the shown PSM set table */
    private JTable psmSetTable;
    /** the shown peptides table model */
    private PSMSetTableModel psmSetTableModel;
    /** the index of the currently selected peptide */
    private int selectedPSMSetIdx;

    /** the panel for the PSM information */
    private JPanel psmsPanel;

    /** the right side of the panel shows the visualization of the cluster */
    private JSplitPane protTableClusterPane;

    /** whether modifications are considered in peptide inference */
    private boolean considerModifications;

    /** mapping of the short names to names for peptide scores */
    private Map<String, String> peptideScoreNameMap;


    public ProteinsVisualizationPanel(List<ReportProtein> proteinList,
            PIAAnalysisModel model) {
        if (proteinList == null) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.add(new JLabel("No data available for visualisation."));
            this.add(new JLabel("Try re-running the node."));
        } else {
            selectedProteinIdx = -1;
            selectedPeptideIdx = -1;
            selectedPSMSetIdx = -1;

            considerModifications = model.getSettingBoolean(PIASettings.CONSIDER_MODIFICATIONS);
            peptideScoreNameMap = model.getPIAModeller().getProteinModeller().getScoreShortsToScoreNames();

            initializeVisualization(proteinList);
        }
    }


    private void initializeVisualization(List<ReportProtein> proteinList) {
        this.setLayout(new GridLayout(1, 0));

        // ProteinTable >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        proteinTableModel = new ProteinTableModel(proteinList);

        proteinTable = new JTable(proteinTableModel);
        proteinTable.setPreferredScrollableViewportSize(new Dimension(800, 200));
        proteinTable.setFillsViewportHeight(true);
        proteinTable.setDefaultRenderer(Double.class, new ReportTableCellRenderer());
        proteinTable.setDefaultRenderer(List.class, new ReportTableCellRenderer());

        proteinTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane proteinScrollPane = new JScrollPane(proteinTable);
        // ProteinTable <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        JPanel proteinBottomInfos = new JPanel();
        proteinBottomInfos.setLayout(new BoxLayout(proteinBottomInfos, BoxLayout.Y_AXIS));
        proteinBottomInfos.setPreferredSize(new Dimension(800, 500));

        proteinInformation = new JTextArea("please select a protein in the list");
        proteinInformation.setEditable(false);
        proteinInformation.setAlignmentX(CENTER_ALIGNMENT);
        proteinBottomInfos.add(proteinInformation);

        // PeptideTable >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        proteinBottomInfos.add(new JLabel("Peptides"));
        peptideTableModel = new PeptideTableModel(null, considerModifications,
                true, peptideScoreNameMap);

        peptideTable = new JTable(peptideTableModel);
        peptideTable.setFillsViewportHeight(true);
        peptideTable.setDefaultRenderer(Double.class, new ReportTableCellRenderer());
        peptideTable.setDefaultRenderer(List.class, new ReportTableCellRenderer());

        peptideTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane peptideScrollPane = new JScrollPane(peptideTable);
        peptideScrollPane.setAlignmentX(CENTER_ALIGNMENT);
        proteinBottomInfos.add(peptideScrollPane);
        // PeptideTable <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


        // PSMSetTable >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        JPanel psmSetsPanel = new JPanel();
        psmSetsPanel.setLayout(new BoxLayout(psmSetsPanel, BoxLayout.Y_AXIS));
        psmSetsPanel.setPreferredSize(new Dimension(600, 200));

        psmSetsPanel.add(new JLabel("PSM sets"));

        psmSetTableModel = new PSMSetTableModel(null);

        psmSetTable = new JTable(psmSetTableModel);
        psmSetTable.setPreferredScrollableViewportSize(new Dimension(600, 200));
        psmSetTable.setFillsViewportHeight(true);
        psmSetTable.setDefaultRenderer(Double.class, new ReportTableCellRenderer());
        psmSetTable.setDefaultRenderer(List.class, new ReportTableCellRenderer());

        psmSetTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane psmSetScrollPane = new JScrollPane(psmSetTable);
        psmSetsPanel.add(psmSetScrollPane);
        // PSMSetTable <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // PSMs >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        JPanel psmsContainerPanel = new JPanel();
        psmsContainerPanel.setLayout(new BoxLayout(psmsContainerPanel, BoxLayout.Y_AXIS));
        psmsContainerPanel.add(new JLabel("PSMs"));

        psmsPanel = new JPanel();
        psmsPanel.setLayout(new BoxLayout(psmsPanel, BoxLayout.Y_AXIS));

        JScrollPane psmsScrollPane = new JScrollPane(psmsPanel);
        psmsContainerPanel.add(psmsScrollPane);
        // PSMs <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<


        JSplitPane psmsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                psmSetsPanel, psmsContainerPanel);
        psmsSplitPane.setAlignmentX(CENTER_ALIGNMENT);
        proteinBottomInfos.add(psmsSplitPane);


        JPanel clusterVisualizationPanelDummy = new JPanel();
        clusterVisualizationPanelDummy.setPreferredSize(new Dimension(500, 400));

        protTableClusterPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                proteinScrollPane, clusterVisualizationPanelDummy);

        JSplitPane protTopBottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                protTableClusterPane, proteinBottomInfos);
        this.add(protTopBottomSplitPane);

        psmsSplitPane.setDividerLocation(0.6);
    }


    @Override
    public void valueChanged(ListSelectionEvent event) {

        if (event.getSource() instanceof DefaultListSelectionModel) {
            // check for protein selection changes
            int proteinIdx = proteinTable.getSelectedRow();
            if (proteinIdx >= 0) {
                proteinIdx = proteinTable.convertRowIndexToModel(proteinIdx);
            }
            if (proteinIdx != selectedProteinIdx) {
                // the selected protein changed
                updateSelectedProtein(proteinIdx);
            }

            int peptideIdx = peptideTable.getSelectedRow();
            if (peptideIdx >= 0) {
                peptideIdx = peptideTable.convertRowIndexToModel(peptideIdx);
            }
            if (peptideIdx != selectedPeptideIdx) {
                // the selected peptide changed
                updateSelectedPeptide(peptideIdx);
            }

            int psmSetIdx = psmSetTable.getSelectedRow();
            if (psmSetIdx >= 0) {
                psmSetIdx = psmSetTable.convertRowIndexToModel(psmSetIdx);
            }
            if (psmSetIdx != selectedPSMSetIdx) {
                // the selected PSM set changed
                updateSelectedPSMSet(psmSetIdx);
            }
        }
    }


    /**
     * Updates the visualization information if the selected protein was
     * changed.
     *
     * @param proteinIdx
     */
    public void updateSelectedProtein(int proteinIdx) {
        selectedProteinIdx = proteinIdx;
        if (proteinIdx < 0) {
            proteinInformation.setText("please select a protein in the list");
            peptideTableModel.updatePeptideList(null,
                    considerModifications, true, peptideScoreNameMap);
            return;
        }

        ReportProtein protein = proteinTableModel.getProteinAt(proteinIdx);
        StringBuffer infoSB = new StringBuffer();

        for (Accession acc : protein.getAccessions()) {
            if (infoSB.length() > 0) {
                infoSB.append("\n");
            }

            infoSB.append(acc.getAccession());

            Boolean decoyState = protein.getAccessionDecoyState(acc.getAccession());
            if ((decoyState != null) && decoyState) {
                infoSB.append(" (decoy)");
            }

            infoSB.append(' ');
            infoSB.append(acc.getDescription(0L));
        }

        proteinInformation.setText(infoSB.toString());

        peptideTableModel.updatePeptideList(protein.getPeptides(),
                considerModifications, true, peptideScoreNameMap);


        // TODO: take the actual selections and groups/subgroups etc. into account
        ReportPeptide anyPeptide = protein.getPeptides().get(0);
        PSMReportItem anyPSM = anyPeptide.getPSMs().get(0);
        Group anyGroup = anyPSM.getPeptide().getGroup();


        List<Map<VertexRelation, Set<Long>>> relations = calculateVertexRelations(protein);

        AmbiguityGroupVisualizationHandler visGraph = new AmbiguityGroupVisualizationHandler(anyGroup,
                relations.get(0),
                relations.get(1),
                relations.get(2),
                protein);

        GraphZoomScrollPane panel = new GraphZoomScrollPane(visGraph.getVisualizationViewer());

        int dividerLoc = protTableClusterPane.getDividerLocation();
        protTableClusterPane.setRightComponent(panel);
        protTableClusterPane.setDividerLocation(dividerLoc);
    }


    /**
     * Updates the visualization information if the selected peptide was
     * changed.
     *
     * @param proteinIdx
     */
    public void updateSelectedPeptide(int peptideIdx) {
        selectedPeptideIdx = peptideIdx;
        if (peptideIdx < 0) {
            psmSetTableModel.updatePSMSetList(null);
            return;
        }

        ReportPeptide peptide = peptideTableModel.getPeptideAt(peptideIdx);

        psmSetTableModel.updatePSMSetList(peptide.getPSMs());
    }


    /**
     * Updates the visualization information if the selected PSM set was
     * changed.
     *
     * @param proteinIdx
     */
    public void updateSelectedPSMSet(int psmSetIdx) {
        selectedPSMSetIdx = psmSetIdx;
        if (psmSetIdx < 0) {
            psmsPanel.removeAll();
            return;
        }

        ReportPSMSet psmSet = psmSetTableModel.getPSMSetAt(psmSetIdx);

        psmsPanel.removeAll();
        for (ReportPSM psm : psmSet.getPSMs()) {
            JPanel psmPanel = new JPanel();
            psmPanel.setBorder(BorderFactory.createTitledBorder(psm.getFileName()));
            psmPanel.setLayout(new GridLayout(psm.getScores().size(), 2));

            for (ScoreModel score : psm.getScores()) {
                JTextArea scoreName = new JTextArea(peptideScoreNameMap.get(score.getShortName()));
                scoreName.setEditable(false);

                JTextArea scoreValue = new JTextArea(
                        ReportTableCellRenderer.getStringOfNumber(score.getValue()));
                scoreValue.setEditable(false);

                psmPanel.add(scoreName);
                psmPanel.add(scoreValue);
            }

            psmsPanel.add(psmPanel);
        }

        psmsPanel.add(Box.createVerticalGlue());

        psmsPanel.revalidate();
    }



    private List<Map<VertexRelation, Set<Long>>> calculateVertexRelations(ReportProtein protein) {
        Map<VertexRelation, Set<Long>> relationsAccessions = new HashMap<>();
        Map<VertexRelation, Set<Long>> relationsPeptides = new HashMap<>();
        Map<VertexRelation, Set<Long>> relationsSpectra = new HashMap<>();
        Set<Long> doneAccs = new HashSet<>();
        Set<Long> donePeps = new HashSet<>();
        Set<Long> doneSpectra = new HashSet<>();

        // get the same PAGs first
        List<Set<Long>> proteinsIds = getConnectedIdsOfReportProtein(protein);
        relationsAccessions.put(VertexRelation.IN_SAME_PAG, proteinsIds.get(0));
        relationsPeptides.put(VertexRelation.IN_SAME_PAG, proteinsIds.get(1));
        relationsSpectra.put(VertexRelation.IN_SAME_PAG, proteinsIds.get(2));

        doneAccs.addAll(proteinsIds.get(0));
        donePeps.addAll(proteinsIds.get(1));
        doneSpectra.addAll(proteinsIds.get(2));

        // the sub-PAGs
        Set<Long> setAccs = new HashSet<>();
        Set<Long> setPeps = new HashSet<>();
        Set<Long> setSpectra = new HashSet<>();
        for (ReportProtein subProtein : protein.getSubSets()) {
            proteinsIds = getConnectedIdsOfReportProtein(subProtein);
            setAccs.addAll(proteinsIds.get(0));
            setPeps.addAll(proteinsIds.get(1));
            setSpectra.addAll(proteinsIds.get(2));
        }
        setAccs.removeAll(doneAccs);
        setPeps.removeAll(donePeps);
        setSpectra.removeAll(doneSpectra);

        relationsAccessions.put(VertexRelation.IN_SUB_PAG, setAccs);
        relationsPeptides.put(VertexRelation.IN_SUB_PAG, setPeps);
        relationsSpectra.put(VertexRelation.IN_SUB_PAG, setSpectra);

        doneAccs.addAll(setAccs);
        donePeps.addAll(setPeps);
        doneSpectra.addAll(setSpectra);

        // get parallel PAGs (i.e. all others for now)
        setAccs = new HashSet<>();
        setPeps = new HashSet<>();
        setSpectra = new HashSet<>();
        for (ReportProtein otherProt : proteinTableModel.getProteins()) {
            if (doneAccs.contains(otherProt.getAccessions().get(0).getAccession())) {
                continue;
            }

            proteinsIds = getConnectedIdsOfReportProtein(otherProt);
            setAccs.addAll(proteinsIds.get(0));
            setPeps.addAll(proteinsIds.get(1));
            setSpectra.addAll(proteinsIds.get(2));
        }
        setAccs.removeAll(doneAccs);
        setPeps.removeAll(donePeps);
        setSpectra.removeAll(doneSpectra);

        relationsAccessions.put(VertexRelation.IN_PARALLEL_PAG, setAccs);
        relationsPeptides.put(VertexRelation.IN_PARALLEL_PAG, setPeps);
        relationsSpectra.put(VertexRelation.IN_PARALLEL_PAG, setSpectra);

        //VertexRelation.IN_SUPER_PAG           no sub-protein can be selected -> not possible now
        //VertexRelation.IN_UNRELATED_PAG       no sub-protein can be selected -> not possible now

        List<Map<VertexRelation, Set<Long>>> res = new ArrayList<Map<VertexRelation,Set<Long>>>(3);
        res.add(relationsAccessions);
        res.add(relationsPeptides);
        res.add(relationsSpectra);

        return res;
    }


    private List<Set<Long>> getConnectedIdsOfReportProtein(ReportProtein protein) {
        Set<Long> accs = new HashSet<>();
        Set<Long> peps = new HashSet<>();
        Set<Long> spectra = new HashSet<>();

        for (Accession acc : protein.getAccessions()) {
            accs.add(acc.getID());
        }

        for (ReportPeptide peptide : protein.getPeptides()) {
            peps.add(peptide.getPeptide().getID());

            for (PSMReportItem repPSM : peptide.getPSMs()) {
                if (repPSM instanceof ReportPSM) {
                    spectra.add(((ReportPSM) repPSM).getSpectrum().getID());
                } else if (repPSM instanceof ReportPSMSet) {
                    for (ReportPSM psm : ((ReportPSMSet) repPSM).getPSMs()) {
                        spectra.add(psm.getSpectrum().getID());
                    }
                }
            }
        }

        List<Set<Long>> res = new ArrayList<Set<Long>>(3);
        res.add(accs);
        res.add(peps);
        res.add(spectra);

        return res;
    }
}
