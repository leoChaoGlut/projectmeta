# maven-project-metadata
### 作用: 
  解析git代码元数据,获取微服务各子应用之间的调用关系,细到方法级别. 结合neo4j等图数据库可对微服务的调用关系,频次,架构评分等进行分析.

### 例子请看: 
  ./projectmeta-executor/src/test/resources/test-example1.yml

### 特殊依赖: (解决无法解析链式调用的问题)
```
<dependency>
    <groupId>com.github.javaparser</groupId>
    <artifactId>javaparser-symbol-solver-core</artifactId>
    <version>3.15.3-SNAPSHOT-leo</version>
</dependency>
```
see https://github.com/leoChaoGlut/javaparser

### JavaParser局限性
~~1.无法解析链式调用的问题,已提交PR给javaparser,fixed~~

2.无法解析方法块内的lambda子块的方法调用
