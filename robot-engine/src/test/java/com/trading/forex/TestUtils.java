package com.trading.forex;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    private static void listDirectory(String dirPath, int level, List<File> fileList) {
        File dir = new File(dirPath);
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                if (aFile.isDirectory()) {
                    listDirectory(aFile.getAbsolutePath(), level + 1,fileList);
                } else {
                    fileList.add(aFile);
                }
            }
        }
    }

    public static List<File> listDirectory(String dirPath, int level) {
        final List<File> fles = new ArrayList();
        listDirectory(dirPath, level, fles);
        return fles;

    }
}
