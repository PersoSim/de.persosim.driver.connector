package de.persosim.driver.connector;

import java.util.List;

import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscListener;

public class TestDriverComm implements IfdComm {

	private List<PcscListener> listeners;
	private boolean running;

	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void setListeners(List<PcscListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void reset() {
		listeners = null;
		stop();
	}

	public PcscCallResult sendData(PcscCallData pcscCallData) {
		PcscCallResult result = null;
		for (PcscListener listener : listeners) {
			PcscCallResult resp = listener.processPcscCall(pcscCallData);
			if (result == null) {
				result = resp;
			}
		}
		return result;
	}

	@Override
	public String getName() {
		return this.getName();
	}

	@Override
	public String getUserString() {
		return this.getName();
	}

}
