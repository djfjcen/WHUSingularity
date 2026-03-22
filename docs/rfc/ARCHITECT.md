# 总体架构说明

## 单点高并发框架部分

### 核心接口

- Slot
- Actor
- Allocator

抽象了多个 actor 同时竞争单个 slot 的场景。

通过 Allocator 实现类的多态，实现不同的 allocation policy。

## 商城 Demo 说明

### 技术栈

- 前端：React TS
- 后端：Spring Cloud
- 中间件：Redis、Rocket MQ
- 数据库：MySQL

### 业务微服务分离

- 用户服务（User）
- 商品信息服务（Product）
- 库存 & 订单服务（Stock & Order）`实际上，Order 的接口服务是独立的`

为了进行展示，总共分为 4 个服务区块，按照 resource 的种类区分。

#### 用户服务

实现简单的登录注册，以及用户信息的 **缓存** 读取。
因为需要进行跨服务读取用户信息以鉴权。

出于简单考虑，可能可以只将用户区分为 normal 和 admin。admin 可以进行商品的 CRUD 以及库存的设置。

#### 商品信息服务

实现简单的商品 CRUD 操作，以及对商品信息、图片等的 **缓存** 读取。

#### 库存 & 订单服务

使用秒杀框架的核心服务，使用 Producer + Comsumer 组合的方式，在 Producer 部分实现用户抢单，MQ 作为唯一信源，Comsumer 异步消费订单数据并落库。

### 其他创新点

提供 WebMCP，以适应当前能力边界愈发强大的 Agent 产品，致力于提供更加 AI 友好的平台能力。
