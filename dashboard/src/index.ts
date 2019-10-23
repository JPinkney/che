/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
export interface ICheWindow extends Window {
  $: Function;
  jQuery: Function;
  Monaco: any;
  yamlLanguageServer: any;
  monacoConversion: any;
  MonacoEnvironment: any;
  url: any;
  jsyaml: Object;
  jsonlint?: Object;
}

declare const require: Function;
/* tslint:disable */
const windowObject = <ICheWindow>window;
const $ = require('jquery');
windowObject.$ = $;
windowObject.jQuery = $;
windowObject.jsyaml = require('js-yaml');
if (windowObject.jsonlint === undefined) {
  windowObject.jsonlint = require('jsonlint');
}
windowObject.yamlLanguageServer = require('yaml-language-server');
windowObject.monacoConversion = require('monaco-languageclient/lib/monaco-converter');
windowObject.url = require('url');
windowObject.Monaco = require('monaco-editor-core/esm/vs/editor/editor.main');

/* tslint:enable */
import 'angular';
import 'angular-animate';
import 'angular-cookies';
import 'angular-file-upload';
import 'angular-touch';
import 'angular-sanitize';
import 'angular-resource';
import 'angular-route';
import 'angular-ui-bootstrap';
import 'angular-aria';
import 'angular-material';
import 'angular-messages';
import 'angular-moment';
import 'angular-filter';
import 'angular-uuid4';
import 'ng-lodash';
import '../node_modules/angular-gravatar/build/md5.min.js';
import '../node_modules/angular-gravatar/build/angular-gravatar.min.js';
import '../node_modules/angular-websocket/dist/angular-websocket.min.js';

// include UD app
import './app/index.module';

// set up monaco initially
(window as any).MonacoEnvironment = {
    getWorkerUrl: function (moduleId, label) {
        return 'app/editor.worker.module.js';
    }
};

const monaco = (window as any).Monaco;
monaco.editor.defineTheme('che', {
    base: 'vs', // can also be vs-dark or hc-black
    inherit: true, // can also be false to completely replace the builtin rules
    rules: [
        { token: 'string.yaml', foreground: '000000' },
        { token: 'comment', foreground: '777777'}
    ],
    colors: {
        'editor.lineHighlightBackground': '#f0f0f0',
        'editorLineNumber.foreground': '#aaaaaa',
        'editorGutter.background': '#f8f8f8'
    }
});

monaco.editor.setTheme('che');

// tslint:disable-next-line: no-var-requires
const mod = require('monaco-languages/release/esm/yaml/yaml.js');

const languageID = 'yaml';

// register the YAML language with Monaco
monaco.languages.register({
    id: languageID,
    extensions: ['.yaml', '.yml'],
    aliases: ['YAML'],
    mimetypes: ['application/json']
});

monaco.languages.setMonarchTokensProvider(languageID, mod.language);
monaco.languages.setLanguageConfiguration(languageID, mod.conf);
