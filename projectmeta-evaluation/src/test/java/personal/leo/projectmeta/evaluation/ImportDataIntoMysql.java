package personal.leo.projectmeta.evaluation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Before;
import org.junit.Test;
import personal.leo.projectmeta.evaluation.mapper.ClassRelationMapper;
import personal.leo.projectmeta.maven.plugin.index.ClassRelation;
import personal.leo.projectmeta.maven.plugin.util.ObjectUtil;

/**
 * @author 谦扬
 * @date 2019-06-02
 */
public class ImportDataIntoMysql {

    ClassRelationMapper classRelationMapper;

    @Before
    public void before() {
        PooledDataSource dataSource = new PooledDataSource(
            com.mysql.jdbc.Driver.class.getCanonicalName(),
            "jdbc:mysql://ali2:3306/test",
            "root",
            "root"
        );

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(ClassRelationMapper.class);
        configuration.setLogImpl(StdOutImpl.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        classRelationMapper = sqlSessionFactory.openSession(true).getMapper(ClassRelationMapper.class);
    }

    @Test
    public void saveData() throws IOException {
        classRelationMapper.createTableIfNotExists();

        final List<ClassRelation> classRelations = ObjectUtil.readJsonList(
            "/tmp/projectMeta/classRelations.json",
            ClassRelation.class
        );

        final int count = classRelationMapper.insertList(
            classRelations.stream().distinct().collect(Collectors.toList())
        );
        System.out.println(count);
    }

}
