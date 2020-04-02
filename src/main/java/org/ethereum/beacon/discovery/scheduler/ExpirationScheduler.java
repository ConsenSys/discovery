/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules `runnable` in delay which is set by constructor. When runnable is renewed by putting it
 * in map again, old task is cancelled and removed. Task are equalled by the <Key>
 */
public class ExpirationScheduler<Key> {
  private final ScheduledExecutorService scheduler;
  private final long delay;
  private final TimeUnit timeUnit;

  @SuppressWarnings({"rawtypes"})
  private Map<Key, ScheduledFuture> expirationTasks = new ConcurrentHashMap<>();

  ExpirationScheduler(long delay, TimeUnit timeUnit, final ScheduledExecutorService scheduler) {
    this.delay = delay;
    this.timeUnit = timeUnit;
    this.scheduler = scheduler;
  }

  /**
   * Puts scheduled task and renews (cancelling old) timeout for the task associated with the key
   *
   * @param key Task key
   * @param runnable Task
   */
  @SuppressWarnings({"rawtypes"})
  public void put(Key key, Runnable runnable) {
    cancel(key);
    ScheduledFuture future =
        scheduler.schedule(
            () -> {
              runnable.run();
              expirationTasks.remove(key);
            },
            delay,
            timeUnit);
    expirationTasks.put(key, future);
  }

  /** Cancels task for key and removes it from storage */
  public void cancel(Key key) {
    synchronized (this) {
      if (expirationTasks.containsKey(key)) {
        expirationTasks.remove(key).cancel(true);
      }
    }
  }
}
