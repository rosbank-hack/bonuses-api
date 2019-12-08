package ros.hack.bonuses.service.impl;

import com.github.voteva.Operation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ros.hack.bonuses.config.KafkaProperties;
import ros.hack.bonuses.service.ConsumerService;
import ros.hack.bonuses.service.ProducerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static ros.hack.bonuses.consts.Constants.SERVICE_NAME;

@Slf4j
@RequiredArgsConstructor
@Service
public class ConsumerServiceImpl implements ConsumerService {

    private final KafkaProperties kafkaProperties;
    private final ProducerService producerService;

    @Override
    @Transactional
    @KafkaListener(topics = "${kafka.payment-topic}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${kafka.group-id}")
    public void consume(@NonNull List<Operation> operations) {
        operations.forEach(operation -> {
            log.info(operation.toString());
            producerService.send(kafkaProperties.getOperationTopic(), addCashback(operation));
        });
    }

    private Operation addCashback(@NonNull Operation operation) {
        com.github.voteva.Service bonusService = new com.github.voteva.Service();
        if (operation.getServices() != null
                && operation.getServices().get(SERVICE_NAME) != null) {
            bonusService = operation.getServices().get(SERVICE_NAME);
        }

        Map<String, String> request = new HashMap<>();
        if (bonusService.getRequest() != null) {
            request = bonusService.getRequest();
        }
        Map<String, String> response = request;

        response.put("cashback", getRandomBonus().toString());

        bonusService.setRequest(request);
        bonusService.setResponse(response);

        operation.getServices().put(SERVICE_NAME, bonusService);
        return operation;
    }

    public Integer getRandomBonus() {
        Random random = new Random();
        return random.nextInt(151) + 50;
    }
}
