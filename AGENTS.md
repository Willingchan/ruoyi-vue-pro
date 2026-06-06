# OpenCode Agent Instructions

This file contains repository-specific context for `ruoyi-vue-pro` (Yudao).

## 🏗️ Architecture & Boundaries
* **Backend Only**: Despite the name `ruoyi-vue-pro`, this repository ONLY contains the Java Spring Boot backend. The front-end applications (Vue2/Vue3/Uniapp) are stored in separate external repositories (see `yudao-ui/*/README.md`).
* **Multi-Module**: 
  * `yudao-server`: The main executable Spring Boot application.
  * `yudao-framework`: Custom Spring Boot starters and foundational logic.
  * `yudao-module-*`: Business modules containing Controllers, Services, and DALs.
* **Code Generation**: Driven by the admin UI at runtime (`yudao-module-infra`), not via CLI commands.

## 🚀 Running & Building
* **Local Services**: Requires MySQL (3306) and Redis (6379). Use `docker-compose -f docker-compose-dev.yml up -d` to start them and automatically initialize the database schema from `sql/mysql/ruoyi-vue-pro.sql`.
* **Main Class**: `cn.iocoder.yudao.server.YudaoServerApplication` (inside `yudao-server`).
* **Build Command**: Use `mvn clean install -Dmaven.test.skip=true` to build the whole project. (Do not run tests during standard builds unless specifically requested, as some CI environments also skip them by default).

## 🛠️ Framework Conventions (Important)
* **MyBatis Plus Extensions**: 
  * **DO NOT** use standard MyBatis Plus `BaseMapper`. Mappers must extend `cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX<T>`.
  * **DO NOT** use standard `LambdaQueryWrapper`. Use `LambdaQueryWrapperX<T>` which supports concise methods like `.likeIfPresent()`, `.eqIfPresent()`, etc.
  * Encapsulate queries as `default` methods directly inside the Mapper interface (e.g., `default List<DeptDO> selectList(DeptListReqVO reqVO)`).
* **Object Mapping**: 
  * For simple object copying, use `cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(source, Target.class)`.
  * For complex mapping, use MapStruct (`@Mapper(componentModel = "spring")` or singleton `INSTANCE`).
* **API Definitions**: 
  * All controllers MUST return `cn.iocoder.yudao.framework.common.pojo.CommonResult<T>` via the `success()` static method.
  * Annotate controllers and DTOs with **Swagger 3** (SpringDoc) annotations: `@Tag`, `@Operation`, `@Schema`, and `@Parameter`.
* **Pagination**: Use `PageParam` (for requests) and `PageResult<T>` (for responses).
* **Exception Handling**: Throw business exceptions using `throw exception(ERROR_CODE);` (statically importing `cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception`).

## 🧪 Testing Quirks
* **Service Unit Tests**: 
  * MUST extend `BaseDbUnitTest` (if interacting with DB). This base class automatically provisions an H2 in-memory DB and cleans it after each test.
  * The convention is to use the H2 database for the module's own mappers, and Mockito (`@MockBean`) for dependencies calling out to other modules' Services.
