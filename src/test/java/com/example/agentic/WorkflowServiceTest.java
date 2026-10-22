package com.example.agentic;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:testflow;DB_CLOSE_DELAY=-1","app.admin-key=test-secret"}) class WorkflowServiceTest {
 @Autowired WorkflowService workflow;
 @Autowired UrlService urls;
 @Test void greenfieldApprovalGate(){String id=(String)workflow.start(new WorkflowService.StartRequest("greenfield","Build a short URL API")).get("ID");workflow.advance(id);assertEquals("WAITING_APPROVAL",workflow.detail(id).get("STATE"));assertEquals("RUNNING",workflow.approve(id,new WorkflowService.DecisionRequest("reviewer","QA reviewed",true)).get("STATE"));assertEquals("DONE",workflow.advance(id).get("STATE"));}
 @Test void brownfieldReplanRetainsAudit(){String id=(String)workflow.start(new WorkflowService.StartRequest("brownfield","Add analytics")).get("ID");workflow.advance(id);Map<String,Object> replanned=workflow.replan(id,"Add daily analytics with privacy constraints");assertEquals(2,((Number)replanned.get("VERSION")).intValue());assertEquals("RUNNING",replanned.get("STATE"));}
 @Test void ambiguousRejectAndRollback(){String id=(String)workflow.start(new WorkflowService.StartRequest("ambiguous","Make URLs secure")).get("ID");workflow.advance(id);assertEquals("REJECTED",workflow.approve(id,new WorkflowService.DecisionRequest("owner","Unclear threat model",false)).get("STATE"));}
 @Test void urlLifecycle(){var created=urls.create(new UrlService.CreateUrl("https://example.com/page","interview-demo",null));assertEquals("https://example.com/page",urls.resolve((String)created.get("code")));assertEquals(1L,((Number)urls.stats((String)created.get("code")).get("HITS")).longValue());urls.disable((String)created.get("code"));}
}
