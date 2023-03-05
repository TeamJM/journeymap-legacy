/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.util;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Mysticdrew on 11/20/2014.
 */
public class TabCompletionHelper
{

    public static List<String> getListOfStringsMatchingLastWord(String[] args, String... possibleCommands)
    {
        String lastWord = args[args.length - 1];

        List<String> worldList = new ArrayList<String>();

        String[] astring1 = possibleCommands;

        int i = possibleCommands.length;

        for (int j = 0; j < i; ++j)
        {
            String currentWord = astring1[j];
            if (doesStringStartWith(lastWord, currentWord))
            {
                worldList.add(currentWord);
            }
        }

        return worldList;
    }

    private static boolean doesStringStartWith(String lastWord, String currentWord)
    {
        return currentWord.regionMatches(true, 0, lastWord, 0, lastWord.length());
    }
}

