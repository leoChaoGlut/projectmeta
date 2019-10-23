package personal.leo.projectmeta.evaluation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import personal.leo.projectmeta.maven.plugin.index.ClassRelation;

/**
 * @author 谦扬
 * @date 2019-10-23
 */
public interface ClassRelationMapper {
    @Select("create table if not exists class_relation\n"
        + "(\n"
        + "    from_product varchar(255) not null,\n"
        + "    from_app     varchar(255) not null,\n"
        + "    from_module  varchar(255) not null,\n"
        + "    from_package varchar(255) not null,\n"
        + "    from_class   varchar(255) not null,\n"
        + "    to_product   varchar(255) not null,\n"
        + "    to_app       varchar(255) not null,\n"
        + "    to_module    varchar(255) not null,\n"
        + "    to_package   varchar(255) not null,\n"
        + "    to_class     varchar(255) not null,\n"
        + "    to_method    varchar(255) not null,\n"
        + "    constraint relation_unique\n"
        + "        unique (from_product, from_app, from_module, from_package, from_class, to_product, to_app, "
        + "to_module,\n"
        + "                to_package, to_class, to_method)\n"
        + ");\n")
    void createTableIfNotExists();

    @Insert(
        "<script>"
            + "insert into class_relation values"
            + "<foreach item='i' collection='classRelations' separator=','>"
            + "("
            + "#{i.fromProduct},#{i.fromApp},#{i.fromModule},#{i.fromPackage},#{i.fromClass},"
            + "#{i.toProduct},#{i.toApp},#{i.toModule},#{i.toPackage},#{i.toClass},"
            + "#{i.toMethod}"
            + ")"
            + "</foreach>"
            + "</script>"
    )
    int insertList(@Param("classRelations") List<ClassRelation> classRelations);
}
