import {useEffect, useReducer, useState} from "react";
import {useAuth} from "./useAuth.ts";
import {defaultGlobalFunction} from "./templates.ts";

type Action =
    { action: "save", ruleset: Ruleset } |
    { action: "reload", data: Record<string, Ruleset> } |
    { action: "committed", rulesetName: string } |
    { action: "revert", rulesetName: string }

export interface Ruleset {
    readonly name: string
    language: string
    code: string
}

export interface SavedDataStore {
    data: Record<string, Ruleset>,
    pendingSave: Record<string, Ruleset>
    update: (ruleset: Ruleset) => void
    save: (rulesetName: string) => void
    revert: (rulesetName: string) => void
    saving: boolean
}

interface SavedDataStoreState {
    state: Record<string, Ruleset>
    pendingSave: Record<string, Ruleset>
    reverted: Record<string, Ruleset>
}

export function useSavedDataStore() {
    const {auth} = useAuth();
    const [saving, setSaving] = useState(false);

    const [data, dispatch] = useReducer((state: SavedDataStoreState, action: Action) => {
        switch (action.action) {
            case "save":
                return {
                    ...state,
                    state: {
                        ...state.state,
                        [action.ruleset.name]: action.ruleset
                    },
                    pendingSave: {
                        ...state.pendingSave,
                        [action.ruleset.name]: action.ruleset
                    }
                }
            case "reload":
                return {
                    state: action.data,
                    reverted: action.data,
                    pendingSave: {}
                }
            case "committed": {
                const {[action.rulesetName]: savedRuleset, ...remainingPending} = state.pendingSave;
                return {
                    ...state,
                    reverted: {
                        ...state.reverted,
                        [action.rulesetName]: savedRuleset
                    },
                    pendingSave: remainingPending
                }
            }
            case "revert": {
                const {[action.rulesetName]: _, ...remainingPending} = state.pendingSave;
                return {
                    ...state,
                    state: {
                        ...state.state,
                        [action.rulesetName]: state.reverted[action.rulesetName]
                    },
                    pendingSave: remainingPending
                }
            }
        }
    }, {
        state: {},
        reverted: {},
        pendingSave: {}
    });

    const update = (ruleset: Ruleset) => {
        dispatch({ action: "save", ruleset: ruleset });
    }

    const save = async (rulesetName: string) => {
        if (!data.pendingSave[rulesetName]) return;

        setSaving(true);
        const response = await fetch("/api/rulesets", {
            method: "POST",
            body: JSON.stringify(data.pendingSave[rulesetName]),
            headers: {
                "Authorization": `Bearer ${auth}`
            }
        });
        setSaving(false);

        if (!response.ok) {
            alert(`There was a problem saving ${rulesetName}.`)
            return;
        }

        dispatch({action: "committed", rulesetName: rulesetName});
    }

    const revert = async (rulesetName: string) => {
        dispatch({action: "revert", rulesetName: rulesetName});
    }

    useEffect(() => {
        (async () => {
            const response = await fetch("/api/rulesets", {
                headers: {
                    "Authorization": `Bearer ${auth}`
                }
            });

            const responseObject: Record<string, Ruleset> = await response.json();
            if (!responseObject.global) {
                responseObject.global = {
                    language: "js",
                    name: "global",
                    code: defaultGlobalFunction
                }
            }
            dispatch({action: "reload", data: responseObject});
        })();
    }, []);

    return {
        data: data.state,
        pendingSave: data.pendingSave,
        update,
        save,
        saving,
        revert
    };
}