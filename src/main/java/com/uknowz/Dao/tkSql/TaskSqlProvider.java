package com.uknowz.Dao.tkSql;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public class TaskSqlProvider {

  public String batchUpdateShowWeight(@Param("ids") List<Integer> ids) {
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE flo_memory_task ");
    sql.append("SET showWeight = showWeight + 1 ");
    sql.append("WHERE taskId IN (");
    for (int i = 0; i < ids.size(); i++) {
      if (i != 0) {
        sql.append(",");
      }
      sql.append("#{ids[").append(i).append("]}");
    }
    sql.append(")");
    return sql.toString();
  }

}