package com.xored.q7.reporting.example.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SampleReportingPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.xored.q7.reporting.example"; //$NON-NLS-1$

	// The shared instance
	private static SampleReportingPlugin plugin;

	/**
	 * The constructor
	 */
	public SampleReportingPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static SampleReportingPlugin getDefault() {
		return plugin;
	}

	public static IStatus createErr(String message) {
		return createErr(message, null);
	}

	public static IStatus createErr(String message, Throwable cause) {
		return new Status(IStatus.ERROR, PLUGIN_ID, message, cause);
	}

}
