/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.feature.impl;

import journeymap.client.feature.FeatureManager;
import journeymap.client.feature.Policy;

import java.util.Collections;
import java.util.Set;

/**
 * Unlimited features.
 *
 * @author techbrew
 */
public class Unlimited implements FeatureManager.PolicySet
{

    private final Set<Policy> policies;
    private final String name = "Unlimited";

    public Unlimited()
    {
        policies = Collections.unmodifiableSet(Policy.bulkCreate(true, true));
    }

    @Override
    public Set<Policy> getPolicies()
    {
        return policies;
    }

    @Override
    public String getName()
    {
        return name;
    }

}
