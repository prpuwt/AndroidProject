# 个人记账本 (MyBill)

一款 Android 个人记账应用，支持账单记录、分类统计、搜索、回收站、应用锁等功能。

## 功能特性

- 账单的增删改查
- 按类别/人员/时间的统计分析
- 账单搜索
- 回收站（软删除，支持恢复）
- 应用锁（图案锁）
- 数据导出（CSV）

## 技术栈

- Java 11
- Android SDK
- Material 3 (Material Design)
- SQLite (本地数据库)
- ConstraintLayout

## 构建

```bash
./gradlew assembleDebug
```

## 运行测试

```bash
./gradlew test
```
