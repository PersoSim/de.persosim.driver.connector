package de.persosim.driver.connector.ui.handlers;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import de.persosim.driver.connector.NativeDriverConnector;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.features.MctReaderDirect;
import de.persosim.driver.connector.features.MctUniversal;
import de.persosim.driver.connector.features.ModifyPinDirect;
import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.features.VerifyPinDirect;

public class NativeConnectorHandler {
	NativeDriverConnector connector;

	@Inject private EPartService partService;
	
	@Execute
	public void execute(Shell shell) {
		if (connector == null){

			
			try {
				connector = new NativeDriverConnector(
						"localhost", 5678, "localhost", 9876);

				
				// ID of part as defined in fragment.e4xmi application model
				MPart pinPadPart = partService.findPart("de.persosim.driver.connector.ui.parts.pinPad");
				pinPadPart.setToBeRendered(true);
				if (pinPadPart.getObject() instanceof VirtualReaderUi){
					connector.addUi((VirtualReaderUi) pinPadPart.getObject());
				}
				connector.addListener(new VerifyPinDirect(new UnsignedInteger(
						0x42000DB2)));
				connector.addListener(new ModifyPinDirect(new UnsignedInteger(
						0x42000DB3)));
				connector.addListener(new MctReaderDirect(new UnsignedInteger(
						0x42000DB4)));
				connector
						.addListener(new MctUniversal(new UnsignedInteger(0x42000DB5)));
				connector.addListener(new PersoSimPcscProcessor(new UnsignedInteger(
						0x42000DCC)));
				connector.connect();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				connector.disconnect();
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			connector = null;
		}
	}
}
