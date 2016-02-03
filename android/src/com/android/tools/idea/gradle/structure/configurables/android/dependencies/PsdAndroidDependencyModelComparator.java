/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.android.dependencies;

import com.android.tools.idea.gradle.structure.model.android.PsdAndroidDependencyModel;
import com.android.tools.idea.gradle.structure.model.android.PsdAndroidLibraryDependencyModel;

import java.util.Comparator;

import static com.android.tools.idea.gradle.structure.configurables.android.dependencies.ArtifactDependencySpecs.asText;

public class PsdAndroidDependencyModelComparator implements Comparator<PsdAndroidDependencyModel> {
  private final boolean myShowGroupId;

  public PsdAndroidDependencyModelComparator(boolean showGroupId) {
    myShowGroupId = showGroupId;
  }

  @Override
  public int compare(PsdAndroidDependencyModel m1, PsdAndroidDependencyModel m2) {
    if (m1 instanceof PsdAndroidLibraryDependencyModel) {
      if (m2 instanceof PsdAndroidLibraryDependencyModel) {
        String s1 = asText(((PsdAndroidLibraryDependencyModel)m1).getSpec(), myShowGroupId);
        String s2 = asText(((PsdAndroidLibraryDependencyModel)m2).getSpec(), myShowGroupId);
        return s1.compareTo(s2);
      }
    }
    return 1;
  }
}
