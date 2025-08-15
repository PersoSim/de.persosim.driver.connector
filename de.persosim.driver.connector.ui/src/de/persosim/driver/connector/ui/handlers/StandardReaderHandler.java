package de.persosim.driver.connector.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

import de.persosim.driver.connector.ui.parts.ReaderPart;
import de.persosim.driver.connector.ui.parts.ReaderPart.ReaderType;
import jakarta.inject.Inject;

public class StandardReaderHandler
{
	private EPartService partService;

	@Inject
	public StandardReaderHandler(EPartService partService)
	{
		this.partService = partService;
	}

	@CanExecute
	public boolean checkState(final MItem mItem)
	{
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");
		if (readerPart.getObject() instanceof ReaderPart mPart) {
			ReaderPart readerPartObject = mPart;
			mItem.setSelected(readerPartObject.getCurrentReaderType() == ReaderType.STANDARD);
			return readerPartObject.getCurrentReaderType() != ReaderType.EXTERNAL;
		}
		return false;
	}

	@Execute
	public void execute()
	{
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");
		if (readerPart.getObject() instanceof ReaderPart mPart) {
			ReaderPart readerPartObject = mPart;
			readerPartObject.switchReaderType(ReaderType.STANDARD);
		}
	}

}
