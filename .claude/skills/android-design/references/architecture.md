# From design to code: the architecture contract

**Applies to:** apps built with Google's recommended architecture (UI layer in Compose, ViewModels, repositories). This file covers only what makes a design implementable; it is not a guide to app architecture.

A design that lists its states and events maps straight onto how Android apps are built. A design that relies on "show a toast when the save finishes" or "this screen remembers where you were" without saying how fights the architecture and gets built differently from what was drawn.

## Contents

1. Four principles, in design terms
2. The state table method
3. Events
4. What survives what
5. Offline
6. Navigation
7. Skeleton
8. Sources

---

## 1. Four principles, in design terms

- **Separation of concerns:** the screen draws state; it doesn't decide business rules.
- **Drive the UI from data models:** what's on screen comes from data that survives lifecycle events, so rotation or a fold never loses work.
- **Single source of truth:** each type of data has one owner; two screens showing the same item show the same value.
- **Unidirectional data flow:** state flows down to the screen, events flow up from it.

## 2. The state table method

For every screen, write a table before the mockups are final:

| UI state field | Example | Shown how | Saveable? |
|---|---|---|---|
| content | list of messages | list | from data (no) |
| `isLoading` | true while refreshing | indicator | no |
| `userMessage` | "Couldn't send" | snackbar until shown | no |
| `selectedId` | message 42 | detail pane | yes (ID only) |
| offline | true | banner, cached content | no |
| permission state | microphone denied | voice button disabled with note | no |

- All of these are fields of **one immutable UI state** for the screen (Google names it after the screen, e.g. `InboxUiState`). Loading and errors are fields, not popups fired from somewhere.
- Every control maps to a named **event**: `on` + verb + target (`onMessageSelected`, `onSendClicked`).
- If a mockup shows something that isn't in the table, the table is incomplete.

## 3. Events

- An event that needs business logic goes to the ViewModel; one that only changes UI (opening a menu, scrolling) is handled in the UI.
- The ViewModel doesn't fire one-off events at the screen. To show a message, it sets `userMessage` in the state; the screen shows it and reports back (`userMessageShown()`), which clears it. A design that needs "a toast fired by business logic" should become a state field.
- Guard against double actions: navigation clicks ignore repeats while the screen isn't resumed, and submit buttons disable while the request runs.
- Work longer than about 5 seconds runs as background work (WorkManager), not in the screen; design its progress as a notification or a status the screen reads back.

## 4. What survives what

| Mechanism | Survives rotation or fold | Survives the process being killed | Survives the person closing the app | Use for |
|---|---|---|---|---|
| ViewModel | yes | no | no | screen state, loaded data |
| Saved state (`rememberSaveable`, `SavedStateHandle`) | yes | yes | no | scroll position, selected item ID, text being typed, a half-made choice; small values only, store IDs not objects |
| Persistent storage | yes | yes | yes | drafts, settings, user data |

Decide per field which row it belongs to; that answers "does the screen come back the way I left it?" (`State_Preservation` in `quality.md`).

## 5. Offline

- **Reads:** the local database is the source of truth, so screens show data immediately without network.
- **Writes**, decided per action:
  - *online-only* (a bank transfer): disable the action offline or explain why it can't be done now;
  - *queued* (analytics): nothing to show;
  - *local first, then sync* (to-do items): the change appears at once; with "last write wins", say where a conflict could overwrite someone's edit.

## 6. Navigation

Google recommends a single activity with **Navigation 3** for Compose apps: the back stack is a list the app owns, and scene strategies show several entries at once on large windows (list and detail side by side). Check the Navigation 3 release notes for the current version and API names before naming them in a spec.

## 7. Skeleton

```kotlin
// One immutable state per screen; the screen collects it with the lifecycle.
data class InboxUiState(
    val messages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val selectedId: Long? = null,
)

class InboxViewModel(repository: InboxRepository) : ViewModel() {
    val uiState: StateFlow<InboxUiState> = repository.messages()
        .map { InboxUiState(messages = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState(isLoading = true))
}

interface InboxRepository { fun messages(): Flow<List<String>> }
```

In the screen: `val state by viewModel.uiState.collectAsStateWithLifecycle()`, then pass only the values each composable needs (`Header(title, subtitle)`, not the whole state).

Google's recommendations page sorts its advice into *strongly recommended*, *recommended* and *optional*. Naming conventions are optional; don't present them as rules.

## 8. Sources

- Guide to app architecture: https://developer.android.com/topic/architecture
- UI layer: https://developer.android.com/topic/architecture/ui-layer
- UI events: https://developer.android.com/topic/architecture/ui-layer/events
- State holders: https://developer.android.com/topic/architecture/ui-layer/stateholders
- State production: https://developer.android.com/topic/architecture/ui-layer/state-production
- Saving UI state: https://developer.android.com/topic/libraries/architecture/saving-states
- Offline-first: https://developer.android.com/topic/architecture/data-layer/offline-first
- Recommendations: https://developer.android.com/topic/architecture/recommendations
- State in Compose: https://developer.android.com/develop/ui/compose/state
- Navigation 3: https://developer.android.com/guide/navigation/navigation-3
