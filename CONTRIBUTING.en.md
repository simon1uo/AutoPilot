# Contributing Guide (English)

> This project follows **Conventional Commits** and **Semantic Versioning**.
> 中文版见 [CONTRIBUTING.md](CONTRIBUTING.md).

## Commit Convention

Format:

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

### type
`feat` new feature · `fix` bug fix · `docs` docs only · `style` formatting · `refactor` refactor ·
`perf` performance · `test` tests · `build` build/deps · `ci` CI · `chore` misc · `revert` revert

### scope (optional)
`ui` `engine` `control` `model` `data` `settings` `build` `deps` `docs` `ci` `release`

### Examples
```
feat(engine): add "open URL" action type
fix(alarm): prevent crash when exact alarm permission missing
docs(readme): add English section
chore(deps): bump compose-bom
```

### Rules
- imperative mood, lowercase subject, no trailing period
- one logical change per commit
- breaking change: footer `BREAKING CHANGE: ...` or `!` after type

## Branches
- `main`: stable, always buildable
- feature: `feat/<desc>`, fix: `fix/<desc>`
- merge to `main` via Squash Merge to keep history clean

## Releasing
See [RELEASE.en.md](RELEASE.en.md). In short: SemVer versioning, tag `vX.Y.Z` triggers auto build & GitHub Release.
