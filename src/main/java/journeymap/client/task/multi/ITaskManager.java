/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.task.multi;


import net.minecraft.client.Minecraft;

public interface ITaskManager
{

    public Class<? extends ITask> getTaskClass();

    public boolean enableTask(Minecraft minecraft, Object params);

    public boolean isEnabled(Minecraft minecraft);

    public ITask getTask(Minecraft minecraft);

    public void taskAccepted(ITask task, boolean accepted);

    public void disableTask(Minecraft minecraft);

}
