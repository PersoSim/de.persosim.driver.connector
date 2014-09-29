package de.persosim.driver.connector.ui.handlers;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import de.persosim.driver.connector.NativeDriverConnector;
import de.persosim.driver.connector.ui.parts.ReaderPart;

public class StandardReaderHandler {
	NativeDriverConnector connector;

	@Inject
	private EPartService partService;

	@Execute
	public void execute(Shell shell) {
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService
				.findPart("de.persosim.driver.connector.ui.parts.reader");

		if (readerPart.getObject() instanceof ReaderPart) {

			((ReaderPart) readerPart.getObject()).switchToStandard();
		}
	}
}
