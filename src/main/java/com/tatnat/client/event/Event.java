package com.tatnat.client.event;

/** Base class for everything posted on the {@link EventBus}. */
public abstract class Event {
	private boolean cancelled;

	/** Only meaningful for events whose poster checks it (input events, for example). */
	public void cancel() {
		cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}
}
