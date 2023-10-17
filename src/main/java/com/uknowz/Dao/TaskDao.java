package com.uknowz.Dao;

import com.uknowz.Dao.tkSql.TaskSqlProvider;
import com.uknowz.Pojo.DO.Memory.Task;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.UpdateProvider;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

@Repository
public interface TaskDao extends Mapper<Task> {
    @UpdateProvider(type = TaskSqlProvider.class, method = "batchUpdateShowWeight")
    void batchUpdateShowWeight(@Param("ids") List<Integer> ids);
}
