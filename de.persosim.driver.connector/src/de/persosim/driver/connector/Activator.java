package de.persosim.driver.connector;

import java.util.Hashtable;

import org.globaltester.simulator.Simulator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator for this bundle.
 * @author mboonk
 *
 */
public class Activator implements BundleActivator {

	private static BundleContext context;
	private static ServiceTracker<Simulator, Simulator> serviceTrackerSimulator;
	private static ServiceTracker<DriverConnectorFactory, DriverConnectorFactory> serviceTrackerDriverConnectorFactory;

	public static final String PERSOSIM_CONNECTOR_CONTEXT_ID = "de.persosim";

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
	
	public static DriverConnectorFactory getFactory() {
		if (serviceTrackerDriverConnectorFactory != null){

			return serviceTrackerDriverConnectorFactory.getService();
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
		bundleContext.registerService(DriverConnectorFactory.class, new DriverConnectorFactoryImpl(), props);
		
		serviceTrackerSimulator = new ServiceTracker<Simulator, Simulator>(context, Simulator.class.getName(), null);
		serviceTrackerSimulator.open();
		
		serviceTrackerDriverConnectorFactory = new ServiceTracker<DriverConnectorFactory, DriverConnectorFactory>(context, DriverConnectorFactory.class.getName(), null);
		serviceTrackerDriverConnectorFactory.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		serviceTrackerSimulator.close();
		serviceTrackerDriverConnectorFactory.close();
	}

}
