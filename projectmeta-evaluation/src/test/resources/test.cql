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
;


MATCH(a:App {name: 'component-a'})
MATCH p = (ac:Class)-[c:CALL]-(oc:Class)
  WHERE (ac)-[:BELONG_TO]->(:Package)-[:BELONG_TO]->(:Module)-[:BELONG_TO]->(a)
  AND NOT (oc)-[:BELONG_TO]->(:Package)-[:BELONG_TO]->(:Module)-[:BELONG_TO]->(a)
RETURN p;