/**
 * Minimal standards-based store implementing the used Redux Toolkit API subset.
 * Case handlers receive a structuredClone of the current slice and may mutate it or return a replacement.
 */

/**
 * @param {string} type
 */
export const createAction = type => {
    const actionCreator = payload => ({ type, payload });
    actionCreator.type = type;
    actionCreator.toString = () => type;
    actionCreator.match = action => action.type === type;
    return actionCreator;
};

/**
 * @param {object} initialState
 * @param {(builder: { addCase: Function }) => void} builderCallback
 */
export const createReducer = (initialState, builderCallback) => {
    const cases = new Map();
    const builder = {
        addCase(actionCreator, handler) {
            cases.set(String(actionCreator), handler);
            return builder;
        }
    };
    builderCallback(builder);
    return (state = initialState, action) => {
        const handler = cases.get(action.type);
        if (!handler) return state;
        const draft = structuredClone(state);
        return handler(draft, action) ?? draft;
    };
};

const combine = reducers => (state = {}, action) =>
    Object.fromEntries(Object.entries(reducers)
        .map(([name, reducer]) => [name, reducer(state[name], action)]));

/**
 * @param {{ reducer: object | Function, preloadedState?: object }} config
 */
export const configureStore = ({ reducer, preloadedState }) => {
    const rootReducer = typeof reducer === "function" ? reducer : combine(reducer);
    let state = rootReducer(preloadedState, { type: "@@init" });
    const listeners = new Set();
    return {
        getState: () => state,
        dispatch(action) {
            state = rootReducer(state, action);
            listeners.forEach(listener => listener());
            return action;
        },
        subscribe(listener) {
            listeners.add(listener);
            return () => listeners.delete(listener);
        }
    };
};
