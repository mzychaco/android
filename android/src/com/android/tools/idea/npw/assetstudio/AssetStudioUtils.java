/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.npw.assetstudio;

import com.android.ide.common.util.AssetUtil;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.idea.npw.project.AndroidProjectPaths;
import com.android.tools.idea.res.AppResourceRepository;
import com.android.tools.adtui.ImageUtils;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.File;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Utility methods helpful for working with and generating Android assets.
 */
public final class AssetStudioUtils {

  /**
   * Create a tiny dummy image, so that we can always return a {@link NotNull} result if an image
   * we were looking for isn't found.
   */
  @NotNull
  public static BufferedImage createDummyImage() {
    // IntelliJ wants us to use UiUtil.createImage, for retina desktop screens, but we
    // intentionally avoid that here, because we just want to make a small notnull image
    //noinspection UndesirableClassUsage
    return new BufferedImage(1, 1, TYPE_INT_ARGB);
  }

  /**
   * Remove any surrounding padding from the image.
   */
  @NotNull
  public static BufferedImage trim(@NotNull BufferedImage image) {
    BufferedImage cropped = ImageUtils.cropBlank(image, null, TYPE_INT_ARGB);
    return cropped != null ? cropped : image;
  }

  /**
   * Pad the image with extra space. The padding percent works by taking the largest side of the
   * current image, multiplying that with the percent value, and adding that portion to each side
   * of the image.
   *
   * So for example, an image that's 100x100, with 50% padding percent, ends up resized to
   * (50+100+50)x(50+100+50), or 200x200. The 100x100 portion is then centered, taking up what
   * looks like 50% of the final image. The same 100x100 image, with 100% padding, ends up at
   * 300x300, looking in the final image like it takes up ~33% of the space.
   *
   * Padding can also be negative, which eats into the space of the original asset, causing a zoom
   * in effect.
   */
  @NotNull
  public static BufferedImage pad(@NotNull BufferedImage image, int paddingPercent) {
    if (image.getWidth() <= 1 || image.getHeight() <= 1) {
      // If we're handling a dummy image, just abort now before AssetUtil.paddedImage throws an
      // exception.
      return image;
    }

    if (paddingPercent > 100) {
      paddingPercent = 100;
    }

    int largerSide = Math.max(image.getWidth(), image.getHeight());
    int smallerSide = Math.min(image.getWidth(), image.getHeight());
    int padding = (largerSide * paddingPercent / 100);

    // Don't let padding get so negative that it would totally wipe out one of the dimensions. And
    // since padding is added to all sides, negative padding should be at most half of the smallest
    // side. (e.g if the smaller side is 100px, min padding is -49px)
    padding = Math.max(-(smallerSide / 2 - 1), padding);

    return AssetUtil.paddedImage(image, padding);
  }

  /**
   * Returns true if a resource with the same name is already found at a location implied by the
   * input parameters.
   */
  public static boolean resourceExists(@NotNull AndroidProjectPaths paths, @NotNull ResourceFolderType resourceType, @NotNull String name) {
    File resDir = paths.getResDirectory();
    if (resDir == null) {
      return false;
    }

    File[] resTypes = resDir.listFiles();
    if (resTypes == null) {
      return false;
    }

    // The path of a resource looks something like:
    //
    // path/to/res/
    //   drawable/name
    //   drawable-hdpi-v9/name
    //   drawable-hdpi-v11/name
    //   drawable-mdpi-v9/name
    //   ...
    //
    // We don't really care about the "drawable" directory here; we just want to search all folders
    // in res/ and look for the first match in any of them.
    for (File resTypeDir : resTypes) {
      if (resTypeDir.isDirectory() && resourceType.equals(ResourceFolderType.getFolderType(resTypeDir.getName()))) {
        File[] files = resTypeDir.listFiles();
        if (files != null) {
          for (File f : files) {
            if (FileUtil.getNameWithoutExtension(f).equalsIgnoreCase(name)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  /**
   * Like {@link #resourceExists(AndroidProjectPaths, ResourceFolderType, String)} but a useful
   * fallback if information about the current paths is not known.
   */
  public static boolean resourceExists(@NotNull AndroidFacet facet, @NotNull ResourceType resourceType, @NotNull String name) {
    AppResourceRepository repository = AppResourceRepository.getOrCreateInstance(facet);
    return repository.hasResourceItem(resourceType, name);
  }

  private AssetStudioUtils() {
  }
}
