package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooUserFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.datacontracts.woocommerce.UserOrderBy;
import com.glamaya.datacontracts.woocommerce.UserSearchRequest;
import com.glamaya.glamayawoocommercesync.config.kafka.KafkaProducer;
import com.glamaya.glamayawoocommercesync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProcessor implements GlamWoocommerceProcessor<List<User>> {

    @Qualifier("woocommerceWebClient")
    private final WebClient woocommerceWebClient;
    @Qualifier("n8nWebhookWebClient")
    private final WebClient n8nWebClient;
    private final OAuth1Service oAuth1Service;
    private final KafkaProducer<Object> producer;
    @Getter
    private final PollerMetadata poller;
    @Getter
    private final ObjectMapper objectMapper;
    private final ProcessorStatusTrackerRepository repository;
    private final ContactMapperFactory<User> contactMapperFactory;
    private final WooUserFormatter wooUserFormatter;

    @Value("${application.woocommerce.entities.users.query-url}")
    private final String queryUsersUrl;
    @Value("${application.woocommerce.entities.users.n8n.webhook-url}")
    private final String n8nWebhookUrl;
    @Value("${application.woocommerce.entities.users.n8n.error-webhook-url}")
    private final String n8nErrorWebhookUrl;
    @Value("${application.woocommerce.entities.users.n8n.enable}")
    private final boolean n8nEnable;
    @Value("${application.woocommerce.entities.users.enable}")
    private final boolean enable;
    @Value("${application.woocommerce.entities.users.page-size}")
    private final long pageSize;
    @Value("${application.woocommerce.entities.users.reset-on-startup: false}")
    private boolean resetOnStartup;
    @Value("${external.woocommerce.api.account-name}")
    private final String sourceAccountName;
    @Value("${application.kafka.topic.user-events}")
    private final String userEventsTopic;
    @Value("${application.kafka.topic.contact-events}")
    private final String contactEventsTopic;
    @Value("${application.woocommerce.entities.users.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.woocommerce.entities.users.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    public MessageSource<List<User>> receive() {
        return () -> {
            if (!enable) {
                log.info("User sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            Mono<ProcessorStatusTracker> trackerMono = getOrCreateStatusTracker(resetOnStartup, pageSize)
                    .doOnNext(t -> resetOnStartup = false);

            return trackerMono.flatMap(statusTracker -> {
                        UserSearchRequest userSearchRequest = buildUserSearchRequest(statusTracker);
                        Map<String, String> queryParamMap = objectMapper.convertValue(userSearchRequest, new TypeReference<>() {});
                        String oauthHeader = oAuth1Service.generateOAuth1Header(queryUsersUrl, queryParamMap);
                        return fetchEntities(woocommerceWebClient, queryUsersUrl, queryParamMap, oauthHeader, new ParameterizedTypeReference<List<User>>() {})
                                .defaultIfEmpty(List.of())
                                .flatMap(users -> {
                                    if (users.isEmpty()) {
                                        log.info("No new woocommerce Users found, resetting status tracker and switching to passive mode");
                                        resetStatusTracker(statusTracker);
                                        modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
                                    } else {
                                        log.info("Fetched {} woocommerce Users", statusTracker.getCount() + users.size());
                                        updateStatusTracker(statusTracker, users, o -> Objects.nonNull(((User) o).getDateModifiedGmt())
                                                ? ((User) o).getDateModified() : ((User) o).getDateCreated());
                                        modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
                                    }
                                    return repository.save(statusTracker)
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .thenReturn(users);
                                });
                    })
                    .map(users -> users.isEmpty() ? null : MessageBuilder.withPayload(users).build())
                    .block(); // boundary sync
        };
    }

    private UserSearchRequest buildUserSearchRequest(ProcessorStatusTracker statusTracker) {
        var builder = UserSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(UserOrderBy.id)
                .withOrder(SortOrder.asc)
                .withPage(statusTracker.getPage())
                .withPerPage(pageSize);

        if (statusTracker.isUseLastUpdatedDateInQuery() && statusTracker.getLastUpdatedDate() != null) {
            builder.withModifiedAfter(statusTracker.getLastUpdatedDate());
        }
        return builder.build();
    }

    @Override
    public ProcessorStatusTrackerRepository getStatusTrackerRepository() {
        return repository;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_USER;
    }

    @Override
    public Consumer<SourcePollingChannelAdapterSpec> poll() {
        return e -> e.poller(getPoller());
    }

    @Override
    public Object handle(List<User> payload, MessageHeaders headers) {
        payload.forEach(user -> {
            try {
                user = wooUserFormatter.format(user);
                producer.send(userEventsTopic, user.getId(), user);
                var contact = contactMapperFactory.toGlamayaContact(user, sourceAccountName);
                producer.send(contactEventsTopic, contact.getId(), contact);
                Map<String,Object> ctx = new HashMap<>();
                ctx.put("entity","user");
                ctx.put("id", user.getId());
                publishData(n8nWebClient, n8nWebhookUrl, user, n8nEnable, ctx);
            } catch (Exception e) {
                Map<String,Object> errCtx = new HashMap<>();
                errCtx.put("entity","user");
                errCtx.put("id", user.getId());
                errCtx.put("error", e.getMessage());
                log.error("Error processing user: {}", user.getId(), e);
                publishData(n8nWebClient, n8nErrorWebhookUrl, e.getMessage(), n8nEnable, errCtx);
            }
        });
        return null;
    }
}