package com.example.kidhackbackend.global.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스키마에 Oracle 예약어를 쓴 테이블/컬럼이 없는지 본다.
 *
 * <p><b>왜 필요한가.</b> 로컬과 CI 는 H2, 운영은 RDS Oracle 이다. 두 엔진의 예약어
 * 집합이 달라서 H2 에서는 만들어지는 컬럼이 Oracle 에서는 ORA-00904 로 죽는다.
 * 즉 이 부류의 실수는 <b>테스트를 100% 통과한 뒤 배포 시점에</b> 드러난다.
 * (실제로 화면 캡처의 {@code trigger} 컬럼이 이 경로로 새어 나갔다.)
 *
 * <p>Hibernate 의 {@code auto_quote_keyword} 는 기본이 꺼짐이라 자동 인용도 없다.
 * 걸리면 {@code @Column(name = ...)} / {@code @Table(name = ...)} 으로 이름을 바꾼다.
 */
@DataJpaTest
class OracleReservedWordColumnTest {

    /** Oracle SQL Language Reference, Appendix "Oracle SQL Reserved Words". */
    private static final Set<String> ORACLE_RESERVED = Set.of(
            "ACCESS", "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUDIT",
            "BETWEEN", "BY", "CHAR", "CHECK", "CLUSTER", "COLUMN", "COLUMN_VALUE",
            "COMMENT", "COMPRESS", "CONNECT", "CREATE", "CURRENT", "DATE", "DECIMAL",
            "DEFAULT", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "EXCLUSIVE",
            "EXISTS", "FILE", "FLOAT", "FOR", "FROM", "GRANT", "GROUP", "HAVING",
            "IDENTIFIED", "IMMEDIATE", "IN", "INCREMENT", "INDEX", "INITIAL", "INSERT",
            "INTEGER", "INTERSECT", "INTO", "IS", "LEVEL", "LIKE", "LOCK", "LONG",
            "MAXEXTENTS", "MINUS", "MLSLABEL", "MODE", "MODIFY", "NESTED_TABLE_ID",
            "NOAUDIT", "NOCOMPRESS", "NOT", "NOWAIT", "NULL", "NUMBER", "OF", "OFFLINE",
            "ON", "ONLINE", "OPTION", "OR", "ORDER", "PCTFREE", "PRIOR", "PUBLIC", "RAW",
            "RENAME", "RESOURCE", "REVOKE", "ROW", "ROWID", "ROWNUM", "ROWS", "SELECT",
            "SESSION", "SET", "SHARE", "SIZE", "SMALLINT", "START", "SUCCESSFUL",
            "SYNONYM", "SYSDATE", "TABLE", "THEN", "TO", "TRIGGER", "UID", "UNION",
            "UNIQUE", "UPDATE", "USER", "VALIDATE", "VALUES", "VARCHAR", "VARCHAR2",
            "VIEW", "WHENEVER", "WHERE", "WITH");

    @Autowired
    DataSource dataSource;

    @Test
    void 엔티티가_만드는_테이블과_컬럼에_Oracle_예약어가_없다() throws SQLException {
        List<String> violations = new ArrayList<>();
        int inspected = 0;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "select table_name, column_name from information_schema.columns"
                             + " where table_schema = 'PUBLIC'")) {

            while (rs.next()) {
                String table = rs.getString("table_name");
                String column = rs.getString("column_name");
                inspected++;

                if (ORACLE_RESERVED.contains(table.toUpperCase())) {
                    violations.add("테이블 " + table);
                }
                if (ORACLE_RESERVED.contains(column.toUpperCase())) {
                    violations.add("컬럼 " + table + "." + column);
                }
            }
        }

        // 스키마를 못 읽으면 아무것도 검사하지 않고 통과해 버린다. 그 상태를 먼저 막는다.
        assertThat(inspected)
                .as("information_schema 를 못 읽었다. 이 테스트는 아무것도 검증하지 못한 것이다")
                .isGreaterThan(50);

        assertThat(violations)
                .as("운영 DB(Oracle)에서 생성·조회가 실패한다. @Column(name=...) 으로 이름을 바꿀 것")
                .isEmpty();
    }
}
