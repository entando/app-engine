package org.entando.entando.plugins.jpsolr.aps.system.solr;

import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_NEED_TO_RELOAD_INDEXES;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_READY;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_RELOADING_INDEXES_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.List;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings.AttributeSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
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
    private ICacheInfoManager cacheInfoManager;

    private SolrProxyTenantAware solrProxy;
    private SolrSearchEngineManager solrSearchEngineManager;

    private MockedConstruction<HttpSolrClient.Builder> mockedConstructionSolrClientBuilder;
    private HttpSolrClient solrClient;

    @BeforeEach
    // NOTE: java:S9024 ("use @InjectMocks") intentionally suppressed - false positive here: the object
    // under test is assembled from a mockConstruction of HttpSolrClient.Builder plus explicit setter
    // wiring (afterPropertiesSet), which @InjectMocks cannot express.
    @SuppressWarnings("java:S9024")
    void setUp() throws Exception {
        mockedConstructionSolrClientBuilder = mockConstruction(HttpSolrClient.Builder.class,
                (builder, context) -> {
                    solrClient = mock(HttpSolrClient.class);
                    when(builder.withHttpClient(any())).thenReturn(builder);
                    when(builder.build()).thenReturn(solrClient);
                });
        solrProxy = new SolrProxyTenantAware(
                langManager, categoryManager, tenantManager, solrHttpClientBuilder
        );
        solrProxy.afterPropertiesSet();
        solrSearchEngineManager = new SolrSearchEngineManager();
        solrSearchEngineManager.setSolrProxy(solrProxy);
        solrSearchEngineManager.setContentManager(contentManager);
        solrSearchEngineManager.setLangManager(langManager);
        solrSearchEngineManager.setCacheInfoManager(cacheInfoManager);
    }

    @AfterEach
    void tearDown() {
        mockedConstructionSolrClientBuilder.close();
    }

    @Test
    void shouldRollbackIndexStatusIfReloadThreadDoesNotStart() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_NEED_TO_RELOAD_INDEXES);
        try (MockedConstruction<SolrIndexLoaderThread> c = mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> doThrow(RuntimeException.class).when(thread).start())) {
            Assertions.assertThrows(EntException.class, () -> solrSearchEngineManager.startReloadContentsReferences());
        }
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, solrProxy.getIndexStatus().getValue());
    }

    @Test
    void shouldSetStatusInProgressIfReloadThreadIsStartedStart() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_READY);
        try (MockedConstruction<SolrIndexLoaderThread> c = mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> doNothing().when(thread).start())) {
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
        try (MockedConstruction<SolrIndexLoaderThread> c = mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> doNothing().when(thread).start())) {
            solrSearchEngineManager.startReloadContentsReferencesByType("ART");
        }
        Assertions.assertEquals(STATUS_RELOADING_INDEXES_IN_PROGRESS, solrProxy.getIndexStatus().getValue());
    }

    @Test
    void shouldRefreshCmsFields() throws Exception {
        solrProxy.getIndexStatus().setValue(STATUS_READY);
        NamedList<Object> solrClientResponse = new NamedList<>();
        solrClientResponse.add("fields", List.of());
        when(solrClient.request(any(SchemaRequest.Fields.class), eq("entando")))
                .thenReturn(solrClientResponse);
        try (MockedConstruction<SolrIndexLoaderThread> c = mockConstruction(SolrIndexLoaderThread.class,
                (thread, context) -> doNothing().when(thread).start())) {
            solrSearchEngineManager.refreshCmsFields();
        }
        verify(solrClient, times(1))
                .request(any(SchemaRequest.MultiUpdate.class), eq("entando"));
    }

    /** Exposes the protected {@code addAttribute} so tests can build composite fixtures. */
    private static class TestComposite extends CompositeAttribute {

        void addChild(AttributeInterface attribute) {
            this.addAttribute(attribute);
        }
    }

    private Lang lang(String code) {
        Lang lang = new Lang();
        lang.setCode(code);
        lang.setDescr(code);
        return lang;
    }

    @Test
    void shouldBuildContentTypesSettingsWithNestedBooleanAttributes() throws Exception {
        // getContentTypesSettings()/addNestedBooleanSettings() had no dedicated unit test at all:
        // this closes buildCurrentFieldConfig's "existing field found" branch, and - through the
        // shared traversal it now delegates to - the boolean/non-boolean leaf dispatch, the
        // recursion into a nested Composite, and the top-level Monolist exclusion.
        when(langManager.getLangs()).thenReturn(List.of(lang("en")));

        SimpleOrderedMap<Object> existingField = new SimpleOrderedMap<>();
        existingField.add("name", "en_myComposite_featured");
        existingField.add("type", "boolean");
        existingField.add("multiValued", false);
        NamedList<Object> solrClientResponse = new NamedList<>();
        solrClientResponse.add("fields", List.of(existingField));
        when(solrClient.request(any(SchemaRequest.Fields.class), eq("entando")))
                .thenReturn(solrClientResponse);

        when(contentManager.getSmallEntityTypes())
                .thenReturn(List.of(new SmallEntityType("TST", "Test type")));

        BooleanAttribute featured = new BooleanAttribute();
        featured.setName("featured");
        featured.setType("Boolean");
        featured.setSearchable(true);

        TextAttribute note = new TextAttribute();
        note.setName("note");
        note.setType("Text");

        BooleanAttribute innerFlag = new BooleanAttribute();
        innerFlag.setName("innerFlag");
        innerFlag.setType("Boolean");
        innerFlag.setSearchable(true);
        TestComposite inner = new TestComposite();
        inner.setName("inner");
        inner.setType("Composite");
        inner.addChild(innerFlag);

        TestComposite composite = new TestComposite();
        composite.setName("myComposite");
        composite.setType("Composite");
        composite.addChild(featured);
        composite.addChild(note);
        composite.addChild(inner);

        BooleanAttribute listItemType = new BooleanAttribute();
        listItemType.setName("myListItem");
        listItemType.setType("Boolean");
        listItemType.setSearchable(true);
        MonoListAttribute list = new MonoListAttribute();
        list.setName("myList");
        list.setType("Monolist");
        list.setNestedAttributeType(listItemType);

        // A plain top-level simple attribute (isSimple()==true) must skip nested-boolean collection
        // entirely rather than attempt it: covers the "false" outcome of "!attribute.isSimple()".
        TextAttribute title = new TextAttribute();
        title.setName("title");
        title.setType("Text");

        Content prototype = new Content();
        prototype.setTypeCode("TST");
        prototype.addAttribute(title);
        prototype.addAttribute(composite);
        prototype.addAttribute(list);
        when(contentManager.createContentType("TST")).thenReturn(prototype);

        List<ContentTypeSettings> settings = solrSearchEngineManager.getContentTypesSettings();

        Assertions.assertEquals(1, settings.size());
        List<AttributeSettings> attributeSettings = settings.get(0).getAttributeSettings();
        List<String> codes = attributeSettings.stream().map(AttributeSettings::getCode).toList();
        // The nested boolean "myComposite_featured" (found in the schema fields) and
        // "myComposite_inner_innerFlag" (recursion, missing from the schema fields) are both registered
        // under their FULL PATH - the same identifier the schema field and the index use, so two
        // same-named children of different Composites are distinguishable. The plain-text "note" child
        // and the boolean nested inside the top-level Monolist are not registered (List/Monolist
        // ancestry is excluded).
        Assertions.assertTrue(codes.contains("myComposite_featured"), codes.toString());
        Assertions.assertTrue(codes.contains("myComposite_inner_innerFlag"), codes.toString());
        Assertions.assertFalse(codes.contains("featured"), codes.toString());
        Assertions.assertFalse(codes.contains("innerFlag"), codes.toString());
        Assertions.assertFalse(codes.contains("myListItem"), codes.toString());
        Assertions.assertFalse(codes.contains("note"), codes.toString());

        AttributeSettings featuredSettings = attributeSettings.stream()
                .filter(s -> "myComposite_featured".equals(s.getCode())).findFirst().orElseThrow();
        Assertions.assertTrue(featuredSettings.isValid());

        AttributeSettings innerFlagSettings = attributeSettings.stream()
                .filter(s -> "myComposite_inner_innerFlag".equals(s.getCode())).findFirst().orElseThrow();
        Assertions.assertFalse(innerFlagSettings.isValid(), "no schema field exists yet for the nested innerFlag");
    }

    @Test
    void shouldReportSameNamedChildrenOfDifferentCompositesDistinctly() throws Exception {
        // F6: reporting the bare child name made these two rows identical in the settings screen and in
        // GET /config, even though they are two different schema fields with independent validity.
        when(langManager.getLangs()).thenReturn(List.of(lang("en")));

        // only "address"'s copy exists in the schema; "billing"'s does not
        SimpleOrderedMap<Object> existingField = new SimpleOrderedMap<>();
        existingField.add("name", "en_address_verified");
        existingField.add("type", "boolean");
        existingField.add("multiValued", false);
        NamedList<Object> solrClientResponse = new NamedList<>();
        solrClientResponse.add("fields", List.of(existingField));
        when(solrClient.request(any(SchemaRequest.Fields.class), eq("entando")))
                .thenReturn(solrClientResponse);
        when(contentManager.getSmallEntityTypes())
                .thenReturn(List.of(new SmallEntityType("TST", "Test type")));

        Content prototype = new Content();
        prototype.setTypeCode("TST");
        prototype.addAttribute(compositeWithVerifiedChild("address"));
        prototype.addAttribute(compositeWithVerifiedChild("billing"));
        when(contentManager.createContentType("TST")).thenReturn(prototype);

        List<AttributeSettings> attributeSettings =
                solrSearchEngineManager.getContentTypesSettings().get(0).getAttributeSettings();
        // the two top-level Composites are reported as well (no expectedConfig, so trivially valid),
        // plus one row per nested boolean
        Assertions.assertEquals(4, attributeSettings.size());

        AttributeSettings address = attributeSettings.stream()
                .filter(a -> "address_verified".equals(a.getCode())).findFirst().orElseThrow();
        AttributeSettings billing = attributeSettings.stream()
                .filter(a -> "billing_verified".equals(a.getCode())).findFirst().orElseThrow();
        // distinguishable, and each carries its own schema state
        Assertions.assertTrue(address.isValid());
        Assertions.assertFalse(billing.isValid(), "no schema field exists yet for billing_verified");
    }

    private TestComposite compositeWithVerifiedChild(String compositeName) {
        BooleanAttribute verified = new BooleanAttribute();
        verified.setName("verified");
        verified.setType("Boolean");
        verified.setSearchable(true);
        TestComposite composite = new TestComposite();
        composite.setName(compositeName);
        composite.setType("Composite");
        composite.addChild(verified);
        return composite;
    }

    @Test
    void shouldSkipNonComplexAttributeInterfaceInstanceEvenWhenNotSimple() throws Exception {
        // Defensive branch of the shared traversal (NestedBooleanSearchSupport
        // .forEachIndexableNestedBoolean): an attribute that reports isSimple()==false but is not a
        // CompositeAttribute (unlike every real Composite/List attribute) must be skipped rather
        // than throw a ClassCastException.
        when(langManager.getLangs()).thenReturn(List.of(lang("en")));
        NamedList<Object> solrClientResponse = new NamedList<>();
        solrClientResponse.add("fields", List.of());
        when(solrClient.request(any(SchemaRequest.Fields.class), eq("entando")))
                .thenReturn(solrClientResponse);

        when(contentManager.getSmallEntityTypes())
                .thenReturn(List.of(new SmallEntityType("TST", "Test type")));

        AttributeInterface fakeComplexAttribute = mock(AttributeInterface.class);
        when(fakeComplexAttribute.isSimple()).thenReturn(false);
        when(fakeComplexAttribute.getName()).thenReturn("fake");
        when(fakeComplexAttribute.getType()).thenReturn("Fake");

        Content prototype = new Content();
        prototype.setTypeCode("TST");
        prototype.addAttribute(fakeComplexAttribute);
        when(contentManager.createContentType("TST")).thenReturn(prototype);

        List<ContentTypeSettings> settings = solrSearchEngineManager.getContentTypesSettings();

        Assertions.assertEquals(1, settings.size());
        Assertions.assertEquals(1, settings.get(0).getAttributeSettings().size());
    }
}
