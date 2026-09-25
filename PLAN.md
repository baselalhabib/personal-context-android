# Personal Context — Project Plan

## What this is

Android library that collects personal data and stores it locally. No network calls, no accounts, no cloud. Each data type is an Entity with a Connector that fills it.

## Architecture

Entity = one data type. Connector = the class that pulls raw Android data and maps it into that Entity, then writes it to Room.

```kotlin
interface ContextEntity {
    val id: String
    val timestamp: Long
    val source: String
}

interface Connector<T : ContextEntity> {
    val id: String
    val requiredPermissions: List<String>
    suspend fun collect(): List<T>
}
```

## Modules

```
personal-context-android/
├── core/
│   ├── entities/       # Room @Entity classes
│   ├── connectors/     # one per data source
│   ├── storage/        # Room db, DAOs
│   ├── query/           # read API for consuming apps
│   └── permissions/
├── app/                # sample app using the library
├── README.md
├── LICENSE
└── PLAN.md
```

## Candidate entities

| Entity | Source | Permission |
|---|---|---|
| `NoteEntity` | in-app / synced | none |
| `MessageEntity` | SMS provider | `READ_SMS` |
| `AppUsageEntity` | `UsageStatsManager` | Usage Access |
| `LocationEntity` | Fused Location Provider | location |
| `CallLogEntity` | call log provider | `READ_CALL_LOG` |
| `ContactEntity` | contacts provider | contacts |
| `EmailEntity` | IMAP or account access | later phase, unresolved |

## Rules

- No networking in `core`, ever
- Export (JSON) and full wipe are required from Phase 1, not optional
- One Entity + one Connector per data source, no cross-dependencies

## Query API

```kotlin
personalContext.query<MessageEntity>()
    .between(startTime, endTime)
    .execute()
```

## Roadmap

**Phase 0 — done / in progress**
- [x] Repo, README, LICENSE
- [ ] `core` / `app` module split
- [ ] `ContextEntity` interface + Room setup
- [ ] Permissions helper

**Phase 1 — first entities**
- [ ] `NoteEntity` + connector
- [ ] `MessageEntity` (SMS) + connector
- [ ] `AppUsageEntity` + connector
- [ ] Basic query API
- [ ] Sample app: connector list, toggle on/off, browse stored data

**Phase 2**
- [ ] `LocationEntity`, `CallLogEntity`, `ContactEntity`
- [ ] JSON export, wipe
- [ ] Background sync (WorkManager)
- [ ] Contributor doc: adding a new connector

**Phase 3**
- [ ] Aggregation/query helpers
- [ ] Local LLM querying the stored context
- [ ] `EmailEntity`

