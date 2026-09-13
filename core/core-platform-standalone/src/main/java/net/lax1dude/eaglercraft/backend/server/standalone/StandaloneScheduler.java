package net.lax1dude.eaglercraft.backend.server.standalone;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.lax1dude.eaglercraft.backend.server.adapter.AbstractScheduler;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformTask;

final class StandaloneScheduler extends AbstractScheduler {

	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

	private IPlatformTask schedule(Runnable runnable, long delay) {
		ScheduledFuture<?> future = executor.schedule(runnable, Math.max(0L, delay), TimeUnit.MILLISECONDS);
		return new IPlatformTask() {
			public Runnable getTask() { return runnable; }
			public void cancel() { future.cancel(false); }
		};
	}

	public void execute(Runnable runnable) { schedule(runnable, 0L); }
	public void executeAsync(Runnable runnable) { schedule(runnable, 0L); }
	public void executeDelayed(Runnable runnable, long delay) { schedule(runnable, delay); }
	public void executeAsyncDelayed(Runnable runnable, long delay) { schedule(runnable, delay); }
	public IPlatformTask executeDelayedTask(Runnable runnable, long delay) { return schedule(runnable, delay); }
	public IPlatformTask executeAsyncDelayedTask(Runnable runnable, long delay) { return schedule(runnable, delay); }

	public void shutdown() { executor.shutdownNow(); }
}