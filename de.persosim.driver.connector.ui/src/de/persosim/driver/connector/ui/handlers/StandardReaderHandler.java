package de.persosim.driver.connector.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;

import de.persosim.driver.connector.service.IfdConnectorImpl;
import de.persosim.driver.connector.ui.parts.ReaderPart;
import de.persosim.driver.connector.ui.parts.ReaderPart.ReaderType;

public class StandardReaderHandler {
	IfdConnectorImpl connector;
	
	@CanExecute
	public boolean checkState(final MPart mPart, final MItem mItem) {
		if (mPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) mPart.getObject();
			mItem.setSelected(readerPartObject.getCurrentReaderType() == ReaderType.STANDARD);
			return readerPartObject.getCurrentReaderType() != ReaderType.EXTERNAL;
		}
		return false;
	}

	@Execute
	public void execute(final MPart mPart, final MItem mItem) {
		if (mPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) mPart.getObject();

			readerPartObject.switchReaderType(ReaderType.STANDARD);
		}
	}
}
