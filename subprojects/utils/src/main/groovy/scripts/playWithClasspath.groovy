package scripts

import com.softwood.utils.ClasspathHelper
import sun.applet.AppletClassLoader

import java.util.stream.Stream

ClassLoader scriptClassLoader = this.class.getClassLoader()//ClassLoader.getSystemClassLoader()

String filename = this.class.getResource("/testfile.config").getPath()  //gets relative to the script in sources
String filename2 = scriptClassLoader.getResource("testfile.config").getPath()  //scriptClassLoader
println "file name from getResource on class using relative path  : " + filename
println "file name from getResource on script class loader  : " + filename

URL res = this.class.getResource("/testfile.config")
URL res2 = AppletClassLoader.getResource("testfile.config") // comes back with null

String resourcesClasspath = ClasspathHelper.getProjectResourcesClasspath()

ClassLoader sysCl = ClassLoader.getSystemClassLoader()
assert sysCl.name == 'app'
URL res3 = sysCl.getResource("testfile.config")  // comes back with null
URL res4 = ClasspathHelper.getUrlFromResource("testfile.config")


println ClasspathHelper.getStreamFromResource("testfile.config").text



