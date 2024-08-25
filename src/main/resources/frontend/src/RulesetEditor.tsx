import {Editor, useMonaco} from "@monaco-editor/react";
import {useEffect, useRef} from "react";
import {SavedDataStore} from "./SavedDataStore.ts";

export function RulesetEditor({data, ruleset}: {
    data: SavedDataStore
    ruleset: string
}) {
    const monaco = useMonaco();
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor>(null);

    useEffect(() => {

    }, [monaco]);

    const onMount = (editor: monaco.editor.IStandaloneCodeEditor) => {
        editorRef.current = editor;
    }

    const editorChange = (value: string | undefined) => {
        data.update({
            ...data.data[ruleset],
            name: ruleset,
            code: value!
        });
    }

    return <Editor height="100vh" defaultLanguage="javascript" defaultValue={data.data[ruleset]?.code ?? ""} onChange={editorChange} onMount={onMount} />
}