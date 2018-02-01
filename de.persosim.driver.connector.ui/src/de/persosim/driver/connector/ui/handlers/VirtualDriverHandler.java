 
package de.persosim.driver.connector.ui.handlers;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.VirtualDriverComm;
import de.persosim.driver.connector.exceptions.IfdCreationException;
import de.persosim.driver.connector.ui.parts.ReaderPart;

public class VirtualDriverHandler {

	@Inject
	private EPartService partService;
	
	@Execute
	public void execute() {
		BasicLogger.log(this.getClass(), "Switch to use virtual driver", LogLevel.INFO);

		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");

		
		if (readerPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) readerPart.getObject();

			try {
				readerPartObject.switchReaderType(new VirtualDriverComm(VirtualDriverComm.DEFAULT_HOST, VirtualDriverComm.DEFAULT_PORT));
			} catch (IfdCreationException e) {
				BasicLogger.logException(this.getClass(), e, LogLevel.WARN);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Connector could not be created", "The virtual driver communication could not be established.");
			}
		}
	}
		
}