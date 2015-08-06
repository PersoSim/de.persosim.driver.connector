package de.persosim.driver.connector.ui;

import java.io.IOException;
import java.net.UnknownHostException;

import de.persosim.driver.connector.NativeDriverConnector;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.features.MctReaderDirect;
import de.persosim.driver.connector.features.MctUniversal;
import de.persosim.driver.connector.features.ModifyPinDirect;
import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.features.VerifyPinDirect;

/**
 * This class serves as a simple console interface to the native driver
 * connector.
 * 
 * @author mboonk
 *
 */
public class NativeDriverConnectorConsole {

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		NativeDriverConnector connector = new NativeDriverConnector();
		connector.addUi(new ConsoleUi());
		connector.addListener(new VerifyPinDirect(new UnsignedInteger(0x42000DB2)));
		connector.addListener(new ModifyPinDirect(new UnsignedInteger(0x42000DB3)));
		connector.addListener(new MctReaderDirect(new UnsignedInteger(0x42000DB4)));
		connector.addListener(new MctUniversal(new UnsignedInteger(0x42000DB5)));
		connector.addListener(new PersoSimPcscProcessor(new UnsignedInteger(0x42000DCC)));
		connector.connect("localhost", 5678);

	}

}

