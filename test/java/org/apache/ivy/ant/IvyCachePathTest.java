/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.io.File;

import org.apache.ivy.TestHelper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.JavaVersion;
import org.apache.tools.ant.types.Path;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IvyCachePathTest {

    private IvyCachePath path;

    private Project project;

    @Before
    public void setUp() {
        TestHelper.createCache();
        project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        path = new IvyCachePath();
        path.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testSimple() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-simple.xml");
        path.setPathid("simple-pathid");
        path.execute();
        Object ref = project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .getAbsolutePath(), new File(p.list()[0]).getAbsolutePath());
    }

    @Test
    public void testInline1() {
        // we first resolve another ivy file
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());

        // then we resolve a dependency directly
        path.setOrganisation("org1");
        path.setModule("mod1.2");
        path.setRevision("2.0");
        path.setInline(true);
        path.setPathid("simple-pathid");
        path.execute();
        Object ref = project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .getAbsolutePath(), new File(p.list()[0]).getAbsolutePath());
    }

    @Test
    public void testInline2() {
        // we first resolve a dependency directly
        path.setOrganisation("org1");
        path.setModule("mod1.2");
        path.setRevision("2.0");
        path.setInline(true);
        path.setPathid("simple-pathid");
        path.execute();
        Object ref = project.getReference("simple-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .getAbsolutePath(), new File(p.list()[0]).getAbsolutePath());

        // we then resolve another ivy file
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        assertTrue(getArchiveFileInCache("org1", "mod1.2", "2.2", "mod1.2", "jar", "jar").exists());
    }

    @Test
    public void testEmptyConf() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-108.xml");
        path.setPathid("emptyconf-pathid");
        path.setConf("empty");
        path.execute();
        Object ref = project.getReference("emptyconf-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(0, p.size());
    }

    /**
     * Test must fail with default haltonfailure setting.
     */
    @Test(expected = BuildException.class)
    public void testFailure() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        path.setPathid("failure-pathid");
        path.execute();
    }

    @Test
    public void testHaltOnFailure() {
        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-failure.xml");
        path.setPathid("haltfailure-pathid");
        path.setHaltonfailure(false);
        path.execute();
    }

    @Test
    public void testWithResolveId() {
        IvyResolve resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("withResolveId");
        resolve.execute();

        // resolve another ivy file
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        path.setResolveId("withResolveId");
        path.setPathid("withresolveid-pathid");
        path.execute();

        Object ref = project.getReference("withresolveid-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .getAbsolutePath(), new File(p.list()[0]).getAbsolutePath());
    }

    @Test
    public void testWithResolveIdWithoutResolve() {
        Project otherProject = TestHelper.newProject();
        otherProject.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        IvyResolve resolve = new IvyResolve();
        resolve.setProject(otherProject);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        resolve.setResolveId("withResolveId");
        resolve.execute();

        // resolve another ivy file
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        path.setResolveId("withResolveId");
        path.setPathid("withresolveid-pathid");
        path.setFile(new File("test/java/org/apache/ivy/ant/ivy-simple.xml"));
        path.execute();

        Object ref = project.getReference("withresolveid-pathid");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        assertEquals(getArchiveFileInCache("org1", "mod1.2", "2.0", "mod1.2", "jar", "jar")
                .getAbsolutePath(), new File(p.list()[0]).getAbsolutePath());
    }

    @Test
    public void testWithResolveIdAndMissingConfs() {
        Project otherProject = TestHelper.newProject();
        otherProject.setProperty("ivy.settings.file", "test/repositories/ivysettings.xml");

        IvyResolve resolve = new IvyResolve();
        resolve.setProject(otherProject);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        resolve.setResolveId("testWithResolveIdAndMissingConfs");
        resolve.setConf("default");
        resolve.execute();

        // resolve another ivy file
        resolve = new IvyResolve();
        resolve.setProject(project);
        resolve.setFile(new File("test/java/org/apache/ivy/ant/ivy-latest.xml"));
        resolve.execute();

        project.setProperty("ivy.dep.file", "test/java/org/apache/ivy/ant/ivy-multiconf.xml");

        path.setResolveId("testWithResolveIdAndMissingConfs");
        path.setPathid("withresolveid-pathid");
        path.setConf("default,compile");
        path.setFile(new File("test/java/org/apache/ivy/ant/ivy-multiconf.xml"));
        path.execute();
    }

    @Test
    public void testUnpack() {
        project.setProperty("ivy.dep.file",
            "test/repositories/1/packaging/module1/ivys/ivy-1.0.xml");
        path.setPathid("testUnpack");
        path.execute();
        Object ref = project.getReference("testUnpack");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        assertTrue(new File(p.list()[0]).isDirectory());
    }

    @Test
    public void testOSGi() {
        project.setProperty("ivy.dep.file",
            "test/repositories/1/packaging/module5/ivys/ivy-1.0.xml");
        path.setPathid("testOSGi");
        path.setOsgi(true);
        path.execute();
        Object ref = project.getReference("testOSGi");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(4, p.size());
        File cacheDir = path.getSettings().getDefaultRepositoryCacheBasedir();
        File unpacked = new File(cacheDir, "packaging/module3/jar_unpackeds/module3-1.0");
        assertEquals(new File(unpacked, "lib/ant-antlr.jar"), new File(p.list()[0]));
        assertEquals(new File(unpacked, "lib/ant-apache-bcel.jar"), new File(p.list()[1]));
        assertEquals(new File(unpacked, "lib/ant-apache-bsf.jar"), new File(p.list()[2]));
        assertEquals(new File(unpacked, "lib/ant-apache-log4j.jar"), new File(p.list()[3]));
    }

    @Test
    public void testOSGi2() {
        project.setProperty("ivy.dep.file",
            "test/repositories/1/packaging/module6/ivys/ivy-1.0.xml");
        path.setPathid("testOSGi");
        path.setOsgi(true);
        path.execute();
        Object ref = project.getReference("testOSGi");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(1, p.size());
        File cacheDir = path.getSettings().getDefaultRepositoryCacheBasedir();
        File unpacked = new File(cacheDir, "packaging/module4/jar_unpackeds/module4-1.0");
        assertEquals(unpacked, new File(p.list()[0]));
    }

    @Test
    public void testPackedOSGi() {
        final JavaVersion java14OrHigher = new JavaVersion();
        java14OrHigher.setAtLeast("14");
        Assume.assumeFalse("Pack200 tools and API have been removed since JDK 14", java14OrHigher.eval());

        project.setProperty("ivy.dep.file",
            "test/repositories/1/packaging/module8/ivys/ivy-1.0.xml");
        path.setPathid("testOSGi");
        path.setOsgi(true);
        path.execute();
        Object ref = project.getReference("testOSGi");
        assertNotNull(ref);
        assertTrue(ref instanceof Path);
        Path p = (Path) ref;
        assertEquals(4, p.size());
        File cacheDir = path.getSettings().getDefaultRepositoryCacheBasedir();
        File unpacked = new File(cacheDir, "packaging/module7/jar_unpackeds/module7-1.0");
        assertEquals(new File(unpacked, "lib/ant-antlr.jar"), new File(p.list()[0]));
        assertEquals(new File(unpacked, "lib/ant-apache-bcel.jar"), new File(p.list()[1]));
        assertEquals(new File(unpacked, "lib/ant-apache-bsf.jar"), new File(p.list()[2]));
        assertEquals(new File(unpacked, "lib/ant-apache-log4j.jar"), new File(p.list()[3]));
    }

    private File getArchiveFileInCache(String organisation, String module, String revision,
            String artifact, String type, String ext) {
        return TestHelper.getArchiveFileInCache(path.getIvyInstance(), organisation, module,
            revision, artifact, type, ext);
    }
}
