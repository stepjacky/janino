
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.*;
import java.util.Map;

import junit.framework.*;

import org.codehaus.janino.*;
import org.codehaus.janino.Compiler; // To resolve ambiguity with "java.lang.Compiler".
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.MapResourceCreator;
import org.codehaus.janino.util.resource.MapResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;

public class JavaSourceClassLoaderTests extends TestCase {
    private static final File SOURCE_DIRECTORY = new File("src");

    public static Test suite() {
        TestSuite s = new TestSuite(JavaSourceClassLoaderTests.class.getName());
        s.addTest(new JavaSourceClassLoaderTests("testJSCL"));
        s.addTest(new JavaSourceClassLoaderTests("testCJSCL"));
        return s;
    }

    public JavaSourceClassLoaderTests(String name) { super(name); }

    public void testJSCL() throws Exception {
        ClassLoader bootstrapClassLoader = new ClassLoader(null) {};
        String className = Compiler.class.getName();

        Benchmark b = new Benchmark(true);
        b.beginReporting("Loading class \"" + className + "\" through a JavaSourceClassLoader");
        new JavaSourceClassLoader(
            bootstrapClassLoader,            // parentClassLoader
            new File[] { SOURCE_DIRECTORY }, // optionalSourcePath
            null,                            // optionalCharacterEncoding
            DebuggingInformation.NONE        // debuggingInformation
        ).loadClass(className);
        b.endReporting();
    }
    
    public void testCJSCL() throws Exception {
        ClassLoader bootstrapClassLoader = new ClassLoader(null) {};
        String className = Compiler.class.getName();
        
        Benchmark b = new Benchmark(true);
        b.beginReporting("Loading class \"" + className + "\" through a CachingJavaSourceClassLoader");
        MapResourceCreator classFileResources1 = new MapResourceCreator();
        new CachingJavaSourceClassLoader(
            bootstrapClassLoader,                          // parentClassLoader
            new DirectoryResourceFinder(SOURCE_DIRECTORY), // sourceFinder
            (String) null,                                 // optionalCharacterEncoding
            ResourceFinder.EMPTY_RESOURCE_FINDER,          // classFileCacheResourceFinder
            classFileResources1,                           // classFileResourceCreator
            DebuggingInformation.NONE                      // debuggingInformation
        ).loadClass(className);
        Map classFileMap1 = classFileResources1.getMap();
        b.endReporting("Generated " + classFileMap1.size() + " class files.");
        assertTrue("More than 200 class files", classFileMap1.size() > 200);

        b.beginReporting("Loading class \"" + className + "\" again, but with the class files created during the first compilation being available, i.e. no source files should be recompiled");
        MapResourceCreator classFileResources2 = new MapResourceCreator();
        MapResourceFinder classFileFinder = new MapResourceFinder(classFileMap1);
        classFileFinder.setLastModified(System.currentTimeMillis());
        new CachingJavaSourceClassLoader(
            bootstrapClassLoader,                          // parentClassLoader
            new DirectoryResourceFinder(SOURCE_DIRECTORY), // sourceFinder
            (String) null,                                 // optionalCharacterEncoding
            classFileFinder,                               // classFileCacheResourceFinder
            classFileResources2,                           // classFileResourceCreator
            DebuggingInformation.NONE                      // debuggingInformation
        ).loadClass(className);
        b.endReporting("Generated " + classFileResources2.getMap().size() + " class files.");
        assertEquals("Zero files recompiled", 0, classFileResources2.getMap().size());
    }
}
