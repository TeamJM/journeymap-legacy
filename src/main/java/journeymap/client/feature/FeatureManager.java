/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.client.feature;

import com.google.common.reflect.ClassPath;
import journeymap.common.Journeymap;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Governs what features are available at runtime.
 */
public class FeatureManager
{
    private static final String NAME_FAIRPLAY = "FairPlay";
    private static final String IMPL_PACKAGE = "journeymap.client.feature.impl";
    private static final String CLASS_UNLIMITED = String.format("%s.Unlimited", IMPL_PACKAGE);
    private final PolicySet policySet;
    private final HashMap<Feature, Policy> policyMap = new HashMap<Feature, Policy>();
    private final HashMap<String, EnumSet<Feature>> disableControlCodes = new HashMap<String, EnumSet<Feature>>();
    private Boolean controlCodeAltered = null;

    /**
     * Private constructure.  Use instance()
     */
    private FeatureManager()
    {
        disableControlCodes.put("\u00a73 \u00a76 \u00a73 \u00a76 \u00a73 \u00a76 \u00a7e", Feature.radar());
        disableControlCodes.put("\u00a73\u00a76\u00a73\u00a76\u00a73\u00a76\u00a7e", Feature.radar());
        disableControlCodes.put("\u00a73 \u00a76 \u00a73 \u00a76 \u00a73 \u00a76 \u00a7d", EnumSet.of(Feature.MapCaves));
        disableControlCodes.put("\u00a73\u00a76\u00a73\u00a76\u00a73\u00a76\u00a7d", EnumSet.of(Feature.MapCaves));
        policySet = locatePolicySet();
        reset();
    }

    /**
     * Gets a detailed description of all policies.
     */
    public static String getPolicyDetails()
    {
        StringBuilder sb = new StringBuilder(String.format("%s Features: ", getPolicySetName()));
        for (Feature feature : Feature.values())
        {
            boolean single = false;
            boolean multi = false;
            if (Holder.INSTANCE.policyMap.containsKey(feature))
            {
                single = Holder.INSTANCE.policyMap.get(feature).allowInSingleplayer;
                multi = Holder.INSTANCE.policyMap.get(feature).allowInMultiplayer;
            }

            sb.append(String.format("\n\t%s : singleplayer = %s , multiplayer = %s", feature.name(), single, multi));
        }
        return sb.toString();
    }

    /**
     * Gets the singleton.
     */
    public static FeatureManager instance()
    {
        return Holder.INSTANCE;
    }

    /**
     * Whether the specified feature is currently permitted.
     *
     * @param feature the feature to check
     * @return true if permitted
     */
    public static boolean isAllowed(Feature feature)
    {
        Policy policy = Holder.INSTANCE.policyMap.get(feature);
        return (policy != null) && policy.isCurrentlyAllowed();
    }

    /**
     * Returns a map of all features and whether they are currently permitted.
     *
     * @return
     */
    public static Map<Feature, Boolean> getAllowedFeatures()
    {
        Map<Feature, Boolean> map = new HashMap<Feature, Boolean>(Feature.values().length * 2);
        for (Feature feature : Feature.values())
        {
            map.put(feature, isAllowed(feature));
        }
        return map;
    }

    /**
     * Gets the name of the PolicySet.
     *
     * @return
     */
    public static String getPolicySetName()
    {
        return instance().policySet.getName();
    }

    public Set<String> getControlCodes()
    {
        return disableControlCodes.keySet();
    }

    public void handleControlCode(String controlCode)
    {
        if (disableControlCodes.containsKey(controlCode))
        {
            controlCodeAltered = true;
            for (Feature feature : disableControlCodes.get(controlCode))
            {
                Journeymap.getLogger().info("Feature disabled in multiplayer via control code: " + feature);
                Holder.INSTANCE.policyMap.put(feature, new Policy(feature, true, false));
            }

        }
    }

    /**
     * Restores FeatureSet if a control code has altered the policy map
     */
    public void reset()
    {
        synchronized (policySet)
        {
            if (controlCodeAltered == null || controlCodeAltered)
            {
                for (Policy policy : policySet.getPolicies())
                {
                    policyMap.put(policy.feature, policy);
                }
                if (controlCodeAltered != null)
                {
                    Journeymap.getLogger().info("Returning to default " + getPolicyDetails());
                }
                controlCodeAltered = false;
            }
        }
    }

    /**
     * Finds the FeatureSet via reflection.
     *
     * @return
     */
    private PolicySet locatePolicySet()
    {
        PolicySet fs = null;
        try
        {
            ClassPath cp = ClassPath.from(getClass().getClassLoader());
            Set<ClassPath.ClassInfo> classInfos = cp.getTopLevelClasses(IMPL_PACKAGE);
            if (classInfos.size() > 1)
            {
                try
                {
                    Class fsClass = Class.forName(CLASS_UNLIMITED);
                    fs = (PolicySet) fsClass.newInstance();
                }
                catch (Throwable e)
                {
                }
            }

            if (fs == null)
            {
                for (ClassPath.ClassInfo classInfo : classInfos)
                {
                    Class aClass = classInfo.load();
                    if (PolicySet.class.isAssignableFrom(aClass))
                    {
                        fs = (PolicySet) aClass.newInstance();
                        break;
                    }
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }

        return (fs != null) ? fs : createFairPlay();
    }

    /**
     * Generates a FeatureSet that disables all features in multiplayer.
     *
     * @return
     */
    private PolicySet createFairPlay()
    {
        return new PolicySet()
        {
            // All features allowed in singleplayer, but none in multiplayer
            private final Set<Policy> policies = Policy.bulkCreate(true, false);
            private final String name = NAME_FAIRPLAY;

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

        };
    }

    /**
     * Interface for a named set of Policies.
     */
    public static interface PolicySet
    {
        public Set<Policy> getPolicies();

        public String getName();
    }

    /**
     * Instance holder.
     */
    private static class Holder
    {
        private static final FeatureManager INSTANCE = new FeatureManager();
    }

}
