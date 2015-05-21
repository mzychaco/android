/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.editors.theme.attributes.editors;

import com.android.SdkConstants;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.editors.theme.preview.AndroidThemePreviewPanel;
import com.android.tools.idea.editors.theme.datamodels.EditedStyleItem;
import com.android.tools.idea.rendering.ResourceHelper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import org.jetbrains.android.uipreview.ChooseResourceDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ColorEditor extends TypedCellEditor<EditedStyleItem, AttributeEditorValue> {
  private static final Logger LOG = Logger.getInstance(ColorEditor.class);

  private final Module myModule;
  private final Configuration myConfiguration;

  private final ColorComponent myComponent;
  private AttributeEditorValue myEditorValue = null;

  private final AndroidThemePreviewPanel myPreviewPanel;

  private EditedStyleItem myItem;

  public ColorEditor(@NotNull Module module, @NotNull Configuration configuration, AndroidThemePreviewPanel previewPanel) {
    myModule = module;
    myConfiguration = configuration;

    myComponent = new ColorComponent();
    myComponent.addActionListener(new ColorEditorActionListener());

    myPreviewPanel = previewPanel;
  }

  @Override
  public Component getEditorComponent(JTable table, EditedStyleItem value, boolean isSelected, int row, int column) {
    myItem = value;
    final List<Color> colors = ResourceHelper.resolveMultipleColors(myConfiguration.getResourceResolver(), myItem.getItemResourceValue());
    myComponent.configure(myItem, colors);
    myEditorValue = null; // invalidate stored editor value

    return myComponent;
  }

  @Override
  public AttributeEditorValue getEditorValue() {
    return myEditorValue;
  }

  private class ColorEditorActionListener implements ActionListener {
    @Override
    public void actionPerformed(final ActionEvent e) {
      String itemValue = myItem.getValue();
      final String colorName;
      // If it points to an existing resource.
      if (itemValue != null &&
          !SdkConstants.NULL_RESOURCE.equals(itemValue) &&
          itemValue.startsWith(SdkConstants.PREFIX_RESOURCE_REF)) {
        // Use the name of that resource.
        colorName = itemValue.substring(itemValue.indexOf('/') + 1);
      }
      else {
        // Otherwise use the name of the attribute.
        colorName = myItem.getName();
      }

      // TODO we need to handle color state lists correctly here.
      String resolvedColor = ResourceHelper.colorToString(
        ResourceHelper.resolveColor(myConfiguration.getResourceResolver(), myItem.getItemResourceValue()));

      final ChooseResourceDialog dialog = new ChooseResourceDialog(myModule, ChooseResourceDialog.COLOR_TYPES, resolvedColor, null,
                                                                   ChooseResourceDialog.ResourceNameVisibility.FORCE, colorName);

      final String oldValue = myItem.getItemResourceValue().getValue();

      dialog.setResourcePickerListener(new ChooseResourceDialog.ResourcePickerListener() {
        @Override
        public void resourceChanged(@Nullable String resource) {
          myItem.getItemResourceValue().setValue(resource == null ? oldValue : resource);
          myPreviewPanel.invalidateGraphicsRenderer();
        }
      });

      dialog.show();

      // Dialog has been closed, clean up
      myItem.getItemResourceValue().setValue(oldValue);
      myPreviewPanel.invalidateGraphicsRenderer();

      myEditorValue = null;
      if (dialog.isOK()) {
        String value = dialog.getResourceName();
        if (value != null) {
          myEditorValue = new AttributeEditorValue(dialog.getResourceName(), dialog.overwriteResource());
        }
      }

      if (myEditorValue == null) {
        ColorEditor.this.cancelCellEditing();
      } else {
        ColorEditor.this.stopCellEditing();
      }
    }
  }
}