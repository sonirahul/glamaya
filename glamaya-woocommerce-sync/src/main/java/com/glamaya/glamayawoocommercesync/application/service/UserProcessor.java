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

/**
 * Application service responsible for processing WooCommerce User entities.
 * This class extends {@link AbstractApplicationService} to provide user-specific
 * logic for building search requests, extracting entity IDs, and publishing events.
 */
@Slf4j
@Service
public class UserProcessor extends AbstractApplicationService<User> {

    private final EventPublisher eventPublisher;
    private final ContactMapperFactory<User> contactMapperFactory;
    private final WooUserFormatter wooUserFormatter;
    private final N8nApplicationService n8nApplicationService;
    private final ApplicationProperties.ProcessorConfig userConfig;

    /**
     * Constructs a new {@code UserProcessor}.
     *
     * @param objectMapper            The {@link ObjectMapper} for JSON serialization/deserialization.
     * @param oAuth1Service           The {@link OAuthSignerPort} for OAuth1 signature generation.
     * @param statusTrackerStore      The {@link StatusTrackerStore} for managing processor status.
     * @param processorStatusService  The {@link ProcessorStatusService} for domain-specific status logic.
     * @param wooCommerceApiClient    The {@link WooCommerceApiClientPort} for making API calls.
     * @param eventPublisher          The {@link EventPublisher} for publishing Kafka events.
     * @param contactMapperFactory    The {@link ContactMapperFactory} for mapping users to contacts.
     * @param wooUserFormatter        The {@link WooUserFormatter} for formatting user data.
     * @param n8nApplicationService   The {@link N8nApplicationService} for sending notifications.
     * @param eventPublisherPublisher The Spring {@link ApplicationEventPublisher} for internal events.
     * @param applicationProperties   The application's configuration properties.
     * @param meterRegistry           The {@link MeterRegistry} for metrics.
     */
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

    /**
     * Returns the processor type for users.
     *
     * @return {@link ProcessorType#WOO_USER}.
     */
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WOO_USER;
    }

    /**
     * Builds a {@link UserSearchRequest} based on the current {@link ProcessorStatus}.
     *
     * @param tracker The current status tracker.
     * @return A configured {@link UserSearchRequest} object.
     */
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
        log.debug("Built user search request: page={} perPage={} lastUpdatedDate={} useLastUpdatedFlag={}", tracker.getPage(), pageSize, tracker.getLastUpdatedDate(), tracker.isUseLastUpdatedDateInQuery());
        return request;
    }

    /**
     * Returns the class type for WooCommerce User entities.
     *
     * @return {@code User.class}.
     */
    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    /**
     * Extracts the ID from a {@link User} entity.
     *
     * @param entity The user entity.
     * @return The ID of the user.
     */
    @Override
    protected Object getEntityId(User entity) {
        return entity.getId();
    }

    /**
     * Publishes the primary Kafka event for a formatted user.
     *
     * @param formatted The formatted user entity.
     */
    @Override
    protected void publishPrimaryEvent(User formatted) {
        log.debug("Publishing primary user event userId={}", formatted.getId());
        eventPublisher.send(userConfig.kafkaTopic(), formatted.getId(), formatted);
    }

    /**
     * Publishes a secondary Kafka event for a formatted user, typically a contact event.
     *
     * @param formatted The formatted user entity.
     */
    @Override
    protected void publishSecondaryEvent(User formatted) {
        log.debug("Publishing secondary user event userId={}", formatted.getId());
        var contact = contactMapperFactory.toGlamayaContact(formatted, userConfig.sourceAccountName());
        eventPublisher.send(userConfig.contactKafkaTopic(), contact.getId(), contact);
    }

    /**
     * Sends a success notification to n8n if enabled.
     *
     * @param formatted The formatted user entity.
     * @param ctx       Context map for the notification.
     */
    @Override
    protected void notifySuccess(User formatted, Map<String, Object> ctx) {
        log.debug("User processed successfully userId={}", formatted.getId());
        if (userConfig.n8n().enable())
            n8nApplicationService.success(true, userConfig.n8n().webhookUrl(), formatted, ctx);
    }

    /**
     * Sends an error notification to n8n if enabled.
     *
     * @param original The original user entity.
     * @param e        The exception that occurred.
     * @param ctx      Context map for the notification.
     */
    @Override
    protected void notifyError(User original, Exception e, Map<String, Object> ctx) {
        log.error("User processing failed userId={} errorMsg={}", original.getId(), e.getMessage(), e);
        if (userConfig.n8n().enable())
            n8nApplicationService.error(true, userConfig.n8n().errorWebhookUrl(), e.getMessage(), ctx);
    }
}
