package com.mengcraft.script.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenLibs {

    private static final String LOCAL_REPOSITORY = System.getProperty("user.home") + "/.m2/repository";
    private static final String CENTRAL = System.getProperty("maven.repository", "https://mirrors.huaweicloud.com/repository/maven");

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();

    private static final Logger LOGGER = Logger.getLogger("MavenLibs");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+?)}");
    private static final Set<String> NSS = Sets.newHashSet();

    private final Map<String, String> properties = Maps.newHashMap();
    private final String ns;
    private final String basename;
    private final boolean optional;
    private MavenLibs parent;
    private List<MavenLibs> dependencies;

    private MavenLibs(String groupId, String artifactId, String version, boolean optional) {
        properties.put("groupId", groupId);
        properties.put("artifactId", artifactId);
        properties.put("version", version);
        properties.put("project.groupId", groupId);
        properties.put("project.artifactId", artifactId);
        properties.put("project.version", version);
        basename = artifactId + "-" + version;
        ns = groupId.replace('.', '/') + "/" + artifactId + "/" + version;
        this.optional = optional;
    }

    private String getProperty(String name) {
        if (parent != null) {
            String property = parent.getProperty(name);
            if (property != null) {
                return property;
            }
        }
        return properties.get(name);
    }

    public void load() {
        load(MavenLibs.class.getClassLoader());
    }

    @SneakyThrows
    public void load(ClassLoader cl) {
        Preconditions.checkState(cl instanceof URLClassLoader, "Current classloader not instanceof URLClassLoader");
        if (optional || NSS.contains(ns)) {
            return;
        }
        // parse pom file
        initialize();
        // parent and dependencies
        if (parent != null) {
            parent.load(cl);
        }
        for (MavenLibs dep : dependencies) {
            dep.load(cl);
        }
        NSS.add(ns);
        // skip if not jar packages
        if (!isNullOrEquals(properties.get("project.packaging"), "jar")) {
            return;
        }
        // hack into classloader
        File jar = new File(LOCAL_REPOSITORY, ns + "/" + basename + ".jar");
        if (!jar.exists()) {
            String url = CENTRAL + "/" + ns + "/" + basename + ".jar";
            LOGGER.info("Get " + url);
            downloads(jar, url);
        }
        URLClassLoaderAccessor.addUrl((URLClassLoader) cl, jar.toURI().toURL());
        LOGGER.info(String.format("Load MavenLibs(%s)", ns));
    }

    @SneakyThrows
    private void initialize() {// initialize
        File pom = new File(LOCAL_REPOSITORY, ns + "/" + basename + ".pom");
        if (!pom.exists()) {
            downloads(pom, CENTRAL + "/" + ns + "/" + basename + ".pom");
        }
        DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        Document doc = builder.parse(pom);
        XPath x = X_PATH_FACTORY.newXPath();
        // parse properties
        Node node = (Node) x.evaluate("/project/properties", doc, XPathConstants.NODE);
        if (node != null) {
            mapOf(() -> properties, node);
        }
        // parent
        node = (Node) x.evaluate("/project/parent", doc, XPathConstants.NODE);
        if (node != null) {
            parent = dependOf(mapOf(node));
            parent.properties.put("project.packaging", "pom");
        }
        // dependencies
        NodeList nodes = (NodeList) x.evaluate("/project/dependencies/dependency", doc, XPathConstants.NODESET);
        int len = nodes.getLength();
        dependencies = Lists.newArrayListWithCapacity(len);
        for (int i = 0; i < len; i++) {
            dependencies.add(dependOf(mapOf(nodes.item(i))));
        }
    }

    @SneakyThrows
    private void downloads(File f, String url) {
        File parent = f.getParentFile();
        Preconditions.checkState(parent.exists() || parent.mkdirs(), "mkdirs");
        File tmp = File.createTempFile("MavenLibs", ".tmp");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0");
        try {
            try (FileOutputStream fs = new FileOutputStream(tmp)) {
                ByteStreams.copy(connection.getInputStream(), fs);
            }
            if (f.exists()) {// async downloads?
                Preconditions.checkState(tmp.delete(), "delete tmp");
            } else {
                Files.move(tmp, f);
            }
        } catch (Exception e) {
            Preconditions.checkState(tmp.delete(), "delete tmp");
        } finally {
            connection.disconnect();
        }
    }

    private Map<String, String> mapOf(Node node) {
        return mapOf(Maps::newHashMap, node);
    }

    private Map<String, String> mapOf(Supplier<Map<String, String>> factory, Node node) {
        Map<String, String> map = factory.get();
        NodeList childNodes = node.getChildNodes();
        int length = childNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node _node = childNodes.item(i);
            if (_node.hasChildNodes()) {
                String nodeName = _node.getNodeName();
                String value = _node.getFirstChild().getNodeValue();
                map.put(nodeName, value);
            }
        }
        return map;
    }

    private String resolve(String value) {
        Matcher mc = PROPERTY_PATTERN.matcher(value);
        while (mc.find()) {
            value = value.replace(mc.group(), String.valueOf(getProperty(mc.group(1))));
        }
        return value;
    }

    private MavenLibs dependOf(Map<String, String> map) {
        String groupId = map.get("groupId");
        String artifactId = map.get("artifactId");
        String version = resolve(map.get("version"));
        boolean optional = Boolean.parseBoolean(map.get("optional")) || version == null || !isNullOrEquals(map.get("scope"), "compile");
        return new MavenLibs(groupId, artifactId, version, optional);
    }

    private static Method assetAccessibleMethod(Class<?> cls, String name, Class<?>... parameters) {
        try {
            Method method = cls.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isNullOrEquals(String obj, String comp) {
        return obj == null || obj.equals(comp);
    }

    public static MavenLibs of(String groupId, String artifactId, String version) {
        return new MavenLibs(groupId, artifactId, version, false);
    }

    public static MavenLibs of(String namespace) {
        String[] split = namespace.split(":");
        return of(split[0], split[1], split[2]);
    }
}
