//去除没有调用和被调用的class
MATCH(c1:Class)
  WHERE NOT (c1)-[:CALL]-(:Class)
DETACH DELETE c1;

//获取有边关系的类
MATCH (c1:Class)-[c:CALL]-(c2:Class)
RETURN count(DISTINCT c);

MATCH p = (c1:Class)-[c:CALL]-(c2:Class)
  WHERE NOT p
RETURN p