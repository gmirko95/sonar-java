/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaClasspath;
import org.sonar.java.JavaFilesCache;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class DefaultJavaResourceLocator implements JavaResourceLocator, JavaFileScanner {

  private static final Logger LOG = LoggerFactory.getLogger(JavaResourceLocator.class);

  private final Project project;
  private final JavaClasspath javaClasspath;
  @VisibleForTesting
  Map<String, Resource> resourcesCache;
  private Map<String, String> sourceFileCache;


  public DefaultJavaResourceLocator(Project project, JavaClasspath javaClasspath) {
    this.project = project;
    this.javaClasspath = javaClasspath;
    resourcesCache = Maps.newHashMap();
    sourceFileCache = Maps.newHashMap();
  }

  @Override
  public Resource findResourceByClassName(String className) {
    String name = className.replace('.', '/');
    Resource resource = resourcesCache.get(name);
    if (resource == null) {
      LOG.warn("Class not found in resource cache : {}", className);
    }
    return resource;
  }

  public String findSourceFileKeyByClassName(String className) {
    String name = className.replace('.', '/');
    return sourceFileCache.get(name);
  }

  @Override
  public Collection<File> classFilesToAnalyze() {
    ImmutableList.Builder<File> result = ImmutableList.builder();
    for (String key : resourcesCache.keySet()) {
      String filePath = key + ".class";
      for (File binaryDir : javaClasspath.getBinaryDirs()) {
        File classFile = new File(binaryDir, filePath);
        if (classFile.isFile()) {
          result.add(classFile);
          break;
        }
      }
    }
    return result.build();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    JavaFilesCache javaFilesCache = new JavaFilesCache();
    javaFilesCache.scanFile(context);
    for (Map.Entry<String, File> stringFileEntry : javaFilesCache.resourcesCache.entrySet()) {
      org.sonar.api.resources.File currentResource = org.sonar.api.resources.File.fromIOFile(stringFileEntry.getValue(), project);
      Preconditions.checkNotNull(currentResource, "resource not found : " + context.getFile().getName());
      resourcesCache.put(stringFileEntry.getKey(), currentResource);
      if(context.getFileKey() != null) {
        sourceFileCache.put(stringFileEntry.getKey(), context.getFileKey());
      }
    }
  }
}
