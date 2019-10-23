# maven-project-metadata
### 作用: 解析git代码元数据,获取微服务各子应用之间的调用关系,细到方法级别. 结合neo4j等图数据库可对微服务的调用关系,频次,架构评分等进行分析.

### 例子请看: ./projectmeta-executor/src/test/resources/test-example1.yml

### 局限性:链式调用无法解析,如 ObjectUtils.getObject().execute() ,getObject() 的返回值如果是其它模块的类,则无法被解析
