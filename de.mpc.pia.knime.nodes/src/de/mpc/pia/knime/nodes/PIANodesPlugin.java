package de.mpc.pia.knime.nodes;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.mpc.pia.knime.nodes.preferences.PreferenceInitializer;


/**
 * This is the eclipse bundle activator.
 *
 * @author julianu
 */
public class PIANodesPlugin extends AbstractUIPlugin {
    // The shared instance.
    private static PIANodesPlugin piaPlugin;


    /**
     * This method is called upon plug-in activation.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        piaPlugin = this;
    }


    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        piaPlugin = null;
        super.stop(context);
    }


    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Plugin
     */
    public static PIANodesPlugin getDefault() {
        return piaPlugin;
    }


    /**
     * @return isUsageStatisticsDisabled
     */
    public static boolean isUsageStatisticsDisabled() {
        IPreferenceStore store = PIANodesPlugin.getDefault().getPreferenceStore();
        boolean isUsageStatisticsDisabled = store.getBoolean(PreferenceInitializer.PREF_USAGE_STATISTICS_OFF);

        return isUsageStatisticsDisabled;
    }


    /**
     * @return the visitorCid
     */
    public static String getVisitorCid() {
        IPreferenceStore store = PIANodesPlugin.getDefault().getPreferenceStore();
        String visitorCid = store.getString(PreferenceInitializer.PREF_USAGE_STATISTICS_VISITOR_CID);

        if (visitorCid.length() < 16) {
            // initialize a new visitorCid
            SecureRandom random = new SecureRandom();
            visitorCid = new BigInteger(64, random).toString(16);

            store.setValue(PreferenceInitializer.PREF_USAGE_STATISTICS_VISITOR_CID, visitorCid);
        }

        return visitorCid;
    }
}

