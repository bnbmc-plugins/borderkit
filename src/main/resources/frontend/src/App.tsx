import {useSavedDataStore} from "./SavedDataStore.ts";
import {Sidebar} from "./Sidebar.tsx";
import {useState} from "react";
import Styles from "./App.module.css"
import {RulesetEditor} from "./RulesetEditor.tsx";
import {useHotkeys} from "react-hotkeys-hook";
import { useBeforeunload } from "react-beforeunload";

function App() {
    const dataStore = useSavedDataStore();
    const [currentRuleset, setCurrentRuleset] = useState("global");

    useHotkeys("ctrl+s", () => dataStore.save(currentRuleset), {
        enableOnContentEditable: true,
        enableOnFormTags: true,
        preventDefault: true
    });

    useBeforeunload(Object.keys(dataStore.pendingSave).length ? e => e.preventDefault() : undefined);

  return (
    <div className={Styles.root}>
        <Sidebar data={dataStore} currentRuleset={currentRuleset} onCurrentRulesetChanged={setCurrentRuleset} />
        <RulesetEditor data={dataStore} ruleset={currentRuleset} key={currentRuleset} />
    </div>
  )
}

export default App
