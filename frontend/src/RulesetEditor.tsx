import {Editor, useMonaco} from "@monaco-editor/react";
import {SavedDataStore} from "./SavedDataStore.ts";
import Types from "./PassportTypes.ts?raw";
import {handlerPreamble} from "./templates.ts";
import {editor} from "monaco-editor";

export function RulesetEditor({data, ruleset}: {
    data: SavedDataStore
    ruleset: string
}) {
    const onMount = (editor: editor.IStandaloneCodeEditor, monaco: ReturnType<typeof useMonaco>) => {
        // @ts-expect-error Not a public API
        editor.setHiddenAreas([{
            startLineNumber: 1,
            endLineNumber: 7
        }])

        monaco?.languages.typescript.javascriptDefaults.addExtraLib(Types, "passportTypes.d.ts");
        monaco?.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
            ...monaco?.languages.typescript.javascriptDefaults.getDiagnosticsOptions(),
            noSemanticValidation: false,
            noSuggestionDiagnostics: false,
            noSyntaxValidation: false
        });
        monaco?.languages.typescript.javascriptDefaults.setCompilerOptions({
            ...monaco?.languages.typescript.javascriptDefaults.getCompilerOptions(),
            checkJs: true,
            alwaysStrict: true,
            strict: true
        });
        monaco?.editor.defineTheme("contemporary", {
            base: "vs-dark",
            inherit: true,
            rules: [
                {
                    token: "comment",
                    foreground: "#7f7f7f",
                }, {
                    token: "keyword",
                    foreground: "#ff9600"
                }, {
                    token: "variable",
                    foreground: "#009600"
                }, {
                    token: "string",
                    foreground: "#ff5959"
                }, {
                    token: "number",
                    foreground: "#b08000"
                }, {
                    token: "comment.doc",
                    foreground: "#607880"
                }
            ],
            colors: {
                "editor.foreground": "#ffffff",
                "editor.background": "#282828",
                "editor.lineHighlightBackground": "#3c3c3c"
            },
        });
        editor.updateOptions({
            lineNumbers: (lineNumber: number) => `${lineNumber - 7}`,
            fontFamily: "JetBrains Mono",
            theme: "contemporary"
        })

        editor.focus();
    }

    const editorChange = (value: string | undefined) => {
        data.update({
            ...data.data[ruleset],
            name: ruleset,
            code: value!.replace(handlerPreamble, "")
        });
    }

    return <Editor height="100vh" defaultLanguage="javascript" defaultValue={`${handlerPreamble}${data.data[ruleset]?.code ?? ""}`} theme={"contemporary"} onChange={editorChange} onMount={onMount} />
}
