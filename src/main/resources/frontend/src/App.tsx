import {useSavedDataStore} from "./SavedDataStore.ts";
import {Sidebar} from "./Sidebar.tsx";
import {useState} from "react";
import Styles from "./App.module.css"
import {RulesetEditor} from "./RulesetEditor.tsx";

function App() {
    const dataStore = useSavedDataStore();
    const [currentRuleset, setCurrentRuleset] = useState("global");

  return (
    <div className={Styles.root}>
        <Sidebar data={dataStore} currentRuleset={currentRuleset} onCurrentRulesetChanged={setCurrentRuleset} />
        <RulesetEditor data={dataStore} ruleset={currentRuleset} key={currentRuleset} />
    </div>
  )
}

export default App
