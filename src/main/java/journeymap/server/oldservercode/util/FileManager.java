/*
 * JourneyMap Mod <journeymap.info> for Minecraft
 * Copyright (c) 2011-2017  Techbrew Interactive, LLC <techbrew.net>.  All Rights Reserved.
 */

package journeymap.server.oldservercode.util;

import journeymap.common.Journeymap;

import java.io.*;

/**
 * Created by Mysticdrew on 10/9/2014.
 */
public class FileManager
{

    public static String readFile(File file)
    {
        BufferedReader bReader;
        FileInputStream fileIn;
        //File file = new File(path, fileName);
        String row = "";
        StringBuilder fileOutput = new StringBuilder();
        try
        {
            fileIn = new FileInputStream(file);
            bReader = new BufferedReader(new InputStreamReader(fileIn));
            try
            {
                while ((row = bReader.readLine()) != null)
                {
                    fileOutput.append(row);
                }
                bReader.close();
            }
            catch (IOException e)
            {
                Journeymap.getLogger().error("Unable to read the JsonFile");
                Journeymap.getLogger().error("Error" + e);
                return null;
            }

            return fileOutput.toString();
        }
        catch (FileNotFoundException e)
        {
            Journeymap.getLogger().info(file + " not found!");
            return null;
        }
    }

    public static boolean writeFile(File file, String text)
    {
        try
        {
            File dir = new File(file.getParent());

            if (!dir.exists() && !dir.isDirectory())
            {
                dir.mkdirs();
                File newConfig = new File(dir, file.getName());
                newConfig.createNewFile();
            }


            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(text);
            bw.close();
            return true;
        }
        catch (IOException e)
        {
            Journeymap.getLogger().error("Error creating file " + file);
            Journeymap.getLogger().error("Error " + e);
            return false;
        }
    }
}
