/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpsolr.aps.system.content.widget;

import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.ent.exception.EntException;

/**
 * @author E.Santoboni
 */
public interface IFacetNavHelper {
    
    public FacetedContentsResult getResult(List<String> selectedFacetNodes, RequestContext reqCtx) throws EntException;
	
    @Deprecated
	public List<String> getSearchResult(List<String> selectedFacetNodes, RequestContext reqCtx) throws EntException;
	
    @Deprecated
	public Map<String, Integer> getOccurences(List<String> selectedFacetNodes, RequestContext reqCtx) throws EntException;
	
	public ITreeNodeManager getTreeNodeManager();
	
}
