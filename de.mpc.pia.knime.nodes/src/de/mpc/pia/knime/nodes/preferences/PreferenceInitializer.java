package de.mpc.pia.knime.nodes.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.mpc.pia.knime.nodes.PIANodesPlugin;


public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /** Preferences key for the usage statistics visitorCid. */
    public static final String PREF_USAGE_STATISTICS_VISITOR_CID = "de.mpc.pia.usageStatisticsVisitorCid";

    /** Preferences key for the field to turn off usage statistics collections at all. */
    public static final String PREF_USAGE_STATISTICS_OFF = "de.mpc.pia.usageStatisticsDisabled";


    @Override
    public void initializeDefaultPreferences() {
        // get the preference store for the UI plugin
        IPreferenceStore store = PIANodesPlugin.getDefault().getPreferenceStore();

        // current or old default values get overwritten by new defaults, therefore this is a workaround to initialize the random value at another place
        store.setDefault(PREF_USAGE_STATISTICS_VISITOR_CID, "newCid"); //$NON-NLS-1$
        store.setDefault(PREF_USAGE_STATISTICS_OFF, false);
    }
}
