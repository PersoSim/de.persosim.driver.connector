package de.persosim.driver.connector.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import de.persosim.driver.connector.service.IfdConnectorImpl;
import de.persosim.driver.connector.ui.parts.ReaderPart;
import de.persosim.driver.connector.ui.parts.ReaderPart.ReaderType;
import jakarta.inject.Inject;

public class StandardReaderHandler {
	IfdConnectorImpl connector;

	@Inject
	private EPartService partService;

	@Execute
	public void execute(final MPart mPart, final MItem mItem) {
		if (mItem.isSelected()) {
			// ID of part as defined in fragment.e4xmi application model
			MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");

			
			if (readerPart.getObject() instanceof ReaderPart) {
				ReaderPart readerPartObject = (ReaderPart) readerPart.getObject();

				readerPartObject.switchReaderType(ReaderType.STANDARD);
			}
		}
	}
}
