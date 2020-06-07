package com.softwood.utils

import java.util.stream.Stream

/**
 * had som many problems loading files
 */
class ClasspathHelper {

    static String targetResourcesMain = "/target/resources/main"
    static String targetClassesMain = "/target/classes/groovy/main"

    /**
     * returns the file path,  in unix format for the resources directory
     * @return resources classpath root
     */
    static String getProjectResourcesClasspath() {
        String userDir = System.getProperty("user.dir").replace('\\', '/')
        String projectRootDir = userDir.split('/src') ?[0]
        String runtimeResourcesClasspath = (projectRootDir + targetResourcesMain).toString()
        runtimeResourcesClasspath
    }

    /**
     * returns the file path,  in unix format for the projects classes  directory
     * @return resources classpath root
     */
    static String getProjectClassesClasspath() {
        String userDir = System.getProperty("user.dir").replace('\\', '/')
        String projectRootDir = userDir.split('/src') ?[0]
        String runtimeClassesClasspath = (projectRootDir + targetClassesMain).toString()
        runtimeClassesClasspath
    }

    /**
     * constructs a new URLClassloader using the same parent as system class loader, and sets the base
     * URL to search in to the resources classpath, returns URL for named file
     *
     * @param name - name of file, normally start with relative name, as we are at the root
     * @return URL for resource
     */

    static URL getUrlFromResource(String name) {
        // note toUrl() has been deprecated https://docs.oracle.com/javase/7/docs/api/java/io/File.html
        URL base = new File(getProjectResourcesClasspath()).toURI().toURL()
        //"/D:/Intellij-projects/softwoodUtils/subprojects/utils/target/resources/main"
        URL[] urls = [base]

        URLClassLoader urlClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());
        URL url = urlClassLoader.getResource(name)

    }

    /**
     * returns an BufferedInputStream from a new URLClassloader using the same parent as system class loader, and sets the base
     * URL to search in to the resources classpath, returns InputStream from the file
     *
     * @param name - name of file, normally start with relative name, as we are at the root
     * @return (Buffered)InputStream  for resource
     */

    static InputStream getStreamFromResource(String name) {
        // note toUrl() has been deprecated https://docs.oracle.com/javase/7/docs/api/java/io/File.html
        URL base = new File(getProjectResourcesClasspath()).toURI().toURL()
        //"/D:/Intellij-projects/softwoodUtils/subprojects/utils/target/resources/main"
        URL[] urls = [base]

        URLClassLoader urlClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader().getParent());
        urlClassLoader.getResourceAsStream(name)
    }


    /**
     *  converts windows file name to unix equivalent
     * @param name
     * @return unix formatted  file name
     */
    static String windowsToUnixFileFormat(String name) {
        name.replace('\\', '/')
    }

    /**
     *  converts windows file name to unix equivalent
     * @param name
     * @return windows formatted file name
     */
    static String toWindowsFileFormat(String name) {
        name.replace('/', '\\')
    }
}
