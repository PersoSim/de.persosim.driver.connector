package de.persosim.driver.connector.pcsc;

import java.util.Collection;

import de.persosim.driver.connector.VirtualReaderUi;

public interface UiEnabled {
	public abstract void setUserInterfaces(Collection<VirtualReaderUi> interfaces);
}
