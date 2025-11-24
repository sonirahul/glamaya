package com.glamaya.glamayawoocommercesync.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glamaya.datacontracts.ecommerce.formatter.WooUserFormatter;
import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.woocommerce.SortOrder;
import com.glamaya.datacontracts.woocommerce.User;
import com.glamaya.datacontracts.woocommerce.UserOrderBy;
import com.glamaya.datacontracts.woocommerce.UserSearchRequest;
import com.glamaya.glamayawoocommercesync.config.ApplicationProperties;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatus;
import com.glamaya.glamayawoocommercesync.domain.ProcessorStatusService;
import com.glamaya.glamayawoocommercesync.domain.ProcessorType;
import com.glamaya.glamayawoocommercesync.port.out.EventPublisher;
import com.glamaya.glamayawoocommercesync.port.out.OAuthSignerPort;
import com.glamaya.glamayawoocommercesync.port.out.StatusTrackerStore;
import com.glamaya.glamayawoocommercesync.port.out.WooCommerceApiClientPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserProcessor extends AbstractApplicationService<User> {

    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<User> contactMapperFactory;
    private final WooUserFormatter wooUserFormatter;
    private final N8nApplicationService n8nApplicationService;
    private final ApplicationProperties.ProcessorConfig userConfig;

    public UserProcessor(
            ObjectMapper objectMapper,
            OAuthSignerPort oAuth1Service,
            StatusTrackerStore statusTrackerStore,
            ProcessorStatusService processorStatusService,
            WooCommerceApiClientPort wooCommerceApiClient,
            EventPublisher eventPublisher,
            ContactMapperFactory<User> contactMapperFactory,
            WooUserFormatter wooUserFormatter,
            N8nApplicationService n8nApplicationService,
            ApplicationEventPublisher eventPublisherPublisher,
            ApplicationProperties applicationProperties,
            MeterRegistry meterRegistry) {
        super(
                objectMapper,
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).pageSize(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).resetOnStartup(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).queryUrl(),
                applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER).enable(),
                applicationProperties.getProcessing().concurrency(),
                oAuth1Service,
                statusTrackerStore,
                processorStatusService,
                wooCommerceApiClient,
                eventPublisherPublisher,
                applicationProperties,
                meterRegistry
        );
        this.eventPublisher = eventPublisher;
        this.contactMapperFactory = contactMapperFactory;
        this.wooUserFormatter = wooUserFormatter;
        this.n8nApplicationService = n8nApplicationService;
        this.userConfig = applicationProperties.getProcessorConfigOrThrow(ProcessorType.WOO_USER);
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_USER;
    }

    @Override
    protected Object buildSearchRequest(ProcessorStatus tracker) {
        var b = UserSearchRequest.builder()
                .withFetchLatest(null)
                .withOrderby(UserOrderBy.id)
                .withOrder(SortOrder.asc)
                .withPage((long) tracker.getPage())
                .withPerPage(pageSize);
        if (tracker.isUseLastUpdatedDateInQuery() && tracker.getLastUpdatedDate() != null) {
            b.withModifiedAfter(tracker.getLastUpdatedDate());
        }
        var request = b.build();
        if (log.isDebugEnabled()) {
            log.debug("Built user search request: page={} perPage={} lastUpdatedDate={} useLastUpdatedFlag={}", tracker.getPage(), pageSize, tracker.getLastUpdatedDate(), tracker.isUseLastUpdatedDateInQuery());
        }
        return request;
    }

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    protected Object getEntityId(User entity) {
        return entity.getId();
    }

    @Override
    protected void publishPrimaryEvent(User formatted) {
        log.debug("Publishing primary user event userId={}", formatted.getId());
        eventPublisher.send(userConfig.kafkaTopic(), formatted.getId(), formatted);
    }

    @Override
    protected void publishSecondaryEvent(User formatted) {
        log.debug("Publishing secondary user event userId={}", formatted.getId());
        var contact = contactMapperFactory.toGlamayaContact(formatted, userConfig.sourceAccountName());
        eventPublisher.send(userConfig.contactKafkaTopic(), contact.getId(), contact);
    }

    protected void notifySuccess(User formatted, Map<String, Object> ctx) {
        log.debug("User processed successfully userId={}", formatted.getId());
        if (userConfig.n8n().enable()) n8nApplicationService.success(true, userConfig.n8n().webhookUrl(), formatted, ctx);
    }

    protected void notifyError(User original, Exception e, Map<String, Object> ctx) {
        log.error("User processing failed userId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (userConfig.n8n().enable()) n8nApplicationService.error(true, userConfig.n8n().errorWebhookUrl(), e.getMessage(), ctx);
    }
}
