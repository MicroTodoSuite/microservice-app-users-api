## [1.5.2](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.5.1...v1.5.2) (2026-09-21)


### Bug Fixes

* **ci:** pin the promotion workflow past the conventions repair ([#33](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/33)) ([35cf367](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/35cf367b5b55c51da0dfcc94c322ccd695045a80)), closes [#191](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/191) [#192](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/192) [#193](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/193) [#195](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/195) [#196](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/196) [#198](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/198) [MicroTodoSuite/microservice-app-gitops#205](https://github.com/MicroTodoSuite/microservice-app-gitops/issues/205)

## [1.5.1](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.5.0...v1.5.1) (2026-09-14)


### Bug Fixes

* **ci:** repoint to the latest .github reusable workflow refs ([#30](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/30)) ([7b7c136](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/7b7c136b6d9afd0d0b24119c066f6ab724d7be09)), closes [#142](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/142) [#19](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/19)

# [1.5.0](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.4.0...v1.5.0) (2026-09-13)


### Features

* **tracing:** export users-api traces over otlp without tracing probes ([#27](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/27)) ([fbeeb89](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/fbeeb89ee087d258a94f2ea68da3cada4c95bb77)), closes [MicroTodoSuite/microservice-app-gitops#123](https://github.com/MicroTodoSuite/microservice-app-gitops/issues/123) [#123](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/123) [#123](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/123)

# [1.4.0](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.3.0...v1.4.0) (2026-09-09)


### Bug Fixes

* **ci:** target replacement AWS account ([d82bc93](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/d82bc9370496975565c6852a7c215bf8b570c148))
* **security:** update embedded Tomcat ([6bf24ad](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/6bf24ad8159663c3524b82bbbd663bc0d48c64f0))


### Features

* **us3:** implement users-api operational contract ([6b404fc](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/6b404fc8f2e63f4cf18a737c57b6ad2cb3da23be))

# [1.3.0](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.2.2...v1.3.0) (2026-08-24)


### Features

* enable histogram buckets for http_server_requests latency ([dce7a17](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/dce7a170106535eff0998c830a36904c5721ad39))

## [1.2.2](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.2.1...v1.2.2) (2026-08-24)


### Bug Fixes

* **ci:** publish images to the migrated AWS account ([f6143bf](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/f6143bf3ef7629a7bc6a0b3895f12e4f3d3f5294))

## [1.2.1](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.2.0...v1.2.1) (2026-08-19)


### Bug Fixes

* correct the spring boot product name typo in the readme ([#13](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/13)) ([82cb8f7](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/82cb8f7bcd668ca782e2b3f2492d5ae38b88bc64))

# [1.2.0](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.1.0...v1.2.0) (2026-08-19)


### Bug Fixes

* correct the users endpoint path syntax in the readme ([#10](https://github.com/MicroTodoSuite/microservice-app-users-api/issues/10)) ([d450444](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/d45044435d1220158e265134178620b40990ac09))
* use numeric runtime identity ([e0d53fb](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/e0d53fb0948c6ffef053c6f121b0311be7e96e58))


### Features

* initialize specify configuration and add project constitution documentation ([8e6ea77](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/8e6ea7785fdb265249ce5477cb04258564e80863))

# [1.1.0](https://github.com/MicroTodoSuite/microservice-app-users-api/compare/v1.0.0...v1.1.0) (2025-04-25)


### Features

* **pipeline:** update pipeline ([0cb7ef9](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/0cb7ef9afcb8cd4ff2573fe7aeb048ea74d6b2aa))

# 1.0.0 (2025-04-25)


### Bug Fixes

* **pipeline:** update pipeline ([2010e7b](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/2010e7b7e941df8833da70b5e01a4566d755767f))


### Features

* add microservice for users api ([f95189b](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/f95189b74ae08a0c5cfa768f6cd81c02359bf914))
* add semantic release pipeline ([cc86ee5](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/cc86ee51232896a02e09099131fc12044c763292))
* **pipeline:** add pipeline of development ([35e9a53](https://github.com/MicroTodoSuite/microservice-app-users-api/commit/35e9a5336fea457f9caa15912f99cca1f806779b))
