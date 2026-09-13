package net.lax1dude.eaglercraft.backend.server.standalone;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import io.netty.handler.codec.http.FullHttpRequest;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchAdapter;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchCallback;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IRegisterSkinDelegate;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerLoginConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPendingConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthCheckRequiredEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthCookieEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthPasswordEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftClientBrandEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftDestroyPlayerEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftInitializePlayerEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftMOTDEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftRegisterSkinEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftRevokeSessionQueryEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftVoiceChangeEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebSocketOpenEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebViewChannelEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebViewMessageEvent;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;
import net.lax1dude.eaglercraft.backend.server.api.query.IQueryConnection;
import net.lax1dude.eaglercraft.backend.server.api.voice.EnumVoiceState;

final class StandaloneEventDispatchAdapter implements IEventDispatchAdapter<StandalonePlayer, String> {
	private IEaglerXServerAPI<StandalonePlayer> api;
	public void setAPI(IEaglerXServerAPI<StandalonePlayer> api) { this.api = api; }
	@SuppressWarnings("unchecked")
	private static <T> void complete(IEventDispatchCallback<T> callback, Class<?> type) {
		if (callback != null) callback.complete((T) (type == null ? null : proxy(type)), null);
	}
	private static Object proxy(Class<?> type) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, method, args) -> {
			if (method.getName().equals("isCancelled")) return false;
			if (method.getName().equals("getAuthRequired")) return IEaglercraftAuthCheckRequiredEvent.EnumAuthResponse.SKIP;
			if (method.getReturnType() == String.class) return null;
			if (method.getReturnType() == UUID.class) return null;
			if (method.getReturnType() == boolean.class) return false;
			if (method.getReturnType() == byte.class) return (byte) 0;
			if (method.getReturnType() == int.class) return 0;
			return null;
		});
	}
	public void dispatchAuthCheckRequired(IEaglerPendingConnection p, boolean c, byte[] u, IEventDispatchCallback<IEaglercraftAuthCheckRequiredEvent<StandalonePlayer, String>> cb) { complete(cb, IEaglercraftAuthCheckRequiredEvent.class); }
	public void dispatchAuthCookieEvent(IEaglerLoginConnection p, byte[] a, boolean n, boolean c, byte[] d, String r, String u, UUID i, byte t, String m, String s, IEventDispatchCallback<IEaglercraftAuthCookieEvent<StandalonePlayer, String>> cb) { complete(cb, IEaglercraftAuthCookieEvent.class); }
	public void dispatchAuthPasswordEvent(IEaglerLoginConnection p, byte[] a, boolean n, byte[] s, byte[] d, boolean c, byte[] k, String r, String u, UUID i, byte t, String m, String q, IEventDispatchCallback<IEaglercraftAuthPasswordEvent<StandalonePlayer, String>> cb) { complete(cb, IEaglercraftAuthPasswordEvent.class); }
	public void dispatchClientBrandEvent(IEaglerPendingConnection p, IEventDispatchCallback<IEaglercraftClientBrandEvent<StandalonePlayer, String>> cb) { complete(cb, IEaglercraftClientBrandEvent.class); }
	public void dispatchLoginEvent(IEaglerLoginConnection p, boolean r, String s, IEventDispatchCallback<IEaglercraftLoginEvent<StandalonePlayer, String>> cb) { complete(cb, IEaglercraftLoginEvent.class); }
	public void dispatchInitializePlayerEvent(IEaglerPlayer<StandalonePlayer> p, Map<String, byte[]> d, IEventDispatchCallback<IEaglercraftInitializePlayerEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftInitializePlayerEvent.class); }
	public void dispatchDestroyPlayerEvent(IEaglerPlayer<StandalonePlayer> p, IEventDispatchCallback<IEaglercraftDestroyPlayerEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftDestroyPlayerEvent.class); }
	public void dispatchMOTDEvent(IMOTDConnection c, IEventDispatchCallback<IEaglercraftMOTDEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftMOTDEvent.class); }
	public void dispatchRegisterSkinEvent(IEaglerLoginConnection p, IRegisterSkinDelegate d, IEventDispatchCallback<IEaglercraftRegisterSkinEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftRegisterSkinEvent.class); }
	public void dispatchRevokeSessionQueryEvent(IQueryConnection q, byte[] d, IEventDispatchCallback<IEaglercraftRevokeSessionQueryEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftRevokeSessionQueryEvent.class); }
	public void dispatchVoiceChangeEvent(IEaglerPlayer<StandalonePlayer> p, EnumVoiceState a, EnumVoiceState b, IEventDispatchCallback<IEaglercraftVoiceChangeEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftVoiceChangeEvent.class); }
	public void dispatchWebSocketOpenEvent(IEaglerConnection c, FullHttpRequest r, IEventDispatchCallback<IEaglercraftWebSocketOpenEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftWebSocketOpenEvent.class); }
	public void dispatchWebViewChannelEvent(IEaglerPlayer<StandalonePlayer> p, IEaglercraftWebViewChannelEvent.EnumEventType t, String c, IEventDispatchCallback<IEaglercraftWebViewChannelEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftWebViewChannelEvent.class); }
	public void dispatchWebViewMessageEvent(IEaglerPlayer<StandalonePlayer> p, String c, IEaglercraftWebViewMessageEvent.EnumMessageType t, byte[] d, IEventDispatchCallback<IEaglercraftWebViewMessageEvent<StandalonePlayer>> cb) { complete(cb, IEaglercraftWebViewMessageEvent.class); }
}