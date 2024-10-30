package com.uknowz.controller;

import com.uknowz.Dao.TaskDao;
import com.uknowz.Pojo.DO.Memory.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

@RestController
@RequestMapping("test")
public class TestController {

    @Autowired
    private TaskDao taskDao;

    @RequestMapping(value = "/taskUpdate")
    public int testTaskUpdate(){
        Example example = new Example(Task.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId","c5g9e125");
        criteria.andEqualTo("messageId","om_43d756fc1c742a009664a8fb1c707ba9");
        int i = taskDao.updateByExampleSelective(new Task() {{
            setTitle("程序员之mysql1");
        }}, example);
        return i;
    }
}
