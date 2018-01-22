package de.persosim.driver.connector;

public interface CommProvider {

	/**
	 * Build an {@link IfdComm} instance if possible.
	 * @param type
	 * @return an {@link IfdComm} instance or null
	 */
	public IfdComm get(String type);

}
