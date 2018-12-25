package com.quakoo.baseFramework.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author LiYongbiao1
 *
 */
public class VersionUtil {

    /**
     * 大于或者等于该版本 返回true
     * 
     * @param version
     * @param standardVersion
     * @return
     */
    public static boolean isValidVesion(String version, String standardVersion) {
        if (standardVersion.equals(version)) {
            return true;
        }
        if (StringUtils.isBlank(version)) {
            return false;
        }

        String[] versions = version.split("\\.");
        String[] serVersions = standardVersion.split("\\.");
        if (versions.length < serVersions.length) {
            return false;
        }
        return isValidVesion(serVersions, versions);
    }

    private static boolean isValidVesion(String[] serVersions, String[] versions) {

        for (int i = 0; i < serVersions.length; i++) {
            if (Integer.parseInt(versions[i]) < Integer.parseInt(serVersions[i])) {
                return false;
            }
            if (Integer.parseInt(versions[i]) > Integer.parseInt(serVersions[i])) {
                return true;
            }
        }
        return true;

    }

    public static void main(String[] sadf) {
        // 0.1.2.3
        String standardVersion = "1.2.1";
        System.out.println(isValidVesion("1.2.1", standardVersion));// true

    }
}
