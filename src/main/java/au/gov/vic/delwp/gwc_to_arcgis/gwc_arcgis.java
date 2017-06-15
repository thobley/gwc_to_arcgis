/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.gov.vic.delwp.gwc_to_arcgis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author th61
 */
public class gwc_arcgis {

    private static Logger log;
    private static boolean slippy = false;

    public static void main(String[] args) {
        Options ops = new Options();
        Option gwccacheOp = new Option("gwc", "gwc-cache", true, "GeoWebCache Fromatted Tile Cache Location");
        Option acrgiscacheOp = new Option("arcgis", "arcgis-cache", true, "ArcGIS Fromatted Tile Cache Output Location");
        Option formatOp = new Option("f", "format", true, "File format to look for (jpg, png8, png)");
        Option projOp = new Option("p", "proj", true, "Cache projection 3857 or 3111");
        Option pxOp = new Option("px", "pixel-width", true, "Image pixel width in the cache");
        Option minLvlOp = new Option("min", "min-level", true, "Minimun Level to convert from");
        Option maxLvlOp = new Option("max", "max-level", true, "Maximun level to convert to");
        Option logOp = new Option("lc", "log-check", false, "Check previous log file to see if the folder has already been copied");
       // Option slippyOp = new Option("sm", "slippy", false, "Output the cache as SLIPPY MAP format, not ArcGIS");

        //Option acrgiscacheOp = new Option("arcgis", "arcgis-cache", true, "ArcGIS Fromatted Tile Cache Output Location");
        gwccacheOp.setRequired(true);
        acrgiscacheOp.setRequired(true);
        formatOp.setRequired(true);
        projOp.setRequired(true);
        pxOp.setRequired(true);
        minLvlOp.setRequired(false);
        maxLvlOp.setRequired(false);
        logOp.setRequired(false);
        //slippyOp.setRequired(false);
        ops.addOption(gwccacheOp)
                .addOption(acrgiscacheOp)
                .addOption(formatOp)
                .addOption(projOp)
                .addOption(pxOp)
                .addOption(minLvlOp)
                .addOption(maxLvlOp)
 //               .addOption(slippyOp)
                .addOption(logOp);
        CommandLineParser parser = new GnuParser(); // <-- Deprecated in commons-cli 1.3 !!!
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(ops, (args.length == 0 ? new String[0] : args));
        } catch (ParseException e) {
            System.out.println("missing cmd line arguments");
            System.out.println(e);
            formatter.printHelp("utility-name", ops);
            System.exit(1);
            return;
        }
        File thisJar = new File(gwc_arcgis.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        //System.out.println(thisJar.getParent());
        File logfileloc = new File(thisJar.getParent() + "\\log");
        File logfile = new File(thisJar.getParent() + "\\log\\gwc_to_arcgis.log");
        File old_logfile = new File(thisJar.getParent() + "\\log\\old_gwc_to_arcgis.log");
        //System.out.println(logfile.getPath());
        //System.out.println(old_logfile.getPath());
        try {
            if (logfile.exists()) {
                //File old_logfile = new File("./Log/old_gwc_to_arcgis.log");
                System.out.println("Copying old Logfile");
                FileUtils.copyFile(logfile, old_logfile);
                FileUtils.deleteQuietly(logfile);

                logfile.createNewFile();
            } else {
                if (!logfileloc.isDirectory()) {
                    logfileloc.mkdir();
                }
                logfile.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("UNBALE TO CREATE LOG FILE or MOVE logfile!");
            e.printStackTrace();
            System.exit(1);
        };

        System.setProperty("logfile.name", logfile.getPath());
        log = Logger.getLogger(gwc_arcgis.class);
        //long start = System.currentTimeMillis();
        String gwccache = cmd.getOptionValue("gwc-cache");
        String acrgiscache = cmd.getOptionValue("arcgis-cache");
        String format = cmd.getOptionValue("format");
        String proj = cmd.getOptionValue("proj");
        int px = Integer.parseInt(cmd.getOptionValue("pixel-width"));
        int maxLvl = (proj.equals("3111") ? 13 : 20);
        int minLvl = 0;
        maxLvl = cmd.hasOption("max-level") ? Integer.parseInt(cmd.getOptionValue("max-level")) : maxLvl;
        minLvl = cmd.hasOption("min-level") ? Integer.parseInt(cmd.getOptionValue("min-level")) : minLvl;
     //   slippy = cmd.hasOption("slippy");
        Boolean logchk = false;
        if (cmd.hasOption("log-check")) {
            if (!old_logfile.exists()) {
                System.out.println("There is no old log file");
                System.exit(1);
            }
            logchk = true;
        }

        System.out.println("Converting GeoWebCache to ArcGIS cache format");
        System.out.println("GWC location - " + gwccache);
        System.out.println("ArcGIS output - " + acrgiscache);
        System.out.println("GWC format - " + format);
        System.out.println("For projection - " + proj);
        System.out.println("Copy from level - " + minLvl);
        System.out.println("Copy to level - " + maxLvl);

        File levelsFolder = new File(acrgiscache + "\\_allLayers");
        levelsFolder.mkdir();
        String[] levelFolders = getLevelFolders(gwccache);
        for (String gwclevel : levelFolders) {
            int level = getArcgisLevelFolderName(gwclevel);
            if (!isInSameProjFolder(gwclevel, proj)) {
                continue;
            }
            if (gwclevel.contains(".")) {
                continue;
            }
            if (level > maxLvl || level < minLvl) {
                continue;
            };
            String foldername = "L" + String.format("%2s", level).replace(" ", "0");
            int totalRows = getTotalLevelRows(proj, level, px);
            //Create arcGIS level folder
            File lvFolder = new File(levelsFolder.getPath() + "\\" + foldername);
            lvFolder.mkdir();
            String[] folders = getFolders(gwccache + "\\" + gwclevel);
            System.out.println("Folders in " + gwclevel + ": " + folders.length);
            int folNum = 1;
            for (String folder : folders) {
                // If there was an error the -lc flag will chech the old log to see if this folder was already completed
                if (logchk && alreadyCopied(gwclevel, folder, old_logfile)) {

                    log.info("Skipping: " + folder);
                    System.out.println("Skipping: " + gwclevel + File.separator + folder);
                    folNum++;
                    continue;

                }

                ArrayList<String> files = getFiles(gwccache + "\\" + gwclevel + "\\" + folder, format);
                System.out.println("Copying " + folder + " - " + folNum + " of " + folders.length);
                //System.out.println("containing " + files.size() + " files");
                int progress = 1;
                for (String file : files) {
                    int col = Integer.parseInt(file.split("\\.")[0].split("_")[0]);
                    int row = Integer.parseInt(file.split("\\.")[0].split("_")[1]);
                    int rowInvert = (totalRows - 1) - row;
                    String colFile = convertToArcGISRowColumn(Integer.toHexString(col), "C") + "." + (format.equals("png8") ? "png" : format);
                    String rowFolder = convertToArcGISRowColumn(Integer.toHexString(rowInvert), "R");
                    if (!new File(lvFolder.getPath() + "\\" + rowFolder).exists()) {
                        new File(lvFolder.getPath() + "\\" + rowFolder).mkdir();
                    }

                    File dest = new File(lvFolder.getPath() + "\\" + rowFolder + "\\" + colFile);
                    File src = new File(gwccache + "\\" + gwclevel + "\\" + folder + "\\" + file);
                    try {

                        FileUtils.copyFile(src, dest);
                        //copyFile(src, dest);

                    } catch (IOException e) {
                        System.out.println("Failed to copy " + file);
                        e.printStackTrace();
                    }
                    printProgress(progress, files.size());
                    progress++;

                }
                System.out.println("[##########]DONE!  ");
                folNum++;
                log.info("Complted: " + gwclevel + "\\" + folder);
            }

        }

    }

    public static Boolean alreadyCopied(String gwclevel, String folder, File old_log) {
        Boolean cont = false;
        try {
            Scanner scanner = new Scanner(old_log);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(gwclevel + "\\" + folder)) {
                    cont = true;
                }
            }
        } catch (IOException e) {
            System.out.println("Cant open old logfile for checking");
            System.exit(1);
        }
        return cont;
    }

    public static String convertToArcGISRowColumn(String hex, String letter) {
        String rowString = letter + String.format("%8s", hex).replace(" ", "0");
        //System.out.println(rowString);
        return rowString;
    }

    public static Boolean isInSameProjFolder(String gwclevel, String proj) {
        return gwclevel.contains(proj);
    }

    public static int getArcgisLevelFolderName(String gwcLevelDir) {
        String[] gwcFolderA = gwcLevelDir.split("\\\\");
        String gwcFolder = gwcFolderA[gwcFolderA.length - 1];
        String[] gwcFolderAtts = gwcFolder.split("_");
        int level = Integer.parseInt(gwcFolderAtts[gwcFolderAtts.length - 1]);
        //String str = "L"+String.format("%2s", level).replace(" ", "0");

        return level;
    }

    public static String[] getLevelFolders(String cacheLoc) {
        File f = new File(cacheLoc);
        //long start = System.currentTimeMillis();
        String[] folders = null;
        //ArrayList<String> folders = new ArrayList<String>();
        try {
            folders = f.list();
        } catch (Exception e) {
            System.out.println("Folder List failed");
        }
        return folders;
    }

    public static String[] getFolders(String cacheLoc) {
        File f = new File(cacheLoc);
        //long start = System.currentTimeMillis();
        String[] folders = null;
        //ArrayList<String> folders = new ArrayList<String>();
        try {
            //System.out.println("Getting Folders " + f.getAbsolutePath());
            folders = f.list();
            //System.out.println("Got folders " + folders.length);
        } catch (SecurityException e) {
            System.out.println("Folder List failed");
            e.printStackTrace();
        }

        return folders;
    }

    public static ArrayList<String> getFiles(String folder, String format) {
        File f = new File(folder);
        ArrayList<String> files = new ArrayList<String>();
        try {

            for (String file : f.list()) {
                if (format.equals(file.split("\\.")[1])) {
                    files.add(file);
                }
            }

        } catch (Exception e) {
            System.out.println("File List failed");
        }
        return files;
    }

    public static int getTotalLevelRows(String proj, int level, int px) {
        double res = getResolution(level, proj);
        double[] vg = {1786000, 1997264.499195665, 2869735.500804335, 3081000};
        double[] wm = {-20037508.34, -20037508.34, 20037508.34, 20037508.34};
        double maxrows = 0;
        double tileheight = px * res;
        if (proj.endsWith("3111") || proj.equals("3111")) {
            maxrows = (vg[3] - vg[1]) / tileheight;

        } else if (proj.endsWith("3857") || proj.equals("3857")) {
            maxrows = (wm[3] - wm[1]) / tileheight;
        } else {
            System.out.println(proj + " is not a valid projection, only 3111 and 3857 excepted");
            System.exit(1);
        }
        //System.out.println(maxrows);
        //System.out.println((int) Math.round(maxrows));
        return (int) Math.round(maxrows);
    }

    public static double getResolution(int level, String proj) {
        if (proj.endsWith("3111") || proj.equals("3111")) {
            switch (level) {
                case 0:
                    return 2116.670900008467;
                case 1:
                    return 1058.3354500042335;
                case 2:
                    return 529.1677250021168;
                case 3:
                    return 264.5838625010584;
                case 4:
                    return 132.2919312505292;
                case 5:
                    return 66.1459656252646;
                case 6:
                    return 26.458386250105836;
                case 7:
                    return 13.229193125052918;
                case 8:
                    return 6.614596562526459;
                case 9:
                    return 2.6458386250105836;
                case 10:
                    return 1.3229193125052918;
                case 11:
                    return 0.6614596562526459;
                case 12:
                    return 0.330729828126323;
                case 13:
                    return 0.2116670900008467;
                default:
                    return 0;
            }

        } else if (proj.endsWith("3857") || proj.equals("3857")) {
            switch (level) {
                case 0:
                    return 156543.0339062;
                case 1:
                    return 78271.516953125;
                case 2:
                    return 39135.7584765625;
                case 3:
                    return 19567.87923828125;
                case 4:
                    return 9783.939619140625;
                case 5:
                    return 4891.9698095703125;
                case 6:
                    return 2445.9849047851562;
                case 7:
                    return 1222.9924523925781;
                case 8:
                    return 611.4962261962891;
                case 9:
                    return 305.74811309814453;
                case 10:
                    return 152.87405654907226;
                case 11:
                    return 76.43702827453613;
                case 12:
                    return 38.218514137268066;
                case 13:
                    return 19.109257068634033;
                case 14:
                    return 9.554628534317017;
                case 15:
                    return 4.777314267158508;
                case 16:
                    return 2.388657133579254;
                case 17:
                    return 1.194328566789627;
                case 18:
                    return 0.5971642833948135;
                case 19:
                    return 0.2985821416974068;
                case 20:
                    return 0.1492910708487034;
                default:
                    return 0;
            }

        } else {
            System.out.println(proj + " is not a valid projection, only 3111 and 3857 excepted");
            System.exit(1);
        }
        return 0;
    }

    public static void printProgress(int progress, int total) {
        double fp = (((double) progress) / total) * 10;
//        if (progress == total) {
//            System.out.println("[  DONE!   ]\n");
//        } else {
        switch ((int) Math.round(fp)) {
            case 0:
                System.out.print("[          ]\r");
                break;
            case 1:
                System.out.print("[#         ]\r");
                break;
            case 2:
                System.out.print("[##        ]\r");
                break;
            case 3:
                System.out.print("[###       ]\r");
                break;
            case 4:
                System.out.print("[####      ]\r");
                break;
            case 5:
                System.out.print("[#####     ]\r");
                break;
            case 6:
                System.out.print("[######    ]\r");
                break;
            case 7:
                System.out.print("[#######   ]\r");
                break;
            case 8:
                System.out.print("[########  ]\r");
                break;
            case 9:
                System.out.print("[######### ]\r");
                break;
            case 10:
                System.out.print("[##########]\r");
                break;
        }
//        }
    }

}
