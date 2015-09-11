package de.persosim.driver.connector;

import java.util.Hashtable;

import org.globaltester.simulator.Simulator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.persosim.driver.connector.service.NativeDriverConnectorInterface;

/**
 * The activator for this bundle.
 * @author mboonk
 *
 */
public class Activator implements BundleActivator {

	private static BundleContext context;
	private static ServiceTracker<Simulator, Simulator> serviceTrackerSimulator;
	private static ServiceTracker<NativeDriverConnectorInterface, NativeDriverConnectorInterface> serviceTrackerNativeDriverConnector;

	static BundleContext getContext() {
		return context;
	}
	
	/**
	 * @return the OSGi-provided simulator service or null if it is not available
	 */
	public static Simulator getSim() {
		if (serviceTrackerSimulator != null){

			return serviceTrackerSimulator.getService();
		}
		return null;
	}
	
	public static NativeDriverConnectorInterface getConnector() {
		if (serviceTrackerNativeDriverConnector != null){

			return serviceTrackerNativeDriverConnector.getService();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {

		Activator.context = bundleContext;
		
		//register service in service registry
		Hashtable<String, String> props = new Hashtable<String, String>();
		bundleContext.registerService(NativeDriverConnectorInterface.class, new NativeDriverConnector(), props);
		
		serviceTrackerSimulator = new ServiceTracker<Simulator, Simulator>(context, Simulator.class.getName(), null);
		serviceTrackerSimulator.open();
		
		serviceTrackerNativeDriverConnector = new ServiceTracker<NativeDriverConnectorInterface, NativeDriverConnectorInterface>(context, NativeDriverConnectorInterface.class.getName(), null);
		serviceTrackerNativeDriverConnector.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		serviceTrackerSimulator.close();
		serviceTrackerNativeDriverConnector.close();
	}

}
