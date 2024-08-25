import Styles from "./Sidebar.module.css"
import {useAuth} from "./useAuth.ts";
import {SavedDataStore} from "./SavedDataStore.ts";
import {defaultHandlerTemplate} from "./templates.ts";



export function Sidebar({data, currentRuleset, onCurrentRulesetChanged}: {
    data: SavedDataStore,
    currentRuleset: string,
    onCurrentRulesetChanged: (ruleset: string) => void
}) {
    const {jurisdiction} = useAuth();

    const addNewRuleset = () => {
        const name = prompt("What are you calling the new ruleset?");
        if (name) {
            data.update({
                name: name,
                language: "js",
                code: defaultHandlerTemplate
            })
            onCurrentRulesetChanged(name);
        }
    }

    return <div className={Styles.sidebar}>
        <div className={Styles.title}>
            <div>{jurisdiction}</div>
            <div className={Styles.titleText}>Rulesets</div>
        </div>
        {Object.keys(data.data).sort((a: string, b: string) => a.localeCompare(b)).map(ruleset => <div key={ruleset}
                                                    className={`${Styles.item} ${ruleset === currentRuleset && Styles.selectedItem}`}
                                                    onClick={() => onCurrentRulesetChanged(ruleset)}>
            <span>{ruleset}</span>
            {data.pendingSave[ruleset] && <span className={Styles.saveButton} onClick={() => data.revert(ruleset)}>Revert</span>}
            {data.pendingSave[ruleset] && <span className={Styles.saveButton} onClick={() => data.save(ruleset)}>Save</span>}
        </div>)}
        <div className={`${Styles.item}`}
             onClick={addNewRuleset}>Addition of a new ruleset pls
        </div>
    </div>
}