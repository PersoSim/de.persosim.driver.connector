package de.persosim.driver.test;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;

import de.persosim.simulator.utils.PersoSimLogger;

public class ConnectorTest {
	public static final int SIMULATOR_PORT = 12345;
	public static final int TESTDRIVER_PORT = 12346;
	public static final String SIMULATOR_HOST = "localhost";
	public static final String TESTDRIVER_HOST = "localhost";


	@BeforeClass
	public static void setUpClass() {
		//setup logging
		PersoSimLogger.init();
		
		//register BouncyCastle provider
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}
}
