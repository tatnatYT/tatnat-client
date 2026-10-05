package com.tatnat.client.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.tatnat.client.TatnatClient;

/**
 * A tiny annotation-driven event bus.
 *
 * Listeners are scanned once when registered and stored per event class, so posting an event
 * is just a walk over a pre-built list -- no reflection lookups on the hot path. Handlers are
 * bound {@link MethodHandle}s, which the JIT inlines about as well as a direct call.
 */
public final class EventBus {
	private record Handler(Object owner, MethodHandle handle) {
	}

	private final Map<Class<?>, List<Handler>> handlers = new HashMap<>();
	private final Map<Object, List<Class<?>>> registered = new HashMap<>();

	public synchronized void register(Object listener) {
		if (registered.containsKey(listener)) return;
		List<Class<?>> types = new ArrayList<>();
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		for (Class<?> c = listener.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (!m.isAnnotationPresent(Subscribe.class)) continue;
				if (m.getParameterCount() != 1 || !Event.class.isAssignableFrom(m.getParameterTypes()[0])) {
					throw new IllegalArgumentException("@Subscribe method " + m + " must take one Event");
				}
				try {
					m.setAccessible(true);
					MethodHandle h = lookup.unreflect(m).bindTo(listener);
					Class<?> type = m.getParameterTypes()[0];
					handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(new Handler(listener, h));
					types.add(type);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		registered.put(listener, types);
	}

	public synchronized void unregister(Object listener) {
		List<Class<?>> types = registered.remove(listener);
		if (types == null) return;
		for (Class<?> t : types) {
			List<Handler> list = handlers.get(t);
			if (list != null) list.removeIf(h -> h.owner == listener);
		}
	}

	/** Delivers {@code event} to every handler for its exact class. Returns the event for chaining. */
	public <E extends Event> E post(E event) {
		List<Handler> list = handlers.get(event.getClass());
		if (list == null || list.isEmpty()) return event;
		for (Handler h : list) {
			try {
				h.handle.invoke(event);
			} catch (Throwable t) {
				// One broken module must never take the game down with it.
				TatnatClient.LOG.error("Event handler in {} failed", h.owner.getClass().getSimpleName(), t);
			}
		}
		return event;
	}
}
