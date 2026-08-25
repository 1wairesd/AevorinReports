package dev.aevorinstudios.aevorinReports.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SchedulerUtils {

    private static boolean isFolia = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    /**
     * Schedules a task to run on the entity's current region.
     * If not on Folia, falls back to the main Bukkit scheduler.
     */
    public static void runTask(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null);
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback catch-all, though mostly shouldn't happen if isFolia is true
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedules a task to run on the entity's current region after a delay.
     * If not on Folia, falls back to the main Bukkit scheduler.
     */
    public static void runTaskLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (isFolia) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Method runDelayedMethod = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                runDelayedMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null, delayTicks);
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Schedules a task to run on the global region.
     * Use this for tasks that don't involve a specific world or entity, or are truly global.
     */
    public static void runGlobalTask(Plugin plugin, Runnable task) {
        if (isFolia) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = globalScheduler.getClass().getMethod("run", Plugin.class, Consumer.class);
                runMethod.invoke(globalScheduler, plugin, (Consumer<Object>) t -> task.run());
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Schedules a one-off asynchronous task (e.g. database or network I/O).
     */
    public static void runTaskAsynchronously(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Schedules an async task.
     * Note: Bukkit's async scheduler works on Folia effectively the same for general purpose async tasks.
     * However, Folia has an AsyncScheduler, but Bukkit's is also supported.
     */
    public static void runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        if (isFolia) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runMethod = asyncScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                // Convert ticks to milliseconds (50ms per tick)
                runMethod.invoke(asyncScheduler, plugin, (Consumer<Object>) t -> task.run(), delay * 50, period * 50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Bukkit's async scheduler is compatible
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        }
    }
    
    /**
     * Schedules a delayed async task.
     */
    public static void runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        if (isFolia) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runMethod = asyncScheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
                // Convert ticks to milliseconds (50ms per tick)
                runMethod.invoke(asyncScheduler, plugin, (Consumer<Object>) t -> task.run(), delay * 50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }
    }
}
