/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;

import journeymap.client.forge.helper.ForgeHelper;
import journeymap.client.log.StatTimer;
import journeymap.client.thread.RunnableTask;
import journeymap.common.Journeymap;
import journeymap.common.thread.JMThreadFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class TaskController
{
    final static Logger logger = Journeymap.getLogger();
    final ArrayBlockingQueue<Future> queue = new ArrayBlockingQueue<Future>(1);
    final List<ITaskManager> managers = new LinkedList<ITaskManager>();
    final Minecraft minecraft = ForgeHelper.INSTANCE.getClient();
    final ReentrantLock lock = new ReentrantLock();

    // Executor for task threads
    private volatile ScheduledExecutorService taskExecutor;

    public TaskController()
    {
        managers.add(new MapRegionTask.Manager());
        managers.add(new SaveMapTask.Manager());
        managers.add(new MapPlayerTask.Manager());
        managers.add(new InitColorManagerTask.Manager());
    }

    private void ensureExecutor()
    {
        if (taskExecutor == null || taskExecutor.isShutdown())
        {
            taskExecutor = Executors.newScheduledThreadPool(1, new JMThreadFactory("task")); //$NON-NLS-1$
            queue.clear();
        }
    }

    public Boolean isMapping()
    {
        return taskExecutor != null && !taskExecutor.isShutdown();
    }

    public void enableTasks()
    {
        queue.clear();
        ensureExecutor();

        List<ITaskManager> list = new LinkedList<ITaskManager>(managers);
        for (ITaskManager manager : managers)
        {
            boolean enabled = manager.enableTask(minecraft, null);
            if (!enabled)
            {
                logger.debug("Task not initially enabled: " + manager.getTaskClass().getSimpleName());
            }
            else
            {
                logger.debug("Task ready: " + manager.getTaskClass().getSimpleName());
            }
        }

    }

    public void clear()
    {
        managers.clear();
        queue.clear();

        if (taskExecutor != null && !taskExecutor.isShutdown())
        {
            taskExecutor.shutdownNow();
            try
            {
                taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            taskExecutor = null;
        }
    }

    private ITaskManager getManager(Class<? extends ITaskManager> managerClass)
    {
        ITaskManager taskManager = null;
        for (ITaskManager manager : managers)
        {
            if (manager.getClass() == managerClass)
            {
                taskManager = manager;
                break;
            }
        }
        return taskManager;
    }

    public boolean isTaskManagerEnabled(Class<? extends ITaskManager> managerClass)
    {
        ITaskManager taskManager = getManager(managerClass);
        if (taskManager != null)
        {
            return taskManager.isEnabled(ForgeHelper.INSTANCE.getClient());
        }
        else
        {
            logger.warn("Couldn't toggle task; manager not in controller: " + managerClass.getClass().getName());
            return false;
        }
    }

    public void toggleTask(Class<? extends ITaskManager> managerClass, boolean enable, Object params)
    {

        ITaskManager taskManager = null;
        for (ITaskManager manager : managers)
        {
            if (manager.getClass() == managerClass)
            {
                taskManager = manager;
                break;
            }
        }
        if (taskManager != null)
        {
            toggleTask(taskManager, enable, params);
        }
        else
        {
            logger.warn("Couldn't toggle task; manager not in controller: " + managerClass.getClass().getName());
        }
    }

    private void toggleTask(ITaskManager manager, boolean enable, Object params)
    {
        Minecraft minecraft = ForgeHelper.INSTANCE.getClient();
        if (manager.isEnabled(minecraft))
        {
            if (!enable)
            {
                logger.debug("Disabling task: " + manager.getTaskClass().getSimpleName());
                manager.disableTask(minecraft);
            }
            else
            {
                logger.debug("Task already enabled: " + manager.getTaskClass().getSimpleName());
            }
        }
        else
        {
            if (enable)
            {
                logger.debug("Enabling task: " + manager.getTaskClass().getSimpleName());
                manager.enableTask(minecraft, params);
            }
            else
            {
                logger.debug("Task already disabled: " + manager.getTaskClass().getSimpleName());
            }
        }
    }

    public void disableTasks()
    {
        for (ITaskManager manager : managers)
        {
            if (manager.isEnabled(minecraft))
            {
                manager.disableTask(minecraft);
                logger.debug("Task disabled: " + manager.getTaskClass().getSimpleName());
            }
        }
    }

    public boolean hasRunningTask()
    {
        return !queue.isEmpty();
    }

    public void performTasks()
    {
        Profiler profiler = ForgeHelper.INSTANCE.getClient().mcProfiler;
        profiler.startSection("journeymapTask");
        StatTimer totalTimer = StatTimer.get("TaskController.performMultithreadTasks", 1, 500).start();

        try
        {
            if (lock.tryLock())
            {
                if (!queue.isEmpty())
                {
                    if (queue.peek().isDone())
                    {
                        try
                        {
                            queue.take();
                        }
                        catch (InterruptedException e)
                        {
                            logger.warn(e.getMessage());
                        }
                    }
                }

                if (queue.isEmpty())
                {
                    ITask task = null;
                    ITaskManager manager = getNextManager(minecraft);
                    if (manager == null)
                    {
                        logger.warn("No task managers enabled!");
                        return;
                    }
                    boolean accepted = false;

                    StatTimer timer = StatTimer.get(manager.getTaskClass().getSimpleName() + ".Manager.getTask").start();
                    task = manager.getTask(minecraft);

                    if (task == null)
                    {
                        timer.cancel();
                    }
                    else
                    {
                        timer.stop();

                        ensureExecutor();

                        if (taskExecutor != null && !taskExecutor.isShutdown())
                        {
                            // Create the runnable wrapper
                            final RunnableTask runnableTask = new RunnableTask(taskExecutor, task);
                            queue.add(taskExecutor.submit(runnableTask));
                            accepted = true;

                            if (logger.isTraceEnabled())
                            {
                                logger.debug("Scheduled " + manager.getTaskClass().getSimpleName());
                            }
                        }
                        else
                        {
                            logger.warn("TaskExecutor isn't running");
                        }

                        manager.taskAccepted(task, accepted);
                    }
                }
                lock.unlock();
            }
            else
            {
                logger.warn("TaskController appears to have multiple threads trying to use it");
            }
        }
        finally
        {
            totalTimer.stop();
            profiler.endSection();
        }
    }

    private ITaskManager getNextManager(final Minecraft minecraft)
    {
        for (ITaskManager manager : managers)
        {
            if (manager.isEnabled(minecraft))
            {
                return manager;
            }
        }
        return null;
    }
}
