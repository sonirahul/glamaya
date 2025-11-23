package com.glamaya.glamayawoocommercesync.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooUserFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.datacontracts.woocommerce.UserOrderBy;
import com.glamaya.datacontracts.woocommerce.UserSearchRequest;
import com.glamaya.glamayawoocommercesync.config.ApplicationProperties;
import com.glamaya.glamayawoocommercesync.port.EventPublisher;
import com.glamaya.glamayawoocommercesync.port.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawoocommercesync.repository.entity.ProcessorType;
import com.glamaya.glamayawoocommercesync.service.N8nNotificationService;
import com.glamaya.glamayawoocommercesync.service.OAuth1Service;
import lombok.extern.slf4j.Slf4j;
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
    private final ApplicationProperties.ProcessorConfig userConfig;

    public UserProcessor(
            WebClient woocommerceWebClient,
            ObjectMapper objectMapper,
            PollerMetadata poller,
            OAuth1Service oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            EventPublisher eventPublisher,
            ContactMapperFactory<User> contactMapperFactory,
            WooUserFormatter wooUserFormatter,
            N8nNotificationService n8nNotificationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties) {
        super(
                woocommerceWebClient,
                objectMapper,
                poller,
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).getPageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).isResetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).getFetchDurationMs().getActive(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).getFetchDurationMs().getPassive(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).getQueryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).isEnable(),
                applicationProperties.getProcessing().getConcurrency(),
                oAuth1Service,
                statusTrackerStore,
                eventPublisherPublisher
        );
        this.eventPublisher = eventPublisher;
        this.contactMapperFactory = contactMapperFactory;
        this.wooUserFormatter = wooUserFormatter;
        this.n8nNotificationService = n8nNotificationService;
        this.userConfig = applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER);
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
                .withPage((long) tracker.getPage())
                .withPerPage(pageSize);
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        return b.build();
    }

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
        eventPublisher.send(userConfig.getKafkaTopic(), formatted.getId(), formatted);
    }

    @Override
    protected void publishSecondaryEvent(User formatted) {
        var contact = contactMapperFactory.toGlamayaContact(formatted, userConfig.getSourceAccountName());
        eventPublisher.send(userConfig.getContactKafkaTopic(), contact.getId(), contact);
    }

    @Override
    protected void notifySuccess(User formatted, Map<String, Object> ctx) {
        if (userConfig.getN8n().isEnable())
            n8nNotificationService.success(true, userConfig.getN8n().getWebhookUrl(), formatted, ctx);
    }

    @Override
    protected void notifyError(User original, Exception e, Map<String, Object> ctx) {
        if (userConfig.getN8n().isEnable())
            n8nNotificationService.error(true, userConfig.getN8n().getErrorWebhookUrl(), e.getMessage(), ctx);
    }

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }
}