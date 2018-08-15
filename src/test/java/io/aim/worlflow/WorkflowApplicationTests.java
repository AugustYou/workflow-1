package io.aim.worlflow;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = WorkflowApplication.class)
public class WorkflowApplicationTests {

    @Autowired
    private SpringProcessEngineConfiguration springProcessEngineConfiguration;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private FormService formService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testStartProcess() throws Exception {
//		ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
//		//连接数据库的配置
//		processEngineConfiguration.setJdbcDriver("com.mysql.jdbc.Driver");
//		processEngineConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/workflow_service?useUnicode=true&characterEncoding=utf-8&useSSL=false");
//		processEngineConfiguration.setJdbcUsername("root");
//		processEngineConfiguration.setJdbcPassword("root");
//
//		processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = springProcessEngineConfiguration.buildProcessEngine();


        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                .addClasspathResource("bmpn/leave.bpmn")
                .deploy();
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .singleResult();
        assert "leave".equals(processDefinition.getKey());
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leave");
        assert processInstance != null;
        System.out.println("pid= " + processInstance.getId() + " , pdid=" + processInstance.getProcessDefinitionId());
    }

    @Test
    public void testStartProcess2() throws Exception {
//		ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
//		//连接数据库的配置
//		processEngineConfiguration.setJdbcDriver("com.mysql.jdbc.Driver");
//		processEngineConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/workflow_service?useUnicode=true&characterEncoding=utf-8&useSSL=false");
//		processEngineConfiguration.setJdbcUsername("root");
//		processEngineConfiguration.setJdbcPassword("root");
//
//		processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = springProcessEngineConfiguration.buildProcessEngine();


        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                .addInputStream("bmpn/leave.bpmn", this.getClass().getClassLoader().getResourceAsStream("bmpn/leave.bpmn"))
                .deploy();
        ProcessDefinition processDefinition = repositoryService
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

        TaskService taskService = processEngine.getTaskService();
        Task taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader")
                .singleResult();
        assert taskOfDeptLeader != null;

        assert "领导审批".equals(taskOfDeptLeader.getName());

        taskService.claim(taskOfDeptLeader.getId(), "deptLeader");

        variables = new HashMap<>();
        variables.put("approved", true);
        taskService.complete(taskOfDeptLeader.getId(), variables);
        taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader").singleResult();
        assert taskOfDeptLeader == null;

        HistoryService historyService = processEngine.getHistoryService();
        long count = historyService.createHistoricProcessInstanceQuery().finished().count();
        assert count == 1;
    }

    @Test
    @Deployment(resources = "chapter6/dynamic-form/leave.bpmn")
    public void allApproved() {
        String currentUserId = "henryyan";
        identityService.setAuthenticatedUserId(currentUserId);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("leave").singleResult();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, String> variables = new HashMap<String, String>();
        Calendar ca = Calendar.getInstance();
        String startDate = sdf.format(ca.getTime());
        ca.add(Calendar.DAY_OF_MONTH, 2);
        String endDate = sdf.format(ca.getTime());
        variables.put("startDate", startDate);
        variables.put("endDate", endDate);
        variables.put("reason", "公休");

        ProcessInstance processInstance = formService.submitStartFormData(processDefinition.getId(), variables);
        assert processInstance != null;

        Task deptLeaderTask = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader").singleResult();
        variables = new HashMap<String, String>();
        variables.put("deptLeaderApproved", "true");
        formService.submitStartFormData(deptLeaderTask.getId(), variables);

        Task hrTask = taskService.createTaskQuery().taskCandidateGroup("hr").singleResult();
        variables = new HashMap<String, String>();
        variables.put("hrApproved", "true");
        formService.submitTaskFormData(hrTask.getId(), variables);

        Task reportBackTask = taskService.createTaskQuery()
                .taskAssignee(currentUserId).singleResult();
        variables = new HashMap<String, String>();
        variables.put("reportBackDate", sdf.format(ca.getTime()));
        formService.submitTaskFormData(reportBackTask.getId(), variables);

        HistoricProcessInstance historicProcessInstance = historyService
                .createHistoricProcessInstanceQuery().finished().singleResult();
        assert historicProcessInstance != null;
        Map<String, Object> historyVariables = packageVariables(processInstance);
        assert "ok".equals(historyVariables.get("result"));
    }

    private Map<String, Object> packageVariables(ProcessInstance processInstance) {
        Map<String, Object> historyVariables = new HashMap<>();
        List<HistoricDetail> list = historyService.createHistoricDetailQuery()
                .processInstanceId(processInstance.getId()).list();
        for (HistoricDetail historicDetail : list) {
            if (historicDetail instanceof HistoricFormProperty) {
				HistoricFormProperty field = (HistoricFormProperty) historicDetail;
				historyVariables.put(field.getPropertyId(), field.getPropertyValue());

            } else if (historicDetail instanceof HistoricVariableUpdate){
                HistoricDetailVariableInstanceUpdateEntity variable = (HistoricDetailVariableInstanceUpdateEntity) historicDetail;
                historyVariables.put(variable.getName(), variable.getValue());
            }
        }
        return historyVariables;
    }


}
