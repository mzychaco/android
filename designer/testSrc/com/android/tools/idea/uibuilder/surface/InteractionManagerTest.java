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
package com.android.tools.idea.uibuilder.surface;

import com.android.tools.idea.uibuilder.LayoutTestCase;
import com.android.tools.idea.uibuilder.SyncNlModel;
import com.android.tools.idea.uibuilder.model.Coordinates;
import com.android.tools.idea.uibuilder.model.NlModel;
import com.android.tools.idea.uibuilder.model.SelectionModel;
import com.android.tools.idea.uibuilder.scene.SceneComponent;
import com.android.tools.idea.uibuilder.scene.draw.DisplayList;
import com.android.tools.idea.uibuilder.util.NlTreeDumper;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlFile;
import org.intellij.lang.annotations.Language;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import static com.android.SdkConstants.CONSTRAINT_LAYOUT;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.TEXT_VIEW;
import static com.android.tools.idea.uibuilder.LayoutTestUtilities.*;

public class InteractionManagerTest extends LayoutTestCase {

  public void testDragAndDrop() throws Exception {
    // Drops a fragment (xmlFragment below) into the design surface (via drag & drop events) and verifies that
    // the resulting document ends up modified as expected.
    SyncNlModel model = model("linear.xml", component(LINEAR_LAYOUT)
      .withBounds(0, 0, 1000, 1000)
      .withAttribute("android:orientation", "vertical")).build();

    ScreenView screenView = createScreen(model);
    DesignSurface designSurface = screenView.getSurface();
    InteractionManager manager = createManager(designSurface);

    @Language("XML")
    String xmlFragment = "" +
                         "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                         "     android:layout_width=\"wrap_content\"\n" +
                         "     android:layout_height=\"wrap_content\"\n" +
                         "     android:text=\"Hello World\"\n" +
                         "/>";
    Transferable transferable = createTransferable(DataFlavor.stringFlavor, xmlFragment);
    dragDrop(manager, 0, 0, 100, 100, transferable);
    Disposer.dispose(model);

    String expected = "NlComponent{tag=<LinearLayout>, bounds=[0,150:2x2, instance=0}\n" +
                      "    NlComponent{tag=<TextView>, bounds=[0,150:2x2, instance=1}";
    assertEquals(expected, new NlTreeDumper().toTree(model.getComponents()));
  }

  public void testLinearLayoutCursorHoverComponent() throws Exception {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getCenterX()),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  public void testLinearLayoutCursorHoverComponentHandle() throws Exception {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    SelectionModel selectionModel = screenView.getModel().getSelectionModel();
    selectionModel.setSelection(ImmutableList.of(textView.getNlComponent()));
    selectionModel.getHandles(textView.getNlComponent());
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawX() + textView.getDrawWidth()),
                         Coordinates.getSwingYDip(screenView, textView.getDrawY() + textView.getDrawHeight()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  public void testLinearLayoutCursorHoverRoot() throws Exception {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawHeight() + textView.getDrawY() + 20),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(Cursor.getDefaultCursor());
  }

  public void testLinearLayoutCursorHoverSceneHandle() throws Exception {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    manager.updateCursor(screenView.getX() + screenView.getSize().width,
                         screenView.getY() + screenView.getSize().height);
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  private InteractionManager setupLinearLayoutCursorTest() {
    SyncNlModel model = model("linear.xml", component(LINEAR_LAYOUT)
      .withBounds(0, 0, 100, 100)
      .withAttribute("android:orientation", "vertical")
      .children(
        component(TEXT_VIEW)
          .id("@+id/textView")
          .withBounds(0, 0, 100, 100)
          .wrapContentWidth()
          .wrapContentHeight())).build();

    NlDesignSurface surface = model.getSurface();
    Mockito.when(surface.getScale()).thenReturn(1.0);

    ScreenView screenView = new ScreenView(surface, ScreenView.ScreenViewType.NORMAL, model);
    Mockito.when(surface.getSceneView(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(screenView);
    InteractionManager manager = createManager(surface);
    surface.getScene().buildDisplayList(new DisplayList(), 0);
    return manager;
  }

  public void testConstraintLayoutCursorHoverComponent() throws Exception {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getCenterX()),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  public void testConstraintLayoutCursorHoverComponentHandle() throws Exception {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    SelectionModel selectionModel = screenView.getModel().getSelectionModel();
    selectionModel.setSelection(ImmutableList.of(textView.getNlComponent()));
    selectionModel.getHandles(textView.getNlComponent());
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawX() + textView.getDrawWidth()),
                         Coordinates.getSwingYDip(screenView, textView.getDrawY() + textView.getDrawHeight()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  public void testConstraintLayoutCursorHoverRoot() throws Exception {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawHeight() + textView.getDrawY() + 20),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(Cursor.getDefaultCursor());
  }

  public void testConstraintLayoutCursorHoverSceneHandle() throws Exception {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    manager.updateCursor(screenView.getX() + screenView.getSize().width,
                         screenView.getY() + screenView.getSize().height);
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  private InteractionManager setupConstraintLayoutCursorTest() {
    SyncNlModel model = model("constraint.xml", component(CONSTRAINT_LAYOUT)
      .withBounds(0, 0, 1000, 1000)
      .matchParentWidth()
      .matchParentHeight()
      .children(
        component(TEXT_VIEW)
          .id("@+id/textView")
          .withBounds(0, 0, 100, 100)
          .wrapContentWidth()
          .wrapContentHeight())).build();

    NlDesignSurface surface = model.getSurface();
    Mockito.when(surface.getScale()).thenReturn(1.0);

    ScreenView screenView = new ScreenView(surface, ScreenView.ScreenViewType.NORMAL, model);
    Mockito.when(surface.getSceneView(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(screenView);
    InteractionManager manager = createManager(surface);
    screenView.getScene().buildDisplayList(new DisplayList(), 0);
    return manager;
  }
}