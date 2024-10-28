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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.ivy.TestHelper;
import org.apache.ivy.util.FileUtil;

import org.apache.tools.ant.Project;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class IvyRepositoryReportTest {

    private IvyRepositoryReport report;

    @Before
    public void setUp() {
        TestHelper.createCache();
        Project project = TestHelper.newProject();
        project.setProperty("ivy.settings.file", "test/repositories/ivysettings-1.xml");

        report = new IvyRepositoryReport();
        report.setProject(project);
        System.setProperty("ivy.cache.dir", TestHelper.cache.getAbsolutePath());
    }

    @After
    public void tearDown() {
        TestHelper.cleanCache();
    }

    @Test
    public void testSimple() throws Exception {
        report.setOrganisation("org1");
        report.setOutputname("testsimple");
        report.setTodir(TestHelper.cache);
        report.execute();

        File reportFile = new File(TestHelper.cache, "testsimple.xml");
        assertTrue(reportFile.exists());
        String g = FileUtil.readEntirely(new BufferedReader(new FileReader(reportFile)));

        // check presence of the modules
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.1\""));
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.2\""));
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.3\""));
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.4\""));
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.5\""));
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.6\""));
    }

    @Test
    public void testBranchBeforeModule() throws Exception {
        report.getProject().setProperty("ivy.settings.file",
            "test/repositories/IVY-716/ivysettings.xml");
        report.setOutputname("testbranch");
        report.setTodir(TestHelper.cache);
        report.execute();

        File reportFile = new File(TestHelper.cache, "testbranch.xml");
        assertTrue(reportFile.exists());
        String g = FileUtil.readEntirely(new BufferedReader(new FileReader(reportFile)));

        // check presence of the modules
        assertTrue(g.contains("<module organisation=\"org1\" name=\"mod1.1\""));

        // check presence of the branches
        assertTrue(g.contains("<revision name=\"1.0\" branch=\"branch1\""));
        assertTrue(g.contains("<revision name=\"1.0\" branch=\"branch2\""));
    }

    @Test
    public void testPatternWithoutOrganisation() throws Exception {
        report.getProject().setProperty("ivy.settings.file",
            "test/repositories/IVY-729/ivysettings.xml");
        report.setOutputname("test-no-org");
        report.setTodir(TestHelper.cache);
        report.execute();

        File reportFile = new File(TestHelper.cache, "test-no-org.xml");
        assertTrue(reportFile.exists());
        String g = FileUtil.readEntirely(new BufferedReader(new FileReader(reportFile)));

        // check presence of the modules
        assertTrue(g.contains("<module organisation=\"null\" name=\"a\""));
        assertTrue(g.contains("<module organisation=\"null\" name=\"b\""));
        assertTrue(g.contains("<module organisation=\"null\" name=\"c\""));
    }
}
