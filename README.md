# Personal Context

**Personal Context** is an open-source Android library (with a sample app) that lets you collect and store your own personal data — locally, privately, and entirely on-device. No cloud, no accounts, no data ever leaving your phone.

Think of it as the foundation for a "Siri-like" personal context layer, but transparent, open, and fully under your control.

## Why

Most personal-data tools (habit trackers, location history, usage stats) ship your data to someone else's server. Personal Context flips that: everything is collected, stored, and queried locally using on-device storage only.

## Status

🚧 **Early development.** This project is just getting started — architecture and scope are still being defined. Not yet ready for production use.

## Goals

- Collect personal data (e.g. location visits, app usage) via modular, optional "connectors"
- Store everything locally (SQLite/Room) — nothing synced, nothing uploaded
- Provide a simple query API so other apps (or a future local LLM) can read this context
- Ship as a reusable Android library, with a minimal sample app demonstrating usage

## Project Structure

```
personal-context-android/
├── core/      # Library module — data collection, local storage, query API
├── app/       # Sample app demonstrating the library
```

*(Structure will evolve as the project takes shape.)*

## Getting Started

Clone the repo and open it in Android Studio:

```bash
git clone https://github.com/<your-username>/personal-context-android.git
```

More setup instructions coming soon.

## Contributing

This project is designed to be contributor-friendly from the start. Good first contributions will include adding new data connectors, improving the storage layer, and building out the sample app. Issues and PRs welcome once the initial architecture is in place.

## Privacy

All data stays on-device. Personal Context does not collect, transmit, or store any data on external servers. Full data export and wipe controls are a core design requirement, not an afterthought.

## License

[MIT](LICENSE)