package de.persosim.driver.connector.ui.handlers;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.service.NativeDriverConnectorImpl;
import de.persosim.driver.connector.ui.parts.ReaderPart;
import de.persosim.driver.connector.ui.parts.ReaderPart.ReaderType;

public class StandardReaderHandler {
	NativeDriverConnectorImpl connector;

	@Inject
	private EPartService partService;

	@Execute
	public void execute(Shell shell) {
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");

		
		if (readerPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) readerPart.getObject();

			try {
				readerPartObject.switchToReaderType(ReaderType.STANDARD);
			} catch (IOException e) {
				BasicLogger.logException(this.getClass(), e, LogLevel.ERROR);
			}
		}
	}
}
