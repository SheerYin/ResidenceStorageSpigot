
# ResidenceStorageSpigot

## 简介
ResidenceStorageSpigot 是一个通过 MySQL 存储 Residence 数据的 Spigot 插件项目。该项目主要使用 Kotlin 编写，旨在提供高效的领地数据管理解决方案。

## 特性
- 使用 MySQL 数据库存储领地数据，确保数据的安全性和可靠性
- 高效的数据查询与存储，提升服务器性能
- 完全兼容 Bukkit/Spigot/Paper 服务器
- 支持跨服传送领地，实现多服务器之间的数据共享
- 无视权限强制传送其他子服的玩家到指定领地
- 列出所有领地列表，方便管理和查看
- 理论上完全兼容原 Residence 的传送逻辑，支持公开领地功能
- 跨服领地数量限制，防止滥用
- 自动检测重复名称，避免冲突
- 提供友好的领地列表悬浮提示与点击操作
- 完全可自定义的语言文本，支持多语言环境
- 提供命令补全功能，提升使用体验
- 允许从 Residence 导入数据，方便迁移

## 安装
1. 克隆此仓库：
   ```bash
   git clone https://github.com/SheerYin/ResidenceStorageSpigot.git
   ```
2. 导入项目到您的开发环境。
3. 根据需求修改 `build.gradle.kts` 文件。
4. 使用 Gradle 构建项目：
   ```bash
   ./gradlew build
   ```
5. 将生成的 jar 文件放置到 Spigot 服务器的插件目录中。

## 使用
1. 将插件放入服务器并启动，忽略报错。
2. 关闭服务器，在 `plugins\ResidenceStorageSpigot\Residence\mysql.yml` 文件中修改 MySQL 配置。
3. 重新启动服务器，插件应正常运行。
4. 通过命令导入领地 （可选）

## 联系
- Tencent QQ Group: [244213805](https://qm.qq.com/q/hMINr3Y6is)

## 贡献
欢迎贡献代码和提交 Issue。如有任何问题，请联系项目维护者。

