package org.jetbrains.jps.android;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.AdditionalRootsProviderService;
import org.jetbrains.jps.builders.BuildTarget;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.ModuleBuildTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidAdditionalRootProviderService extends AdditionalRootsProviderService<JavaSourceRootDescriptor> {
  public AndroidAdditionalRootProviderService() {
    super(Collections.singletonList(JavaModuleBuildTargetType.PRODUCTION));
  }

  @NotNull
  @Override
  public List<JavaSourceRootDescriptor> getAdditionalRoots(@NotNull BuildTarget<JavaSourceRootDescriptor> target, File dataStorageRoot) {
    ModuleBuildTarget buildTarget = (ModuleBuildTarget)target;
    final File generatedSourcesRoot = AndroidJpsUtil.getGeneratedSourcesStorage(buildTarget.getModule(), dataStorageRoot);
    final List<JavaSourceRootDescriptor> result = new ArrayList<JavaSourceRootDescriptor>();

    addRoot(result, buildTarget, new File(generatedSourcesRoot, AndroidJpsUtil.AAPT_GENERATED_SOURCE_ROOT_NAME));
    addRoot(result, buildTarget, new File(generatedSourcesRoot, AndroidJpsUtil.AIDL_GENERATED_SOURCE_ROOT_NAME));
    addRoot(result, buildTarget, new File(generatedSourcesRoot, AndroidJpsUtil.RENDERSCRIPT_GENERATED_SOURCE_ROOT_NAME));
    addRoot(result, buildTarget, new File(generatedSourcesRoot, AndroidJpsUtil.BUILD_CONFIG_GENERATED_SOURCE_ROOT_NAME));

    return result;
  }

  private static void addRoot(List<JavaSourceRootDescriptor> result, ModuleBuildTarget buildTarget, final File file) {
    result.add(new JavaSourceRootDescriptor(file, buildTarget, true, false, ""));
  }
}
