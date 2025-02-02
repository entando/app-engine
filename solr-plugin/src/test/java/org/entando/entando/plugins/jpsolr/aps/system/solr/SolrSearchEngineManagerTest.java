package org.entando.entando.plugins.jpsolr.aps.system.solr;

import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_NEED_TO_RELOAD_INDEXES;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_READY;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_RELOADING_INDEXES_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.eq;

import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedEvent;
import java.util.List;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.util.NamedList;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.cache.ISolrSearchEngineCacheWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolrSearchEngineManagerTest {

    @Mock
    private ILangManager langManager;
    @Mock
    private ICategoryManager categoryManager;
    @Mock
    private ITenantManager tenantManager;
    @Mock
    private HttpClientBuilder solrHttpClientBuilder;
    @Mock
    private IContentManager contentManager;
    @Mock
    private ISolrSearchEngineCacheWrapper cacheWrapper;

    private SolrProxyTenantAware solrProxy;
    private SolrSearchEngineManager solrSearchEngineManager;

    private MockedConstruction<Http2SolrClient.Builder> mockedConstructionSolrClientBuilder;
    private Http2SolrClient solrClient;

    @BeforeEach
    void setUp() throws Exception {
        mockedConstructionSolrClientBuilder = Mockito.mockConstruction(Http2SolrClient.Builder.class,
                (builder, context) -> {
                    solrClient = Mockito.mock(Http2SolrClient.class);
                    Mockito.when(builder.build()).thenReturn(solrClient);
                });
        solrProxy = new SolrProxyTenantAware(
                langManager, categoryManager, tenantManager, solrHttpClientBuilder
        );
        solrProxy.afterPropertiesSet();
        solrSearchEngineManager = new SolrSearchEngineManager();
        solrSearchEngineManager.setSolrProxy(solrProxy);
        solrSearchEngineManager.setContentManager(contentManager);
        solrSearchEngineManager.setLangManager(langManager);
        solrSearchEngineManager.setCacheWrapper(cacheWrapper);
    }

    @AfterEach
    void tearDown() {
        mockedConstructionSolrClientBuilder.close();
    }
    
    @Test
    void shouldReleaseCacheWhenrefreshService() throws Throwable {
        this.solrSearchEngineManager.refresh();
        Mockito.verify(cacheWrapper, Mockito.times(1)).release();
    }
    
    @Test
    void shouldExecuteNotifyWithInternalError() {
        PublicContentChangedEvent event = new PublicContentChangedEvent();
        event.setContentId("ART123");
        Assertions.assertDoesNotThrow(() -> this.solrSearchEngineManager.updateFromPublicContentChanged(event));
    }

    @Test
    void shouldRollbackIndexStatusIfReloadThreadDoesNotStart() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_NEED_TO_RELOAD_INDEXES);
        try (MockedConstruction<SolrIndexLoaderThread> c = Mockito.mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> Mockito.doThrow(RuntimeException.class).when(thread).start())) {
            Assertions.assertThrows(EntException.class, () -> solrSearchEngineManager.startReloadContentsReferences());
        }
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, solrProxy.getIndexStatus().getValue());
    }

    @Test
    void shouldSetStatusInProgressIfReloadThreadIsStartedStart() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_READY);
        try (MockedConstruction<SolrIndexLoaderThread> c = Mockito.mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> Mockito.doNothing().when(thread).start())) {
            Assertions.assertNotNull(solrSearchEngineManager.startReloadContentsReferences());
        }
        Assertions.assertEquals(STATUS_RELOADING_INDEXES_IN_PROGRESS, solrProxy.getIndexStatus().getValue());
    }

    @Test
    void shouldSkipReloadThreadIfReloadIsAlreadyInProgress() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_RELOADING_INDEXES_IN_PROGRESS);
        Assertions.assertNull(solrSearchEngineManager.startReloadContentsReferences());
        Assertions.assertEquals(STATUS_RELOADING_INDEXES_IN_PROGRESS, solrProxy.getIndexStatus().getValue());
    }

    @Test
    void shouldReloadByType() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_READY);
        try (MockedConstruction<SolrIndexLoaderThread> c = Mockito.mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> Mockito.doNothing().when(thread).start())) {
            solrSearchEngineManager.startReloadContentsReferencesByType("ART");
        }
        Assertions.assertEquals(STATUS_RELOADING_INDEXES_IN_PROGRESS, solrProxy.getIndexStatus().getValue());
    }

    @Test
    void shouldRefreshCmsFields() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_READY);
        NamedList<Object> solrClientResponse = new NamedList<>();
        solrClientResponse.add("fields", List.of());
        Mockito.when(solrClient.request(Mockito.any(SchemaRequest.Fields.class), eq("entando")))
                .thenReturn(solrClientResponse);
        try (MockedConstruction<SolrIndexLoaderThread> c = Mockito.mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> Mockito.doNothing().when(thread).start())) {
            solrSearchEngineManager.refreshCmsFields();
        }
        Mockito.verify(solrClient, Mockito.times(1))
                .request(Mockito.any(SchemaRequest.MultiUpdate.class), eq("entando"));
    }
}
