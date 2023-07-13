package de.mpc.pia.knime.nodes;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


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
}

