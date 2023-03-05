/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.common.version;

import com.google.common.base.Joiner;
import journeymap.common.Journeymap;
import org.apache.logging.log4j.core.helpers.Strings;

import java.util.Arrays;

/**
 * Object model for representing a software version:
 * MAJOR.MINOR.MICRO.PATCH, where the first three are numbers and patch may be a string. (a1, b2, rc1)
 */
public class Version implements Comparable<Version>
{
    public final int major;
    public final int minor;
    public final int micro;
    public final String patch;

    /**
     * Constructor without a patch modifier.
     *
     * @param major
     * @param minor
     * @param micro
     */
    public Version(int major, int minor, int micro)
    {
        this(major, minor, micro, "");
    }

    /**
     * Constructor with a patch modifier.
     *
     * @param major
     * @param minor
     * @param micro
     * @param patch
     */
    public Version(int major, int minor, int micro, String patch)
    {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.patch = patch != null ? patch : "";
    }

    /**
     * Creates a Version using parameters and a default Version if the parameters can't be parsed.
     *
     * @param major
     * @param minor
     * @param micro
     * @param patch
     * @param defaultVersion
     * @return
     */
    public static Version from(String major, String minor, String micro, String patch, Version defaultVersion)
    {
        try
        {
            return new Version(parseInt(major), parseInt(minor), parseInt(micro), patch);
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn(String.format("Version had problems when parsed: %s, %s, %s, %s", major, minor, micro, patch));
            if (defaultVersion == null)
            {
                defaultVersion = new Version(0, 0, 0);
            }
            return defaultVersion;
        }
    }

    /**
     * Converts a version string to a Version.
     *
     * @param versionString
     * @param defaultVersion
     * @return
     */
    public static Version from(String versionString, Version defaultVersion)
    {
        try
        {
            String[] strings = versionString.split("(?<=\\d)(?=\\p{L})");
            String[] majorMinorMicro = strings[0].split("\\.");
            String patch = strings.length == 2 ? strings[1] : "";
            if (majorMinorMicro.length < 3)
            {
                majorMinorMicro = Arrays.copyOf(strings, 3);
            }
            return Version.from(majorMinorMicro[0], majorMinorMicro[1], majorMinorMicro[2], patch, defaultVersion);
        }
        catch (Exception e)
        {
            Journeymap.getLogger().warn(String.format("Version had problems when parsed: %s", versionString));
            if (defaultVersion == null)
            {
                defaultVersion = new Version(0, 0, 0);
            }
            return defaultVersion;
        }
    }

    /**
     * Converts a numeric string to an int.
     *
     * @param number
     * @return
     */
    private static int parseInt(String number)
    {
        if (number == null)
        {
            return 0;
        }
        return Integer.parseInt(number);
    }

    /**
     * Creates a MAJOR.MINOR string from this instance.
     *
     * @return
     */
    public String toMajorMinorString()
    {
        return Joiner.on(".").join(major, minor);
    }

    /**
     * Whether this is a newer version than the other.
     *
     * @param other
     * @return
     */
    public boolean isNewerThan(Version other)
    {
        return compareTo(other) > 0;
    }

    /**
     * Whether this version is a release (no patch).
     *
     * @return
     */
    public boolean isRelease()
    {
        return Strings.isEmpty(patch);
    }


    @Override
    public String toString()
    {
        return Joiner.on(".").join(major, minor, micro + patch);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Version version = (Version) o;

        if (major != version.major)
        {
            return false;
        }
        if (micro != version.micro)
        {
            return false;
        }
        if (minor != version.minor)
        {
            return false;
        }
        if (!patch.equals(version.patch))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + micro;
        result = 31 * result + patch.hashCode();
        return result;
    }

    @Override
    public int compareTo(Version other)
    {
        int result = Integer.compare(major, other.major);
        if (result == 0)
        {
            result = Integer.compare(minor, other.minor);
        }
        if (result == 0)
        {
            result = Integer.compare(micro, other.micro);
        }
        if (result == 0)
        {
            result = patch.compareToIgnoreCase(other.patch);
            if (result != 0)
            {
                if (patch.equals(""))
                {
                    result = 1;
                }
                if (other.patch.equals(""))
                {
                    result = -1;
                }
            }
        }
        return result;
    }
}
