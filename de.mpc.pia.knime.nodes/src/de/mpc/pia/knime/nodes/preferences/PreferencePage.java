package de.mpc.pia.knime.nodes.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.mpc.pia.knime.nodes.PIANodesPlugin;

/**
 * Preference settings page for PIA
 *
 * @author julian
 *
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /** field to turn off usage statistics */
    private BooleanFieldEditor usageStatisticsDisabledFieldEditor;

    /** field to edit the visitor Cid */
    private StringFieldEditor visitorCidFieldEditor;


    /**
     * Default creator
     */
    public PreferencePage() {
        super(GRID);
        IPreferenceStore store = PIANodesPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(store);
        setDescription("Preferences for PIA - Protein Inference Algorithms");
    }


    @Override
    public void init(IWorkbench workbench) {

    }


    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        visitorCidFieldEditor = new StringFieldEditor(PreferenceInitializer.PREF_USAGE_STATISTICS_VISITOR_CID,
                "visitor CID (cannot be changed by user)", parent);
        visitorCidFieldEditor.setEnabled(false, parent);
        addField(visitorCidFieldEditor);

        usageStatisticsDisabledFieldEditor = new BooleanFieldEditor(
                PreferenceInitializer.PREF_USAGE_STATISTICS_OFF, "Disable sending of usage statistics", parent);
        addField(usageStatisticsDisabledFieldEditor);
    }


    @Override
    public boolean performOk() {
        IPreferenceStore store = PIANodesPlugin.getDefault().getPreferenceStore();

        boolean usageStatisticsDisabled = usageStatisticsDisabledFieldEditor.getBooleanValue();
        store.setValue(PreferenceInitializer.PREF_USAGE_STATISTICS_OFF, usageStatisticsDisabled);

        String visitorCid = visitorCidFieldEditor.getStringValue();
        store.setValue(PreferenceInitializer.PREF_USAGE_STATISTICS_VISITOR_CID, visitorCid);

        return true;
    }
}
