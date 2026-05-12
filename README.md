# 电商全链路运营管理系统 (E-Mall)

基于 **Java 微服务 + Vue 3** 的电商全链路运营管理系统，覆盖商品、订单、库存、退货、仓储、供应商、统计等核心业务域。

## 系统架构

```
Vue 3 Frontend (:5173)
        |
        v
Spring Cloud Gateway (:9000)
        |-- product-service   (:9001)
        |-- order-service     (:9002)
        |-- inventory-service (:9003)
        |-- return-service    (:9004)
        |-- supplier-service  (:9005)
        |-- warehouse-service (:9006)
        `-- stats-service     (:9007)
```

## 技术栈

- **前端**: Vue 3, Vite, TypeScript, Axios
- **网关**: Spring Cloud Gateway MVC
- **后端**: Spring Boot 3, Spring Data JPA, Actuator
- **持久化**: H2 File Database
- **事务**: Saga 模式跨服务编排
- **构建**: Maven Wrapper

## 目录结构

```
E_mall/
├── RunMicroservices.java        # 统一启动入口（Java 微服务 + 前端）
├── java-backend/                # Spring Boot 多模块微服务后端
│   ├── common/                  # 公共模块（Saga、Feign、健康检查）
│   ├── gateway-service/         # API 网关
│   ├── product-service/         # 商品服务
│   ├── order-service/           # 订单服务
│   ├── inventory-service/       # 库存服务
│   ├── return-service/          # 退货服务
│   ├── supplier-service/        # 供应商服务
│   ├── warehouse-service/       # 仓库服务
│   └── stats-service/           # 统计服务
├── frontend/                    # Vue 3 管理后台
└── schema.sql                   # 数据库 Schema
```

## 快速启动

### 1. 安装前端依赖

```bash
cd frontend
npm install
```

### 2. 一键启动所有服务

```bash
# 编译 Java 微服务
./java-backend/mvnw.cmd -s java-backend/.mvn/settings.xml -DskipTests package

# 编译并启动
javac RunMicroservices.java
java RunMicroservices
```

启动器会自动：编译 Java 多模块工程 → 启动 7 个业务服务 + 1 个网关 → 启动前端开发服务器

### 3. 访问

- 前端: http://127.0.0.1:5173
- 网关: http://127.0.0.1:9000
- 健康检查: http://127.0.0.1:9000/health

### 4. 单独启动某个服务

```bash
cd java-backend
./mvnw.cmd -s .mvn/settings.xml -pl product-service spring-boot:run
```

## 核心业务链路

### 模拟下单 (Saga)
扣减库存 → 保存订单（失败时回补库存）

### 新增商品 (Saga)
保存商品 → 创建库存记录（失败时删除商品）

### 退货完成 (Saga)
回补库存 → 更新退货状态（失败时反向扣减库存）

## API 接口

所有接口通过网关统一暴露，前缀 `/api`：

| 模块 | 接口 |
|------|------|
| 商品 | `GET/POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` |
| 库存 | `GET /api/products/{id}/inventory`, `PUT /api/products/{id}/inventory` |
| 订单 | `GET /api/orders`, `POST /api/orders/simulate`, `GET /api/orders/{orderNo}` |
| 退货 | `GET /api/returns`, `PUT /api/returns/{id}/approve\|reject\|complete` |
| 仓库 | `GET /api/warehouses` |
| 统计 | `GET /api/stats/dashboard`, `GET /api/stats/orders/{status}` |

## 设计原则

- **单一职责**: 每个服务只负责一个业务域
- **服务自治**: 独立启动、独立持久化、独立健康检查
- **前后端分离**: 前端只通过网关访问后端
- **数据隔离**: 各服务维护本域数据，统计服务通过调用聚合
