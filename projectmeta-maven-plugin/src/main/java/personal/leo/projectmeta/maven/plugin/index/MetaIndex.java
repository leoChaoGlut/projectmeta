package personal.leo.projectmeta.maven.plugin.index;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author 谦扬
 * @date 2019-05-30
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MetaIndex {

    private final Map<String, SortedSet<String>> appMapModules = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, SortedSet<String>> moduleMapPackages = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, SortedSet<String>> packageMapClasses = Collections.synchronizedMap(new TreeMap<>());

    private final Map<String, SortedSet<String>> classMapPackages = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, SortedSet<String>> packageMapModules = Collections.synchronizedMap(new TreeMap<>());
    private final Map<String, SortedSet<String>> moduleMapApps = Collections.synchronizedMap(new TreeMap<>());

    /**
     * class: full qualifier class
     */
    private final Map<String, SortedSet<String>> classMapModules = Collections.synchronizedMap(new TreeMap<>());
    /**
     * class: full qualifier class
     */
    private final Map<String, SortedSet<String>> moduleMapClasses = Collections.synchronizedMap(new TreeMap<>());

    public static MetaIndex getInstance() {
        return new MetaIndex();
    }

    public void appMapModule(String app, String module) {
        appMapModules.computeIfAbsent(app, key -> new TreeSet<>()).add(module);
    }

    public void moduleMapPackage(String module, String pkg) {
        moduleMapPackages.computeIfAbsent(module, key -> new TreeSet<>()).add(pkg);
    }

    public void packageMapClasse(String pkg, String clz) {
        packageMapClasses.computeIfAbsent(pkg, key -> new TreeSet<>()).add(clz);
    }

    public void classMapPackage(String clz, String pkg) {
        classMapPackages.computeIfAbsent(clz, key -> new TreeSet<>()).add(pkg);
    }

    public void packageMapModule(String pkg, String module) {
        packageMapModules.computeIfAbsent(pkg, key -> new TreeSet<>()).add(module);
    }

    public void moduleMapApp(String module, String app) {
        moduleMapApps.computeIfAbsent(module, key -> new TreeSet<>()).add(app);
    }

    public void classMapModule(String clz, String module) {
        moduleMapApps.computeIfAbsent(clz, key -> new TreeSet<>()).add(module);
    }

    public void moduleMapClasse(String module, String clz) {
        moduleMapApps.computeIfAbsent(module, key -> new TreeSet<>()).add(clz);
    }

    //======
    public void appMapModules(String app, Set<String> modules) {
        appMapModules.computeIfAbsent(app, key -> new TreeSet<>()).addAll(modules);
    }

    public void moduleMapPackages(String module, Set<String> pkgs) {
        moduleMapPackages.computeIfAbsent(module, key -> new TreeSet<>()).addAll(pkgs);
    }

    public void packageMapClasses(String pkg, Set<String> clzs) {
        packageMapClasses.computeIfAbsent(pkg, key -> new TreeSet<>()).addAll(clzs);
    }

    public void classMapPackages(String clz, Set<String> pkgs) {
        classMapPackages.computeIfAbsent(clz, key -> new TreeSet<>()).addAll(pkgs);
    }

    public void packageMapModules(String pkg, Set<String> modules) {
        packageMapModules.computeIfAbsent(pkg, key -> new TreeSet<>()).addAll(modules);
    }

    public void moduleMapApps(String module, Set<String> apps) {
        moduleMapApps.computeIfAbsent(module, key -> new TreeSet<>()).addAll(apps);
    }

    public void classMapModules(String clz, Set<String> modules) {
        moduleMapApps.computeIfAbsent(clz, key -> new TreeSet<>()).addAll(modules);
    }

    public void moduleMapClasses(String module, Set<String> clzs) {
        moduleMapApps.computeIfAbsent(module, key -> new TreeSet<>()).addAll(clzs);
    }

    public String getAppByModule(String module) {
        final SortedSet<String> apps = moduleMapApps.get(module);
        if (apps.size() > 1) {
            throw new RuntimeException("a module is belong to " + apps.size() + " apps:" + apps);
        } else {
            return apps.iterator().next();
        }
    }

    public String getModuleByClass(String clz) {
        final SortedSet<String> modules = classMapModules.get(clz);
        if (modules.size() > 1) {
            throw new RuntimeException("a class is belong to " + modules.size() + " modules:" + modules);
        } else {
            return modules.iterator().next();
        }
    }

}
