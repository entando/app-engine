package org.entando.entando.plugins.jpsolr.apsadmin.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import com.opensymphony.xwork2.Action;
import java.util.ArrayList;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrLastReloadInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolrConfigActionTest {

    @Mock
    private ISolrSearchEngineManager solrSearchEngineManager;

    @InjectMocks
    private SolrConfigAction solrConfigAction;

    @Test
    void shouldReturnContentTypesSettings() throws Exception {
        Mockito.when(solrSearchEngineManager.getContentTypesSettings()).thenReturn(new ArrayList<>());
        solrConfigAction.getContentTypesSettings();
        Mockito.verify(solrSearchEngineManager).getContentTypesSettings();
    }

    @Test
    void shouldRefreshContentType() throws Exception {
        solrConfigAction.setTypeCode("typeCode");
        String result = solrConfigAction.refreshContentType();

        Assertions.assertEquals(Action.SUCCESS, result);
        Assertions.assertEquals(1, solrConfigAction.getRefreshResult());
        Mockito.verify(solrSearchEngineManager).refreshContentType("typeCode");
    }

    @Test
    void shouldHandleRefreshContentTypeFailure() throws Exception {
        solrConfigAction.setTypeCode("typeCode");
        Mockito.doThrow(EntException.class).when(solrSearchEngineManager).refreshContentType("typeCode");
        String result = solrConfigAction.refreshContentType();

        Assertions.assertEquals(BaseAction.FAILURE, result);
        Assertions.assertEquals(0, solrConfigAction.getRefreshResult());
        Mockito.verify(solrSearchEngineManager).refreshContentType("typeCode");
    }

    @Test
    void shouldReturnSuccessIfReloadIsInProgress() {
        Mockito.when(solrSearchEngineManager.getStatus())
                .thenReturn(ICmsSearchEngineManager.STATUS_RELOADING_INDEXES_IN_PROGRESS);
        String result = solrConfigAction.reloadContentsIndex();
        Assertions.assertEquals(Action.SUCCESS, result);
    }

    @Test
    void shouldReturnSuccessIfReloadIsNotInProgressButTypeCodeIsNotValid() throws Exception {
        Mockito.when(solrSearchEngineManager.getStatus()).thenReturn(ICmsSearchEngineManager.STATUS_READY);
        String result = solrConfigAction.reloadContentsIndex();
        Assertions.assertEquals(Action.SUCCESS, result);
        Mockito.verify(solrSearchEngineManager, never()).startReloadContentsReferencesByType(any());
    }

    @Test
    void shouldStartReloadIfStatusIsReady() throws Exception {
        Mockito.when(solrSearchEngineManager.getStatus()).thenReturn(ICmsSearchEngineManager.STATUS_READY);
        solrConfigAction.setTypeCode("typeCode");
        String result = solrConfigAction.reloadContentsIndex();
        Assertions.assertEquals(Action.SUCCESS, result);
        Mockito.verify(solrSearchEngineManager).startReloadContentsReferencesByType("typeCode");
    }

    @Test
    void shouldHandleReloadFailure() throws Exception {
        Mockito.when(solrSearchEngineManager.getStatus()).thenReturn(ICmsSearchEngineManager.STATUS_READY);
        Mockito.doThrow(EntException.class).when(solrSearchEngineManager)
                .startReloadContentsReferencesByType("typeCode");
        solrConfigAction.setTypeCode("typeCode");
        String result = solrConfigAction.reloadContentsIndex();
        Assertions.assertEquals(BaseAction.FAILURE, result);
        Mockito.verify(solrSearchEngineManager).startReloadContentsReferencesByType("typeCode");
    }

    @Test
    void shouldReturnLastReloadInfo() {
        Mockito.when(solrSearchEngineManager.getLastReloadInfo()).thenReturn(new SolrLastReloadInfo());
        solrConfigAction.getLastReloadInfo();
        Mockito.verify(solrSearchEngineManager).getLastReloadInfo();
    }
}
