package net.lax1dude.eaglercraft.backend.server.standalone;

import java.lang.reflect.Proxy;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentBuilder;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentHelper;

final class StandaloneComponentHelper implements IPlatformComponentHelper {

	private final IPlatformComponentBuilder builder = (IPlatformComponentBuilder) Proxy.newProxyInstance(
			IPlatformComponentBuilder.class.getClassLoader(), new Class<?>[] { IPlatformComponentBuilder.class },
			(proxy, method, args) -> {
				if (method.getName().startsWith("build")) {
					return componentProxy(method.getReturnType());
				}
				return null;
			});

	private static Object componentProxy(Class<?> type) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
			if (method.getName().equals("end")) return "disconnect.closed";
			if (method.getReturnType().isInterface()) return proxy;
			if (method.getReturnType() == String.class) return method.getName();
			return null;
		});
	}

	public IPlatformComponentBuilder builder() { return builder; }
	public Class<?> getComponentType() { return String.class; }
	public Object getStandardKickAlreadyPlaying() { return "Already connected"; }
	public String serializeLegacySection(Object component) { return String.valueOf(component); }
	public String serializePlainText(Object component) { return String.valueOf(component); }
	public String serializeGenericJSON(Object component) { return "{\"text\":\"" + component + "\"}"; }
	public String serializeLegacyJSON(Object component) { return serializeGenericJSON(component); }
	public String serializeModernJSON(Object component) { return serializeGenericJSON(component); }
	public Object parseGenericJSON(String json) { return json; }
	public Object parseLegacyJSON(String json) { return json; }
	public Object parseModernJSON(String json) { return json; }
}