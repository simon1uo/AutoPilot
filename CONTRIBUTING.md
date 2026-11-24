# 贡献指南 / Contributing Guide

> 本项目遵循 **Conventional Commits** 提交规范、**Semantic Versioning** 版本规范。
> This project follows **Conventional Commits** and **Semantic Versioning**.

---

## 提交规范 / Commit Convention

提交信息格式 / Commit message format:

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

### type 类型

| type | 含义 / Meaning |
|------|----------------|
| `feat` | 新功能 / A new feature |
| `fix` | 修复 bug / A bug fix |
| `docs` | 仅文档变更 / Documentation only |
| `style` | 代码格式(不影响逻辑) / Formatting, no code change |
| `refactor` | 重构(非新增功能/非修 bug) / Code refactor |
| `perf` | 性能优化 / Performance improvement |
| `test` | 增加或修改测试 / Adding or updating tests |
| `build` | 构建系统或依赖变更 / Build system or dependencies |
| `ci` | CI 配置变更 / CI configuration |
| `chore` | 其它杂项 / Other chores |
| `revert` | 回滚 / Revert a previous commit |

### scope 作用域（可选）

常用：`ui` `engine` `control` `model` `data` `build` `deps` `docs` `ci`

### 示例 / Examples

```
feat(engine): 新增“打开网页”动作类型
fix(alarm): 修复无精确闹钟权限时的崩溃
docs(readme): 补充英文说明
refactor(ui): 抽出 TaskCard 组件
chore(deps): 升级 compose-bom 到 2024.12.01
```

### 规则 / Rules

- subject 用祈使句、首字母小写、结尾不加句号 / imperative mood, lowercase, no trailing period
- 一次提交只做一件事 / one logical change per commit
- 破坏性变更在 footer 写 `BREAKING CHANGE: ...`，或在 type 后加 `!` / mark breaking changes
- 中文或英文均可，但同一条提交内保持一致 / Either language, consistent within one commit

---

## 分支规范 / Branch Convention

- `main`：稳定分支，始终可构建 / stable branch, always buildable
- 功能分支：`feat/<short-desc>`、修复分支：`fix/<short-desc>`
- 分支合并到 `main` 时使用 Squash Merge，保留规范的 commit message

---

## 发包规范 / Release Convention

详见 [RELEASE.md](RELEASE.md)。核心原则 / See RELEASE.md. Key points:

- 版本号遵循 **SemVer**：`MAJOR.MINOR.PATCH`
- 打 tag 触发 GitHub Actions 自动构建并发布 Release
- Tag 格式：`vX.Y.Z`（如 `v0.2.0`）
