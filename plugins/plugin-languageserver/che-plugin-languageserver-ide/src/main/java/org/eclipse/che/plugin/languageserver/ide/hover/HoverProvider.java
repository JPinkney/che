/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.languageserver.ide.hover;

import com.google.common.base.Joiner;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import elemental.dom.Element;
import elemental.html.Window;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.JsPromise;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.editor.orion.client.OrionHoverHandler;
import org.eclipse.che.ide.editor.orion.client.jso.OrionHoverContextOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionHoverOverlay;
import org.eclipse.che.ide.util.StringUtils;
import org.eclipse.che.ide.util.dom.Elements;
import org.eclipse.che.plugin.languageserver.ide.editor.LanguageServerEditorConfiguration;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;
import org.eclipse.che.plugin.languageserver.ide.util.DtoBuildHelper;
import org.eclipse.che.plugin.languageserver.ide.util.OpenFileInEditorHelper;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides hover LS functionality for Orion editor.
 *
 * @author Evgen Vidolob
 */
@Singleton
public class HoverProvider implements OrionHoverHandler {

  private final EditorAgent editorAgent;
  private final TextDocumentServiceClient client;
  private final DtoBuildHelper helper;
  private final OpenFileInEditorHelper openFileInEditorHelper;

  @Inject
  public HoverProvider(
      EditorAgent editorAgent,
      TextDocumentServiceClient client,
      DtoBuildHelper helper,
      OpenFileInEditorHelper openFileInEditorHelper) {
    this.editorAgent = editorAgent;
    this.client = client;
    this.helper = helper;
    this.openFileInEditorHelper = openFileInEditorHelper;
  }

  @Override
  public JsPromise<OrionHoverOverlay> computeHover(OrionHoverContextOverlay context) {
    EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
    if (activeEditor == null || !(activeEditor instanceof TextEditor)) {
      return null;
    }

    TextEditor editor = ((TextEditor) activeEditor);
    if (!(editor.getConfiguration() instanceof LanguageServerEditorConfiguration)) {
      return null;
    }

    LanguageServerEditorConfiguration configuration =
        (LanguageServerEditorConfiguration) editor.getConfiguration();
    if (configuration.getServerCapabilities().getHoverProvider() == null
        || !configuration.getServerCapabilities().getHoverProvider()) {
      return null;
    }

    Document document = editor.getDocument();
    TextDocumentPositionParams paramsDTO = helper.createTDPP(document, context.getOffset());

    Promise<Hover> promise = client.hover(paramsDTO);
    Promise<OrionHoverOverlay> then =
        promise.then(
            (Hover arg) -> {
              OrionHoverOverlay hover = OrionHoverOverlay.create();
              hover.setType("markdown");
              String content = renderContent(arg);
              // do not show hover with only white spaces
              if (StringUtils.isNullOrWhitespace(content)) {
                return null;
              }
              hover.setContent(content);
              return hover;
            });

    final Window window = Elements.getWindow();
    window.addEventListener(
        "mousedown",
        evt -> {
          Element anchorEle = (Element) evt.getTarget();

          // Register the onClick and open only if the element is in a tooltip.
          if (anchorEle.getOffsetParent().getClassList().contains("textViewTooltipOnHover")) {
            String hrefContent = anchorEle.getAttribute("href");
            Location uriLocation = getPositionFromURI(hrefContent);
            anchorEle.setOnclick(
                anchorEleClick -> {
                  anchorEleClick.preventDefault();
                  anchorEleClick.stopPropagation();
                  editor.getEditorWidget().hideTooltip();
                });
            this.openFileInEditorHelper.openLocation(uriLocation);
          }
        });
    return (JsPromise<OrionHoverOverlay>) then;
  }

  private String renderContent(Hover hover) {
    List<String> contents = new ArrayList<>();
    for (Either<String, MarkedString> dto : hover.getContents()) {
      if (dto.isLeft()) {
        // plain markdown text
        contents.add(dto.getLeft());
      } else {
        contents.add(dto.getRight().getValue());
      }
    }
    return Joiner.on("\n\n").join(contents);
  }

  public Location getPositionFromURI(String uri) {
    String pattern = "\\d+$";
    RegExp p = RegExp.compile(pattern);
    MatchResult m = p.exec(uri);

    Location uriLoc = new Location();
    uriLoc.setUri(uri);

    Position uriPos = new Position();

    // We have line information from the URI
    if (m != null && m.getGroupCount() > 0) {
      String lastMatch = m.getGroup(m.getGroupCount() - 1);
      Integer lineNumber = Integer.parseInt(lastMatch);

      // Case when line number is 0 for whatever reason
      if (lineNumber > 0) {
        uriPos.setLine(lineNumber - 1);
      } else {
        uriPos.setLine(lineNumber);
      }
    } else {
      uriPos.setLine(0);
    }

    uriLoc.setRange(new Range(uriPos, uriPos));

    return uriLoc;
  }
}
