package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooUserFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.datacontracts.woocommerce.UserOrderBy;
import com.glamaya.datacontracts.woocommerce.UserSearchRequest;
import com.glamaya.glamayawoocommercesync.port.EventPublisher;
import com.glamaya.glamayawoocommercesync.port.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.N8nNotificationService;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class UserProcessor extends AbstractWooProcessor<User> {

    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<User> contactMapperFactory;
    private final WooUserFormatter wooUserFormatter;
    private final N8nNotificationService n8nNotificationService;

    @Value("${application.kafka.topic.user-events}")
    private String userEventsTopic;
    @Value("${application.kafka.topic.contact-events}")
    private String contactEventsTopic;
    @Value("${application.woocommerce.entities.users.n8n.webhook-url}")
    private String n8nWebhookUrl;
    @Value("${application.woocommerce.entities.users.n8n.error-webhook-url}")
    private String n8nErrorWebhookUrl;
    @Value("${application.woocommerce.entities.users.n8n.enable}")
    private boolean n8nEnable;
    @Value("${external.woocommerce.api.account-name}")
    private String sourceAccountName;

    public UserProcessor(WebClient woocommerceWebClient,
                         ObjectMapper objectMapper,
                         PollerMetadata poller,
                         @Value("${application.woocommerce.entities.users.page-size}") long pageSize,
                         @Value("${application.woocommerce.entities.users.reset-on-startup: false}") boolean resetOnStartup,
                         @Value("${application.woocommerce.entities.users.fetch-duration-in-millis.active-mode}") int active,
                         @Value("${application.woocommerce.entities.users.fetch-duration-in-millis.passive-mode}") int passive,
                         @Value("${application.woocommerce.entities.users.query-url}") String queryUrl,
                         @Value("${application.woocommerce.entities.users.enable}") boolean enable,
                         @Value("${application.processing.concurrency:4}") int processingConcurrency,
                         OAuth1Service oAuth1Service,
                         StatusTrackerStore statusTrackerStore,
                         EventPublisher eventPublisher,
                         ContactMapperFactory<User> contactMapperFactory,
                         WooUserFormatter wooUserFormatter,
                         N8nNotificationService n8nNotificationService,
                         ApplicationEventPublisher eventPublisherPublisher) {
        super(woocommerceWebClient, objectMapper, poller, pageSize, resetOnStartup, active, passive, queryUrl, enable, processingConcurrency, oAuth1Service, statusTrackerStore, eventPublisherPublisher);
        this.eventPublisher = eventPublisher;
        this.contactMapperFactory = contactMapperFactory;
        this.wooUserFormatter = wooUserFormatter;
        this.n8nNotificationService = n8nNotificationService;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_USER;
    }

    @Override
    protected Object buildSearchRequest(ProcessorStatusTracker tracker) {
        var b = UserSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(UserOrderBy.id)
                .withOrder(SortOrder.asc)
                .withPage(Long.valueOf(tracker.getPage()))
                .withPerPage(Long.valueOf(pageSize));
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        return b.build();
    }

    // Hook implementations
    @Override
    protected User doFormat(User entity) {
        return wooUserFormatter.format(entity);
    }

    @Override
    protected Object getEntityId(User entity) {
        return entity.getId();
    }

    @Override
    protected void publishPrimaryEvent(User formatted) {
        eventPublisher.send(userEventsTopic, formatted.getId(), formatted);
    }

    @Override
    protected void publishSecondaryEvent(User formatted) {
        var contact = contactMapperFactory.toGlamayaContact(formatted, sourceAccountName);
        eventPublisher.send(contactEventsTopic, contact.getId(), contact);
    }

    @Override
    protected void notifySuccess(User formatted, Map<String, Object> ctx) {
        if (n8nEnable) n8nNotificationService.success(true, n8nWebhookUrl, formatted, ctx);
    }

    @Override
    protected void notifyError(User original, Exception e, Map<String, Object> ctx) {
        if (n8nEnable) n8nNotificationService.error(true, n8nErrorWebhookUrl, e.getMessage(), ctx);
    }

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }
}