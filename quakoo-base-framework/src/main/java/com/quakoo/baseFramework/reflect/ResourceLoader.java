package com.quakoo.baseFramework.reflect;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class ResourceLoader {

    /**
     * The '.class' file extension as a constant (to prevent the instantiation
     * of the same String over and over).
     */
    final static private String CLASS_FILE_EXTENSION = ".class";

    /**
     * The package separator char as a constant (to prevent the instantiation of
     * the same String over and over).
     */
    final static char PACKAGE_SEPARATOR_CHAR = '.';

    /**
     * The '/' char as a constant (to prevent the instantiation of the same
     * String over and over).
     */
    final static char SLASH_CHAR = '/';

    /**
     * The '/' char as a String constant (to prevent the instantiation of the
     * same String over and over).
     */
    final static String SLASH_STRING = "/";

    /**
     * The "WEB-INF/classes" path as a String constant (to prevent the
     * instantiation of the same String over and over).
     */
    final static String WEB_INF_CLASSES = "WEB-INF/classes/";

    /** Enforce noninstantiability. */
    private ResourceLoader() {
    }

    /**
     * Returns all resources associated with a given package.
     *
     * @param packageName
     *            the package to fetch the resources for
     * @return all resources associated to the package
     * @throws ResourceException
     *             if class loader, package or resources can't be found
     */
    private static Enumeration<URL> getPackageResources(String packageName) throws ResourceException {

        Enumeration<URL> resources = null;

        try {
            /*
             * IMPLEMENTATION NOTE:
             *
             * Must use the Thread context class loader since inside Surefire
             * the system classLoader does not have all classpath entries
             */
            // Get class loader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            if (classLoader == null) {
                classLoader = ResourceLoader.class.getClassLoader();
            }

            if (classLoader == null) {
                throw new ResourceException("Could not get class loader!!");
            }

            // Generate path from package name
            String path = packageName.replace(PACKAGE_SEPARATOR_CHAR, SLASH_CHAR);

            // Get all resources for the path
            resources = classLoader.getResources(path);

        } catch (NullPointerException nullPointerException) {
            throw new ResourceException(packageName + " does not appear to be a valid package!", nullPointerException);

        } catch (IOException ioException) {
            // Unable to provide unit test since IOException is raised by Java's
            // own ClassLoader class
            throw new ResourceException("Could not get all resources for " + packageName, ioException);

        }

        return resources;
    }

    /**
     * Returns a list of class names from the given folders.
     *
     * @param packageName
     *            the package name to search for
     * @param folders
     *            the folder list to search
     * @return the list of loaded classes
     * @throws ResourceException
     *             if the package is invalid
     */
    private static List<String> getClassNamesInFolders(String packageName, List<File> folders) {

        // List of classes on the package
        List<String> classes = new ArrayList<String>();

        // For every folder identified capture all the .class files
        for (File folder : folders) {

            if (folder.exists()) {

                // Get the list of the files contained in the package
                String[] files = folder.list();

                for (String file : files) {

                    // Filter .class files
                    if (file.endsWith(CLASS_FILE_EXTENSION)) {
                        // removes the .class extension
                        String className = packageName + PACKAGE_SEPARATOR_CHAR
                                + file.substring(0, file.length() - CLASS_FILE_EXTENSION.length());

                        classes.add(className);
                    }
                }

            }
        }

        return classes;
    }

    /**
     * Returns a list of class names from the given Jar entries.
     *
     * @param packageName
     *            the package name where the directories where gathered
     * @param jars
     *            the jar entry list to search
     * @return a list of classes from the jar
     */
    private static List<String> getClassNamesInJars(String packageName, List<JarFile> jars) {

        // Replace the package "." notation for "/" file notation if needed
        packageName = packageName.replace(".", "/");

        // List of classes on the package
        List<String> classes = new ArrayList<String>();

        // For every jar...
        for (JarFile jarFile : jars) {

            // Get it's contents...
            Enumeration<JarEntry> entries = jarFile.entries();

            // Iterate through contents...
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // If it's a .class from the given package on the classpath or
                // on the AS jar folder...
                if ((entry.getName().startsWith(packageName) || entry.getName().startsWith(
                        WEB_INF_CLASSES + packageName))
                        && entry.getName().endsWith(CLASS_FILE_EXTENSION)) {

                    // ...get it's name...
                    String className = entry.getName();

                    // ...trim as necessary...
                    if (className.startsWith(SLASH_STRING)) {
                        className = className.substring(1);
                    }

                    // ...convert from folder-kind name to package-kind name...
                    className = className.replace(SLASH_CHAR, PACKAGE_SEPARATOR_CHAR);

                    // ...prune '.class' extension...
                    className = className.substring(0, className.length() - CLASS_FILE_EXTENSION.length());

                    // ...load the class with javassist and add it to result
                    classes.add(className);
                }
            }
        }
        return classes;
    }

    /**
     * List all the classes on a given package as determined by the context
     * class loader
     *
     * @param packageName
     *            the package name to search
     * @return a list of classes that exist within that package
     * @throws ResourceException
     *             if the package can't be found or uses an unknown protocol
     */
    private static List<String> getClassesInSinglePackage(String packageName) throws ResourceException {

        // This will hold a list of directories/jars matching the packageName.
        // There may be more than one if a package is split over multiple
        // jars/paths
        List<File> folders = new ArrayList<File>();
        List<JarFile> jars = new ArrayList<JarFile>();

        // The package resources
        Enumeration<URL> resources = getPackageResources(packageName);

        // List of classes on the package
        List<String> classes = null;

        if (resources != null) {
            // Add the resources to directories
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (resource.getProtocol().equalsIgnoreCase("FILE")) {
                    try {
                        folders.add(new File(URLDecoder.decode(resource.getPath(), "UTF-8")));

                    } catch (UnsupportedEncodingException unsupportedEncodingException) {
                        // Unable to provide unit test since
                        // UnsupportedEncodingException is raised by URLDecoder
                        throw new ResourceException(packageName
                                + " does not appear to be a valid package (Unsupported encoding)",
                                unsupportedEncodingException);
                    }

                } else if (resource.getProtocol().equalsIgnoreCase("JAR")) {
                    try {
                        JarURLConnection conn = (JarURLConnection) resource.openConnection();
                        jars.add(conn.getJarFile());

                    } catch (IOException exception) {
                        // Unable to provide unit test since IOException is
                        // raised by openConnection() and getJarFile()
                        throw new ResourceException(packageName
                                + " does not appear to be a valid package (Unsupported encoding)", exception);
                    }
                } else {
                    throw new ResourceException(packageName + "Unknown protocol on class resource: "
                            + resource.toExternalForm());
                }
            }

            // Init classes
            classes = getClassNamesInFolders(packageName, folders);
            classes.addAll(getClassNamesInJars(packageName, jars));
        }

        return classes;
    }

    /**
     * Returns a list of classes in a given package and in all it's subpackages,
     * if they exist.
     *
     * @param packageName
     *            the package to search for classes
     * @return a list with all classes from the package and its sub-packages, if
     *         they exist
     * @throws ResourceException
     *             if package is not found
     */
    public static List<String> getClassesInPackage(String packageName) throws ResourceException {

        // Get all sub packages
        List<String> subpackages = getSubpackages(packageName);
        // Add top-level package
        subpackages.add(packageName);

        // Registry for processed classes. The key is the class's FQN.
        List<String> deepPackageClasses = new ArrayList<String>();

        // Get classes in each sub-package
        for (String subpackage : subpackages) {
            List<String> packageClasses = getClassesInSinglePackage(subpackage);

            // If class wasn't found before, add it!
            for (String className : packageClasses) {
                if (!deepPackageClasses.contains(className)) {
                    deepPackageClasses.add(className);
                }
            }
        }

        return deepPackageClasses;
    }

    /**
     * Returns a list of subpackages of a given package.
     *
     * @param packageName
     *            the package to search for subpackages
     * @return a list of all subpackages of the given package
     * @throws ResourceException
     *             if package resources can't be found
     */
    private static List<String> getSubpackages(String packageName) throws ResourceException {

        // Initialize result. Although V type is String, it will set to null on
        // element addition.
        Set<String> subpackages = new HashSet<String>();

        // Get all package resources
        Enumeration<URL> resources = null;

        try {
            resources = getPackageResources(packageName);
        } catch (ResourceException resourceNotFoundException) {
            throw new ResourceException("Could not get resources for package " + packageName, resourceNotFoundException);
        }

        // ...if there are resources for the given packageName...
        if (resources != null) {

            while (resources.hasMoreElements()) {
                String path = resources.nextElement().getPath();

                List<String> subpackagesFolders = null;
                try {
                    subpackagesFolders = getSubfolders(new File(URLDecoder.decode(path, "UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // For each subfolder...
                for (String fullpath : subpackagesFolders) {

                    // ...split the absolute path by the package name (produces
                    // two parts: <absolute_path_to_package> + <a_subfolder>
                    String[] parts = fullpath.split(packageName);

                    // Convert SLASH to DOT. Note: parts[1] contains the
                    // subfolder name.
                    String subfolderAsPackage = parts[1].replace(File.separatorChar, PACKAGE_SEPARATOR_CHAR);

                    // subpackage name is the package name followed by
                    // subfolderAsPackage
                    String subpackageFQN = packageName + subfolderAsPackage;

                    // If subpackage FQN is not on subpackages, then add it!!
                    if (!subpackages.contains(subpackageFQN)) {
                        subpackages.add(subpackageFQN);
                    }
                }
            }
        }

        /*
         * Convert Set to List.
         *
         * <pre> IMPLEMENTATION NOTE:
         *
         * Using:
         *
         * List<String> subpackagesNames = Arrays.asList(subpackages.
         * toArray(new String[subpackages.size()]));
         *
         * Converts the List to a fixed-size array (the toArray(T[]) method) and
         * the array "const-ness" is preserved on the conversion back to list.
         *
         * Later on, it is not possible to add new elements to the reconverted
         * list.
         *
         * Addition of new elements raises an UnsupportedOperationException.
         * </pre>
         */
        List<String> subpackagesNames = new ArrayList<String>();

        for (String subpackageName : subpackages) {
            subpackagesNames.add(subpackageName);
        }

        return subpackagesNames;
    }

    /**
     * Returns a list of all non-empty subfolders of a given top-level folder.
     *
     * @param topLevelFolder
     *            the top level folder from which subfolders will be searched
     * @return the list of all subfolders of the given folders
     */
    private static List<String> getSubfolders(File topLevelFolder) {
        // Initialize return
        List<String> subfolders = new ArrayList<String>();

        // The folder contents
        File[] folderContents;
        // Getting all the direct subfolders of the top-level folder
        folderContents = topLevelFolder.listFiles(new FolderFilter());

        if (folderContents != null) {
            // For each of the direct subfolders...
            for (int i = 0; i != folderContents.length; ++i) {
                // ...add the path to the subfolders list
                subfolders.add(folderContents[i].getAbsolutePath());

                // ...search each of the direct subfolders for subfolders
                // recursively
                List<String> subfolder = getSubfolders(folderContents[i]);

                // ...if the previous search returned any results...
                if (subfolder.size() != 0) {
                    // ...add them to the result
                    subfolders.addAll(subfolder);
                }
            }
        }
        return subfolders;
    }

    /** A FileFilter implementation to filter out non-folder files. */
    private static class FolderFilter implements FileFilter {

        /** @see java.io.FileFilter#accept(File) */
        @Override
        public boolean accept(File file) {
            // T if file is a folder
            return file.isDirectory();
        }
    }

    public static void main(String[] sdfa) {
        List<String> Strings = ResourceLoader.getClassesInPackage("org");
        for (String string : Strings) {
            System.out.println(string);
        }
        System.out.println(Strings.size());
        System.out.println(Strings.size());
    }
}
