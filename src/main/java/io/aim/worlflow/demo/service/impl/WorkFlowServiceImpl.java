package io.aim.worlflow.demo.service.impl;

import io.aim.worlflow.demo.service.WorkFlowService;
import org.activiti.engine.*;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jian_zhang02@163.com on 2018/7/24.
 */
@Service
public class WorkFlowServiceImpl implements WorkFlowService{

    @Autowired
    private SpringProcessEngineConfiguration springProcessEngineConfiguration;

    @Override
    public boolean startWorkFlow() {
        ProcessEngine processEngine = springProcessEngineConfiguration.buildProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                .addInputStream("bmpn/leave.bpmn", this.getClass().getClassLoader().getResourceAsStream("bmpn/leave.bpmn"))
                .deploy();
        ProcessDefinition processDefinition  = repositoryService
                .createProcessDefinitionQuery()
                .singleResult();
        assert "leave".equals(processDefinition.getKey());
        RuntimeService runtimeService = processEngine.getRuntimeService();

        Map<String, Object> variables = new HashMap<>();
        variables.put("applyUser", "employee1");
        variables.put("days", 3);


        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leave", variables);
        assert processInstance != null;
        System.out.println("pid= " + processInstance.getId() + " , pdid=" + processInstance.getProcessDefinitionId());

        TaskService taskService  = processEngine.getTaskService();
        Task taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader")
                .singleResult();

        taskService.claim(taskOfDeptLeader.getId(), "deptLeader");

        variables = new HashMap<>();
        variables.put("approved", true);
        taskService.complete(taskOfDeptLeader.getId(), variables);
        taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader").singleResult();

        HistoryService historyService = processEngine.getHistoryService();
        long count = historyService.createHistoricProcessInstanceQuery().finished().count();
        return true;
    }
}
