package com.gam0zing.stage_enchantment.utils;

import com.gam0zing.stage_enchantment.generator.MixinRefmapGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author xWode
 * @version 1.0
 */

public class ZipReader {
    private static final String zipFilePath = "";
    private static final String[] targetCsvFile = {"methods.csv","fields.csv"};

    /**
     * 返回查找到的所有srg名
     * @param name 开发名
     * @param flag 0为方法，1为字段
     * @return 查找到的srg名的list
     */
    public static ArrayList<String> findSrg(String name,byte flag) {
        ArrayList<String> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 判断是否是目标 CSV 文件
                if (entry.getName().equals(targetCsvFile[flag])) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 简单以逗号分隔 CSV
                            String[] values = line.split(",");
                            if (values[1].equals(name)) {
                                list.add(values[0]);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    public static void findSrg(MixinRefmapGenerator.Refmap[] refmaps) {
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // 判断是否是目标 CSV 文件
                if (entry.getName().equals(targetCsvFile[refmaps[0].getIndex()])) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 简单以逗号分隔 CSV
                            String[] values = line.split(",");
                            for (MixinRefmapGenerator.Refmap refmap : refmaps) {
                                if (values[1].equals(refmap.name)) {
                                    refmap.optionsSrg.add(values[0]);
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
