 
package de.persosim.driver.connector.ui.handlers;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.VirtualDriverComm;
import de.persosim.driver.connector.ui.parts.ReaderPart;

public class NativeDriverHandler {

	@Inject
	private EPartService partService;
	
	@Execute
	public void execute() {
		BasicLogger.log(this.getClass(), "Switch to use NativeDriver", LogLevel.INFO);

		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");

		
		if (readerPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) readerPart.getObject();

			try {
				readerPartObject.switchReaderType(new VirtualDriverComm(VirtualDriverComm.DEFAULT_HOST, VirtualDriverComm.DEFAULT_PORT));
			} catch (IOException e) {
				BasicLogger.logException(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}
		
}