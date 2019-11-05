package br.com.maisvida.camunda.worker;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.maisvida.camunda.service.VerificarDiasDisponiveisService;

@Component
public class VerificarDiasDisponiveisWorker {

	@Autowired
	private VerificarDiasDisponiveisService service;

	public static final String WORKER_ID = "verificarDiasDisponiveisWorker";

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificarDiasDisponiveisWorker.class);

	@Scheduled(fixedRate = 30000)
	public void fetchExternalTask() throws IOException, InterruptedException {		

		final ObjectMapper mapper = new ObjectMapper();

		final ObjectNode topic = mapper.createObjectNode();
		topic.put("topicName", "verifica-dias-disponiveis-topic");
		topic.put("lockDuration", 300_000);

		final ArrayNode topics = mapper.createArrayNode();
		topics.add(topic);

		final ObjectNode root = mapper.createObjectNode();
		root.put("workerId", WORKER_ID);
		root.put("maxTasks", 3);
		root.set("topics", topics);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth("admin", "qwe123");

		// fetch and lock 3 tasks for 5 minutes
		HttpEntity<String> entity = new HttpEntity<String>(//
				mapper.writeValueAsString(root), // here is the JSON body
				headers);
		String result = new RestTemplate().postForObject("http://localhost:8080/engine-rest/external-task/fetchAndLock", entity, String.class);

		JsonNode solicitacoesFerias = mapper.readTree(result);


		for (JsonNode lockedTask : solicitacoesFerias) {
			String taskId = lockedTask.get("id").asText();
			String processInstanceId = lockedTask.get("processInstanceId").asText();
			
			// complete the task
			LOGGER.info("processInstanceId: {}, complete task {}", processInstanceId, taskId);
			
			int diasDisponiveis = service.recuperarQuantidadeDeDiasDiasponiveis();
			
			
			ObjectNode variables = mapper.createObjectNode();
			ObjectNode resultVariable = mapper.createObjectNode()
					.put("value", diasDisponiveis);
			variables.set("qtdDiasDisponiveis", resultVariable);

			ObjectNode completeRoot = mapper.createObjectNode();
			completeRoot.put("workerId", WORKER_ID);
			completeRoot.set("variables", variables);

			entity = new HttpEntity<String>(mapper.writeValueAsString(completeRoot), headers);
			ResponseEntity<Object> completeResponse = new RestTemplate().postForEntity("http://localhost:8080/engine-rest/external-task/" + taskId + "/complete", entity, null);
			LOGGER.info("processInstanceId: {}, qtdDiasDisponiveis: {}, status code for completion: {} ", processInstanceId, diasDisponiveis, completeResponse.getStatusCodeValue());
		}
	}
}
