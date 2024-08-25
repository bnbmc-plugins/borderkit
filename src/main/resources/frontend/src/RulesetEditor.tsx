import {Editor, useMonaco} from "@monaco-editor/react";
import {useRef} from "react";
import {SavedDataStore} from "./SavedDataStore.ts";
import Types from "./PassportTypes.ts?raw";
import {handlerPreamble} from "./templates.ts";

export function RulesetEditor({data, ruleset}: {
    data: SavedDataStore
    ruleset: string
}) {
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor>(null);

    const onMount = (editor: monaco.editor.IStandaloneCodeEditor, monaco: ReturnType<typeof useMonaco>) => {
        editorRef.current = editor;
        editor.setHiddenAreas([{
            startLineNumber: 1,
            endLineNumber: 7
        }])
        editor.updateOptions({
            lineNumbers: (lineNumber: number) => lineNumber - 7
        })

        monaco?.languages.typescript.javascriptDefaults.addExtraLib(Types, "passportTypes.d.ts");
        monaco?.languages.typescript.javascriptDefaults.addExtraLib(`declare const handler: HandlerFunction;`, "handler.d.ts");
    }

    const editorChange = (value: string | undefined) => {
        data.update({
            ...data.data[ruleset],
            name: ruleset,
            code: value!.replace(handlerPreamble, "")
        });
    }

    return <Editor height="100vh" defaultLanguage="javascript" defaultValue={`${handlerPreamble}${data.data[ruleset]?.code ?? ""}`} onChange={editorChange} onMount={onMount} />
}
