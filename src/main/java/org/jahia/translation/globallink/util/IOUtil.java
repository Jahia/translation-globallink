/*
 * /*
 *  * ==========================================================================================
 *  * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 *  * ==========================================================================================
 *  *
 *  *                                  http://www.jahia.com
 *  *
 *  * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 *  * ==========================================================================================
 *  *
 *  *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *  *
 *  *     This file is part of a Jahia's Enterprise Distribution.
 *  *
 *  *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *  *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *  *     the Jahia Sustainable Enterprise License (JSEL).
 *  *
 *  *     For questions regarding licensing, support, production usage...
 *  *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *  *
 *  * ==========================================================================================
 *  */
package org.jahia.translation.globallink.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;


/**
 * Utility Class for IO Operations
 *
 * @author Aashish.Kocchar, WebItUp.
 */
public class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    /**
     * Check a directory path and create if not exist
     *
     * @param path
     * @return
     */
    public static boolean createDirectories(Path path) {
        try {
            if (Files.exists(path)) {
                return true;
            }
            Files.createDirectories(path);
            return true;
        } catch (Exception ex) {
            LOGGER.error("Cannot create directory ", ex);
        }
        return false;
    }

    /**
     * Check a directory path and create if not exist
     *
     * @param path
     * @return
     */
    public static boolean createDirectories(String path) {
        try {
            if (Files.exists(FileSystems.getDefault().getPath(path))) {
                return true;
            }
            Files.createDirectories(FileSystems.getDefault().getPath(path));
            return true;
        } catch (Exception ex) {
            LOGGER.error("Cannot create directory ", ex);
        }
        return false;
    }

    /**
     * Create file in a given file path
     *
     * @param filePath
     * @return
     */
    public static boolean createFile(String filePath) {
        try {
            if (Files.exists(FileSystems.getDefault().getPath(filePath))) {
                return true;
            }
            Files.createFile(FileSystems.getDefault().getPath(filePath));
            return true;
        } catch (Exception ex) {
            LOGGER.error("Cannot create file ", ex);
            return false;
        }
    }

    /**
     * Create file in a location from input stream
     *
     * @param inputStream
     * @param path
     * @return
     */
    public static boolean createFile(InputStream inputStream, String path) {
        try {
            Path filePath = FileSystems.getDefault().getPath(path);
            if (!Files.exists(filePath)) {
                Files.createFile(FileSystems.getDefault().getPath(path));
            }
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ex) {
            LOGGER.error("Cannot create file ", ex);
        }
        return false;
    }

    /**
     * Get Files list under a directory
     *
     * @param path
     * @return
     */
    public static List<File> listFiles(String path) {
        try {
            Path directory = FileSystems.getDefault().getPath(path);
            return Arrays.asList(directory.toFile().listFiles());
        } catch (Exception ex) {
            LOGGER.error("Error while getting files list: ", ex);
        }
        return null;
    }

    /**
     * Get {@link File} from a given file path
     *
     * @param filePath
     * @return
     */
    public static File getFile(String filePath) {
        try {
            Path path = FileSystems.getDefault().getPath(filePath);
            if (Files.exists(path)) {
                return new File(filePath);
            }
        } catch (Exception ex) {
            LOGGER.error("Error while getting file: ", ex);
        }
        return null;
    }
}
