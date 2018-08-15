package io.aim.worlflow.demo.service.impl

import io.aim.worlflow.WorkflowApplication
import io.aim.worlflow.demo.service.WorkFlowService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootContextLoader
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Title
import spock.lang.Unroll

/**
 * description
 *
 * @author aim 2018/07/28 12:21
 */
@SpringBootTest(classes = WorkflowApplication.class)
@ContextConfiguration(loader = SpringBootContextLoader.class)
@Title("单元测试")
@Unroll
@Transactional
@Rollback
@Timeout(10)
class WorkFlowServiceImplTest extends Specification {

    @Autowired
    private WorkFlowService workFlowService;

    def "StartWorkFlow"() {
        when:
            def result = workFlowService.startWorkFlow();
        then:
            result == true
    }
}
