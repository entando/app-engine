package org.entando.entando.aps.system.services.assertionhelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.entando.entando.aps.system.services.mockhelper.PageMockHelper;
import org.entando.entando.aps.system.services.page.IPageService;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;

public class PageModelAssertionHelper {


    /**
     * does assertions on the received PagedMetadata basing on the default PageMockHelper mocked data
     * @param usageDetails
     */
    public static void assertUsageDetails(PagedMetadata<ComponentUsageEntity> usageDetails) {

        assertEquals(1, usageDetails.getTotalItems());
        assertEquals(1, usageDetails.getBody().size());
        assertEquals(1, usageDetails.getPage());

        ComponentUsageEntity expected = new ComponentUsageEntity(ComponentUsageEntity.TYPE_PAGE, PageMockHelper.PAGE_CODE, IPageService.STATUS_ONLINE);
        ComponentUsageEntityAssertionHelper.assertComponentUsageEntity(expected, usageDetails.getBody().get(0));
    }

}
