# singularity-product 后端边界说明（Phase 0）

## 1. 模块定位

`singularity-product` 负责商品主数据管理，是独立微服务。

## 2. 当前职责（Phase 0）

- 提供独立启动入口与服务注册能力（Nacos Discovery）。
- 提供基础连通性接口：`GET /api/product/ping`。
- 对齐工程基线：Spring Boot + MyBatis XML + Flyway + Nacos 配置中心。

## 3. 不在当前阶段职责内

- 不处理库存扣减与回补（由 `singularity-stock` 负责）。
- 不处理下单事务（由 `singularity-order` 负责）。
- 不包含商品业务 CRUD（在 Phase 2 实现）。

## 4. 与其他服务交互约束

- 与 `singularity-stock`：后续仅做库存视图聚合（读）。
- 与 `singularity-order`：后续提供商品信息查询能力，不承载订单状态流转。
- 与 `singularity-user`：不直接依赖用户账户逻辑。

## 5. 运行与配置约定

- 服务名：`singularity-product`
- 默认端口：`8087`
- 数据库：`singularity_product`
- Flyway 目录：`classpath:db/migration`
- Mapper 目录：`classpath:mapper/*.xml`

## 6. 下一阶段入口

- Phase 1 将补齐商品主表与索引设计，并在 Flyway 中落地正式迁移脚本。
